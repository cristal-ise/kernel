/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.kernel.process;

import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.CorbaServer;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ProxyManager;
import org.cristalise.kernel.entity.proxy.ProxyServer;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.Lookup;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.persistency.TransactionManager;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.process.module.ModuleManager;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.process.resource.DefaultResourceImportHandler;
import org.cristalise.kernel.process.resource.Resource;
import org.cristalise.kernel.process.resource.ResourceImportHandler;
import org.cristalise.kernel.process.resource.ResourceLoader;
import org.cristalise.kernel.scripting.ScriptConsole;
import org.cristalise.kernel.security.SecurityManager;
import org.cristalise.kernel.utils.CastorXMLUtility;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.kernel.utils.ObjectProperties;

/**
 * The Gateway is the central object of a CRISTAL process. It initializes,
 * maintains and shuts down every other subsystem in both the client and the
 * server.
 *
 * Child objects:
 * <ul>
 * <li>Lookup - Provides access to the CRISTAL directory. Find or
 * search for Items or Agents.
 * <li>ProxyManager - Gives a local proxy object for Entities found
 * in the directory. Execute activities in Items, query or subscribe to Entity data.
 * <li>TransactionManager - Access to the configured CRISTAL databases
 * <li>CorbaServer - Manages the memory pool of active Entities
 * <li>ORB - the CORBA ORB
 * </ul>
 */
public class Gateway
{
    static private ObjectProperties     mC2KProps = new ObjectProperties();
    static private ModuleManager        mModules;
    static private org.omg.CORBA.ORB    mORB;
    static private boolean              orbDestroyed = false;
    static private Lookup               mLookup;
    static private LookupManager        mLookupManager = null;
    static private TransactionManager   mStorage;
    static private ProxyManager         mProxyManager;
    static private ProxyServer          mProxyServer;
    static private CorbaServer          mCorbaServer;
    static private CastorXMLUtility     mMarshaller;
    static private ResourceLoader       mResource;
    static private SecurityManager      mSecurityManager = null;

    //FIXME: Move this cache to Resource class - requires to extend ResourceLoader with getResourceImportHandler()
    static private HashMap<BuiltInResources, ResourceImportHandler> resourceImportHandlerCache = new HashMap<BuiltInResources, ResourceImportHandler>();

    private Gateway() { }

    /**
     * Initialises the Gateway and all of the client objects it holds, with
     * the exception of the Lookup, which is initialised during connect()
     *
     * @param props - java.util.Properties containing all application properties.
     * If null, the java system properties are used
     * @throws InvalidDataException - invalid properties caused a failure in initialisation
     */
    static public void init(Properties props) throws InvalidDataException {
        init(props, null);
    }

    /**
     * Initialises the Gateway and all of the client objects it holds, with
     * the exception of the Lookup, which is initialised during connect()
     *
     * @param props - java.util.Properties containing all application properties.
     * If null, the java system properties are used
     * @param res - ResourceLoader for the kernel to use to resolve all class resource requests
     * such as for bootstrap descriptions and version information
     * @throws InvalidDataException - invalid properties caused a failure in initialisation
     */
    static public void init(Properties props, ResourceLoader res) throws InvalidDataException {
        // Init properties & resources
        mC2KProps.clear();

        orbDestroyed = false;
        mResource = res;
        if (mResource == null) mResource = new Resource();

        // report version info
        Logger.msg("Gateway.init() - Kernel version: "+getKernelVersion());

        // load kernel mapfiles giving the resourse loader and the properties of
        // the application to be able to configure castor
        try {
            mMarshaller = new CastorXMLUtility(mResource, props, mResource.getKernelResourceURL("mapFiles/"));
        }
        catch (MalformedURLException e1) {
            throw new InvalidDataException("Invalid Resource Location");
        }

        Properties allModuleProperties;

        // init module manager
        try {
            mModules = new ModuleManager(AbstractMain.isServer);
            allModuleProperties = mModules.loadModules(mResource.getModuleDefURLs());
        }
        catch (Exception e) {
            Logger.error(e);
            throw new InvalidDataException("Could not load module definitions.");
        }

        // merge in module props
        for (Enumeration<?> e = allModuleProperties.propertyNames(); e.hasMoreElements();) {
            String propName = (String)e.nextElement();
            mC2KProps.put(propName, allModuleProperties.get(propName));
        }

        // Overwrite with argument props
        if (props != null) mC2KProps.putAll(props);

        // dump properties
        Logger.msg("Gateway.init() - DONE");
        dumpC2KProps(7);
    }

    /**
     * Makes this process capable of creating and managing server entities. Runs the
     * Creates the LookupManager, ProxyServer, initialises the ORB and CORBAServer
     * 
     * @param auth - this is NOT USED
     */
    @Deprecated
    static public void startServer(Authenticator auth) throws InvalidDataException, CannotManageException {
        startServer();
    }

    /**
     * Makes this process capable of creating and managing server entities. Runs the
     * Creates the LookupManager, ProxyServer, initialises the ORB and CORBAServer
     */
    static public void startServer() throws InvalidDataException, CannotManageException {
        try {
            // check top level directory contexts
            if (mLookup instanceof LookupManager) {
                mLookupManager = (LookupManager) mLookup;
                mLookupManager.initializeDirectory();
            }
            else {
                throw new CannotManageException("Lookup implementation is not a LookupManager. Cannot write to directory");
            }

            // start entity proxy server
            mProxyServer = new ProxyServer(mC2KProps.getProperty("ItemServer.name"));

            // Init ORB - set various config 
            String serverName = mC2KProps.getProperty("ItemServer.name");

            //TODO: externalize this (or replace corba completely)
            if (serverName != null) mC2KProps.put("com.sun.CORBA.ORBServerHost", serverName);

            String serverPort = mC2KProps.getProperty("ItemServer.iiop", "1500");
            mC2KProps.put("com.sun.CORBA.ORBServerPort", serverPort);
            mC2KProps.put("com.sun.CORBA.POA.ORBServerId", "1");
            mC2KProps.put("com.sun.CORBA.POA.ORBPersistentServerPort", serverPort);
            mC2KProps.put("com.sun.CORBA.codeset.charsets", "0x05010001, 0x00010109"); // need to force UTF-8 in the Sun ORB
            mC2KProps.put("com.sun.CORBA.codeset.wcharsets", "0x00010109, 0x05010001");
            //Standard initialisation of the ORB
            orbDestroyed = false;
            mORB = org.omg.CORBA.ORB.init(new String[0], mC2KProps);

            Logger.msg("Gateway.startServer() - ORB initialised. ORB class:'" + mORB.getClass().getName()+"'" );

            // start corba server components
            mCorbaServer = new CorbaServer();

            Logger.msg("Gateway.startServer() - Server '"+serverName+"' STARTED.");
        }
        catch (Exception ex) {
            Logger.error(ex);
            Logger.die("Exception starting server components. Shutting down.");
        }
    }

    /**
     * Static getter for ModuleManager
     * 
     * @return ModuleManager
     */
    public static ModuleManager getModuleManager() {
        return mModules;
    }

    /**
     * Connects to the Lookup server in an administrative context - using the admin username and
     * password available in the implementation of the Authenticator. It shall be
     * used in server processes only.
     *
     * @throws InvalidDataException - bad params
     * @throws PersistencyException - error starting storages
     * @throws ObjectNotFoundException - object not found
     */
    static public Authenticator connect() throws InvalidDataException, PersistencyException, ObjectNotFoundException {
        try {
            Authenticator auth = null;

            if ("Shiro".equals(mC2KProps.getString("Authenticator", ""))) {
                if (mSecurityManager == null) mSecurityManager = new SecurityManager();

                mSecurityManager.setupShiro();
            }
            else {
                auth = getAuthenticator();
                if (!auth.authenticate("system")) throw new InvalidDataException("Server authentication failed");
            }

            if (mLookup != null) mLookup.close();

            mLookup = (Lookup)mC2KProps.getInstance("Lookup");
            mLookup.open(auth);

            mStorage = new TransactionManager(auth);
            mProxyManager = new ProxyManager();

            Logger.msg("Gateway.connect() - DONE.");
            return auth;
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.error(ex);
            throw new InvalidDataException("Cannot connect server process. Please check config.");
        }
    }

    /**
     * Log in with the given username and password, and initialises the {@link Lookup}, {@link TransactionManager} and {@link ProxyManager}.
     * It shall be uses in client processes only.
     * 
     * @param agentName - username
     * @param agentPassword - password
     * @return an AgentProxy on the requested user
     * 
     * @throws InvalidDataException - bad params
     * @throws PersistencyException - error starting storages
     * @throws ObjectNotFoundException - object not found
     */
    static public AgentProxy connect(String agentName, String agentPassword, String resource)
            throws InvalidDataException, ObjectNotFoundException, PersistencyException
    {
        Authenticator auth = null;

        if ("Shiro".equals(mC2KProps.getString("Authenticator", ""))) {
            if (mSecurityManager == null) mSecurityManager = new SecurityManager();

            mSecurityManager.setupShiro();

            if (!mSecurityManager.shiroAuthenticate(agentName, agentPassword))  throw new InvalidDataException("Login failed");
        }
        else {
            auth = getAuthenticator();
            if (!auth.authenticate(agentName, agentPassword, resource)) throw new InvalidDataException("Login failed");
        }

        try {
            if (mLookup != null) mLookup.close();

            mLookup = (Lookup)mC2KProps.getInstance("Lookup");
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.error(ex);
            throw new InvalidDataException("Lookup "+mC2KProps.getString("Lookup")+" could not be instantiated");
        }
        mLookup.open(auth);

        mStorage = new TransactionManager(auth);
        mProxyManager = new ProxyManager();

        // find agent proxy
        AgentPath agentPath = mLookup.getAgentPath(agentName);
        AgentProxy agent = (AgentProxy) mProxyManager.getProxy(agentPath);
        //agent.setAuthObj(auth);
        ScriptConsole.setUser(agent);

        // Run module startup scripts. Server does this during bootstrap
        mModules.setUser(agent);
        mModules.runScripts("startup");

        Logger.msg("Gateway.connect(agent) - DONE.");

        return agent;
    }

    /**
     * Authenticates the agent using the configured {@link Authenticator}
     * 
     * @param agentName the name of the agent
     * @param agentPassword the password of the agent
     * @param resource check {@link Authenticator#authenticate(String, String, String)}
     * @return AgentProxy representing the logged in user/agent
     * 
     * @throws InvalidDataException - bad params
     * @throws ObjectNotFoundException - object not found
     */
    static public AgentProxy login(String agentName, String agentPassword, String resource) 
            throws InvalidDataException, ObjectNotFoundException
    {
        if (mSecurityManager != null) {
            if (!mSecurityManager.shiroAuthenticate(agentName, agentPassword))  throw new InvalidDataException("Login failed");
        }
        else {
            Authenticator auth = getAuthenticator();
            if (!auth.authenticate(agentName, agentPassword, resource)) throw new InvalidDataException("Login failed");
        }

        // find agent proxy
        AgentPath agentPath = mLookup.getAgentPath(agentName);
        AgentProxy agent = (AgentProxy) mProxyManager.getProxy(agentPath);
        //agent.setAuthObj(auth);

        return agent;
    }

    static public Authenticator getAuthenticator() throws InvalidDataException {
        try {
            return (Authenticator)mC2KProps.getInstance("Authenticator");
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.error(ex);
            throw new InvalidDataException("Authenticator "+mC2KProps.getString("Authenticator")+" could not be instantiated");
        } 
    }

    static public AgentProxy connect(String agentName, String agentPassword) 
            throws InvalidDataException, ObjectNotFoundException, PersistencyException, InstantiationException, IllegalAccessException, ClassNotFoundException
    {
        return connect(agentName, agentPassword, null);
    }

    /**
     * Shuts down all kernel API objects
     */
    public static void close() {
        // run shutdown module scripts
        if (mModules != null) mModules.runScripts("shutdown");

        // shut down servers if running
        if (mCorbaServer != null) mCorbaServer.close();
        mCorbaServer = null;

        // disconnect from storages
        if (mStorage != null) mStorage.close();
        mStorage = null;

        // disconnect from lookup
        if (mLookup != null) mLookup.close();
        mLookup = null;
        mLookupManager = null;

        // shut down proxy manager & server
        if (mProxyManager != null) mProxyManager.shutdown();
        if (mProxyServer != null)  mProxyServer.shutdownServer();
        mProxyManager = null;
        mProxyServer = null;

        // close log consoles
        Logger.closeConsole();

        // finally, destroy the ORB
        if (!orbDestroyed) {
            getORB().destroy();
            orbDestroyed = true;
            mORB = null;
        }

        // clean up remaining objects
        mModules = null;
        mResource = null;
        mMarshaller = null;
        mC2KProps.clear();

        // abandon any log streams
        Logger.removeAll();
    }

    /**
     * Returns the initialised CORBA ORB Object 
     * 
     * @return the CORBA ORB Object
     */
    static public org.omg.CORBA.ORB getORB() {
        if (orbDestroyed) throw new RuntimeException("Gateway has been closed. ORB is destroyed. ");

        if (mORB == null) {
            mC2KProps.put("com.sun.CORBA.codeset.charsets", "0x05010001, 0x00010109"); // need to force UTF-8 in the Sun ORB
            mC2KProps.put("com.sun.CORBA.codeset.wcharsets", "0x00010109, 0x05010001");
            mORB = org.omg.CORBA.ORB.init(new String[0], mC2KProps);
        }
        return mORB;
    }

    static public SecurityManager getSecurityManager() {
        return mSecurityManager;
    }

    static public Lookup getLookup() {
        return mLookup;
    }

    static public LookupManager getLookupManager() throws CannotManageException {
        if (mLookupManager == null)
            throw new CannotManageException("No Lookup Manager created. Not a server process.");
        else
            return mLookupManager;
    }

    static public CorbaServer getCorbaServer() {
        return mCorbaServer;
    }

    static public TransactionManager getStorage() {
        return mStorage;
    }

    static public CastorXMLUtility getMarshaller() {
        return mMarshaller;
    }

    static public ResourceLoader getResource() {
        return mResource;
    }

    static public ProxyManager getProxyManager() {
        return mProxyManager;
    }


    public static ProxyServer getProxyServer() {
        return mProxyServer;
    }

    static public String getCentreId() {
        return getProperties().getString("LocalCentre");
    }

    static public Enumeration<?> propertyNames() {
        return mC2KProps.propertyNames();
    }

    static public void dumpC2KProps(int logLevel) {
        if (!Logger.doLog(logLevel)) return;
        mC2KProps.dumpProps(logLevel);
    }

    static public ObjectProperties getProperties() {
        return mC2KProps;
    }

    static public String getKernelVersion() {
        try {
            return mResource.getTextResource(null, "textFiles/version.txt");
        }
        catch (Exception ex) {
            return "No version info found";
        }
    }

    /**
     * Retrieves the ResourceImportHandler available for the resource type. It creates a new if configured 
     * or falls back to the default one provided in the kernel
     * 
     * @param resType the type o the Resource. ie. one of these values: OD/SC/SM/EA/CA/QL
     * @return the initialised ResourceImportHandler
     */
    @Deprecated
    public static ResourceImportHandler getResourceImportHandler(String resType) throws Exception {
        return getResourceImportHandler(BuiltInResources.getValue(resType));
    }

    /**
     * Retrieves the ResourceImportHandler available for the resource type. It creates a new if configured 
     * or falls back to the default one provided in the kernel
     * 
     * @param resType the type o the Resource
     * @return the initialised ResourceImportHandler
     */
    public static ResourceImportHandler getResourceImportHandler(BuiltInResources resType) throws Exception {
        if (resourceImportHandlerCache.containsKey(resType)) return resourceImportHandlerCache.get(resType);

        ResourceImportHandler handler = null;

        if (Gateway.getProperties().containsKey("ResourceImportHandler."+resType)) {
            try {
                handler = (ResourceImportHandler) Gateway.getProperties().getInstance("ResourceImportHandler."+resType);
            }
            catch (Exception ex) {
                Logger.error(ex);
                Logger.error("Exception loading ResourceHandler for "+resType+". Using default.");
            }
        }

        if (handler == null) handler = new DefaultResourceImportHandler(resType);

        resourceImportHandlerCache.put(resType, handler);

        return handler;
    }

}
