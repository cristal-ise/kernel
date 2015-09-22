/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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

/**
 * @version $Revision: 1.17 $ $Date: 2005/10/12 12:51:54 $
 * @author  $Author: abranson $
 */

import java.net.MalformedURLException;
import java.util.Enumeration;
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
import org.cristalise.kernel.process.resource.Resource;
import org.cristalise.kernel.process.resource.ResourceLoader;
import org.cristalise.kernel.utils.CastorXMLUtility;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.kernel.utils.ObjectProperties;



/**************************************************************************
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
 *
 * @author $Author: abranson $ $Date: 2005/10/12 12:51:54 $
 * @version $Revision: 1.17 $
 **************************************************************************/

public class Gateway
{
    static private ObjectProperties     mC2KProps = new ObjectProperties();
    static private ModuleManager   		mModules;
    static private org.omg.CORBA.ORB    mORB;
    static private boolean				orbDestroyed = false;
    static private Lookup               mLookup;
    static private LookupManager        mLookupManager = null;
    static private TransactionManager   mStorage;
    static private ProxyManager   		mProxyManager;
    static private ProxyServer			mProxyServer;
    static private CorbaServer          mCorbaServer;
    static private CastorXMLUtility		mMarshaller;
    static private ResourceLoader		mResource;



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
        Logger.msg("Kernel version: "+getKernelVersion());

		// load kernel mapfiles giving the resourse loader and the properties of
		// the application to be able to configure castor
        try {
        	mMarshaller = new CastorXMLUtility(mResource, props, mResource.getKernelResourceURL("mapFiles/"));
        } catch (MalformedURLException e1) {
            throw new InvalidDataException("Invalid Resource Location");
        }

        
        // init module manager
        try {
			mModules = new ModuleManager(mResource.getModuleDefURLs(), AbstractMain.isServer);
		} catch (Exception e) {
			Logger.error(e);
			throw new InvalidDataException("Could not load module definitions.");
		}

        // merge in module props
        Properties moduleProperties = mModules.getAllModuleProperties();
        for (Enumeration<?> e = moduleProperties.propertyNames(); e.hasMoreElements();) {
            String propName = (String)e.nextElement();
            mC2KProps.put(propName, moduleProperties.get(propName));
        }

        // Overwrite with argument props
        if (props != null) mC2KProps.putAll(props);

        // dump properties
        dumpC2KProps(7);
    }

    /**
     * Makes this process capable of creating and managing server entities. Runs the
     * bootstrap to create the root LDAP contexts, initialises the CORBA server and
     * time-out manager.
     *
     * @throws InvalidDataException - error initialising
     */
    static public void startServer(Authenticator auth) throws InvalidDataException, CannotManageException {
        try {
            // check top level directory contexts
        	if (mLookup instanceof LookupManager) {
        		mLookupManager = (LookupManager)mLookup;
        		mLookupManager.initializeDirectory();
        	}
        	else {
        		throw new CannotManageException("Lookup implementation is not a LookupManager. Cannot write to directory");
        	}
            		
            // start entity proxy server
            mProxyServer = new ProxyServer(mC2KProps.getProperty("ItemServer.name"));

            // Init ORB - set various config 
            String serverName = mC2KProps.getProperty("ItemServer.name");
            if (serverName != null)
            	mC2KProps.put("com.sun.CORBA.ORBServerHost", serverName);
            String serverPort = mC2KProps.getProperty("ItemServer.iiop", "1500");
            mC2KProps.put("com.sun.CORBA.ORBServerPort", serverPort);
            //TODO: externalize this (or replace corba completely)
            mC2KProps.put("com.sun.CORBA.POA.ORBServerId", "1");
            mC2KProps.put("com.sun.CORBA.POA.ORBPersistentServerPort", serverPort);
            mC2KProps.put("com.sun.CORBA.codeset.charsets", "0x05010001, 0x00010109"); // need to force UTF-8 in the Sun ORB
            mC2KProps.put("com.sun.CORBA.codeset.wcharsets", "0x00010109, 0x05010001");
            //Standard initialisation of the ORB
            orbDestroyed = false;
            mORB = org.omg.CORBA.ORB.init(new String[0], mC2KProps);

            Logger.msg("Gateway.init() - ORB initialised. ORB is " + mORB.getClass().getName() );

            // start corba server components
            mCorbaServer = new CorbaServer();

            // start checking bootstrap & module items
            Bootstrap.run();
            System.out.println("Server '"+serverName+"' initialised.");            
        } catch (Exception ex) {
            Logger.error(ex);
            Logger.die("Exception starting server components. Shutting down.");
        }

    }

    public static ModuleManager getModuleManager() {
		return mModules;
	}

	/**
     * Connects to the LDAP server in an administrative context - using the admin username and
     * password given in the LDAP.user and LDAP.password props of the kernel properties.
     *
     * @throws InvalidDataException - bad params
     * @throws PersistencyException - error starting storages
     */
    static public Authenticator connect()
        throws InvalidDataException,
               PersistencyException
    {
    	try {
    		Authenticator auth = getAuthenticator();
    		if (!auth.authenticate("System"))
    			throw new InvalidDataException("Server authentication failed");
    		
    		if (mLookup != null)
    			mLookup.close();
    		
            mLookup = (Lookup)mC2KProps.getInstance("Lookup");
            mLookup.open(auth);
            
            mStorage = new TransactionManager(auth);
            mProxyManager = new ProxyManager();
            
            return auth;
    	} catch (Exception ex) {
            Logger.error(ex);
            throw new InvalidDataException("Cannot connect server process. Please check config.");
        }


    }

    /**
     * Logs in with the given username and password, and initialises the lookup, storage and proxy manager.
     *
     * @param agentName - username
     * @param agentPassword - password
     * @return an AgentProxy on the requested user
     * @throws InvalidDataException
     * @throws PersistencyException 
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    static public AgentProxy connect(String agentName, String agentPassword, String resource)
    throws InvalidDataException, ObjectNotFoundException, PersistencyException
    {
        Authenticator auth = getAuthenticator();
        if (!auth.authenticate(agentName, agentPassword, resource))
        	throw new InvalidDataException("Login failed");
        
        try {
    		if (mLookup != null)
    			mLookup.close();
        	mLookup = (Lookup)mC2KProps.getInstance("Lookup");
        } catch (Exception e) {
			Logger.error(e);
			throw new InvalidDataException("Lookup "+mC2KProps.getString("Lookup")+" could not be instantiated");
		}  
        mLookup.open(auth);

        mStorage = new TransactionManager(auth);
        mProxyManager = new ProxyManager();

        // find agent proxy
        AgentPath agentPath = mLookup.getAgentPath(agentName);
        AgentProxy userProxy = (AgentProxy) mProxyManager.getProxy(agentPath);
        userProxy.setAuthObj(auth);
        
        // Run module startup scripts. Server does this during bootstrap
        mModules.setUser(userProxy);
        mModules.runScripts("startup");
        
		return userProxy;
    }
    
    static public AgentProxy login(String agentName, String agentPassword, String resource) 
    		throws InvalidDataException, ObjectNotFoundException {
        Authenticator auth = getAuthenticator();
        if (!auth.authenticate(agentName, agentPassword, resource))
        	throw new InvalidDataException("Login failed");
        
        // find agent proxy
        AgentPath agentPath = mLookup.getAgentPath(agentName);
        AgentProxy userProxy = (AgentProxy) mProxyManager.getProxy(agentPath);
        userProxy.setAuthObj(auth);
        
		return userProxy;
    }
    
    static public Authenticator getAuthenticator() throws InvalidDataException {
    	try {
			return (Authenticator)mC2KProps.getInstance("Authenticator");
		} catch (Exception e) {
			Logger.error(e);
			throw new InvalidDataException("Authenticator "+mC2KProps.getString("Authenticator")+" could not be instantiated");
		} 
    }
    
    static public AgentProxy connect(String agentName, String agentPassword) 
    		throws InvalidDataException, ObjectNotFoundException, PersistencyException, InstantiationException, IllegalAccessException, ClassNotFoundException
    {
    	return connect(agentName, agentPassword, null);
    }

    /**
     * Shuts down all kernel api objects
     */
    public static void close()
    {
    	// run shutdown module scripts
    	if (mModules != null)
    		mModules.runScripts("shutdown");

        // shut down servers if running
        if (mCorbaServer != null)
            mCorbaServer.close();
        mCorbaServer = null;
        
        // disconnect from storages
        if (mStorage != null)
            mStorage.close();
        mStorage = null;

        // disconnect from ldap
        if (mLookup != null)
            mLookup.close();
        mLookup = null;
        mLookupManager = null;

        // shut down proxy manager & server
        if (mProxyManager != null)
            mProxyManager.shutdown();
        if (mProxyServer != null)
        	mProxyServer.shutdownServer();
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
    }

    static public org.omg.CORBA.ORB getORB()
    {
    	if (orbDestroyed) throw new RuntimeException("Gateway has been closed. ORB is destroyed. ");

    	if (mORB == null) {
    		mC2KProps.put("com.sun.CORBA.codeset.charsets", "0x05010001, 0x00010109"); // need to force UTF-8 in the Sun ORB
    		mC2KProps.put("com.sun.CORBA.codeset.wcharsets", "0x00010109, 0x05010001");
    		mORB = org.omg.CORBA.ORB.init(new String[0], mC2KProps);
    	}
        return mORB;
    }

    static public Lookup getLookup()
    {
        return mLookup;
    }

    static public LookupManager getLookupManager() throws CannotManageException
    {
        if (mLookupManager == null)
        	throw new CannotManageException("No Lookup Manager created. Not a server process.");
        else
        	return mLookupManager;
    }
    
    static public CorbaServer getCorbaServer()
    {
        return mCorbaServer;
    }

    static public TransactionManager getStorage()
    {
        return mStorage;
    }
    
    static public CastorXMLUtility getMarshaller()
    {
        return mMarshaller;
    }
    
    static public ResourceLoader getResource()
    {
    	return mResource;
    }

    static public ProxyManager getProxyManager()
    {
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
    	} catch (Exception ex) {
    		return "No version info found";
    	}

    }
}

