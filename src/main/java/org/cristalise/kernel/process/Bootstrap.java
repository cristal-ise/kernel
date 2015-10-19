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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.OutcomeValidator;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.resource.DefaultResourceImportHandler;
import org.cristalise.kernel.process.resource.ResourceImportHandler;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.property.PropertyDescription;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.scripting.ScriptConsole;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;


/**
 * @version $Revision: 1.25 $ $Date: 2006/01/10 09:48:32 $
 * @author  $Author: abranson $
 */

public class Bootstrap
{
    static DomainPath thisServerPath;
    static HashMap<String, ResourceImportHandler> resHandlerCache = new HashMap<String, ResourceImportHandler>();
    static HashMap<String, AgentProxy> systemAgents = new HashMap<String, AgentProxy>();
    public static boolean shutdown = false;

	/**
	 * Run everything without timing-out the service wrapper
	 */
	public static void run() throws Exception {
		// check for system agents
		checkAdminAgents();

		// create the server's mother item
		createServerItem();
		new Thread(new Runnable() {
			@Override
			public void run() {
		        try {
                    Thread.currentThread().setName("Bootstrapper");
                    
                    ClassLoader wClassLoader = Bootstrap.class.getClassLoader();
                    Logger.msg(String.format("Bootstrap.run() setContextClassLoader=[%s]",wClassLoader));
                    Thread.currentThread().setContextClassLoader(wClassLoader);

					// make sure all of the boot items are up-to-date
                    if (!shutdown) {
                    	Logger.msg("Bootstrap.run() - Verifying kernel boot data items");
                    	verifyBootDataItems();
                    }

					// verify the server item's wf
                    if (!shutdown) {
                    	Logger.msg("Bootstrap.run() - Initialising Server Item Workflow");
                    	initServerItemWf();
                    }
                    
                    if (!shutdown) {
                    	Gateway.getModuleManager().setUser(systemAgents.get("system"));
                    	Gateway.getModuleManager().registerModules();
                    }

					if (!shutdown) {
						Logger.msg("Bootstrap.run() - Bootstrapping complete");
						Gateway.getModuleManager().runScripts("initialized");
					}	
					
				} catch (Throwable e) {
					Logger.error(e);
					Logger.die("Exception performing bootstrap. Check that everything is OK.");
				}
			}
		}).start();
	}
	
	public static void abort() {
		shutdown = true;
	}

    /**************************************************************************
     * Checks all kernel descriptions, stored in resources
     **************************************************************************/
    public static void verifyBootDataItems() throws Exception {
    	String bootItems;
    	Logger.msg(1, "Verifying kernel boot items");
    	bootItems = FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/allbootitems.txt"));
    	verifyBootDataItems(bootItems, null, true);
    	Logger.msg(1, "Boot data items complete");
    }

    private static void verifyBootDataItems(String bootList, String ns, boolean reset) throws InvalidItemPathException {
        StringTokenizer str = new StringTokenizer(bootList, "\n\r");
        while (str.hasMoreTokens() && !shutdown) {
            String thisItem = str.nextToken();
            ItemPath itemPath = null;
            String[] itemParts = thisItem.split("/");
            if (itemParts.length == 3) { // includes UUID
            	itemPath = new ItemPath(itemParts[2]);
            }
            String itemType = itemParts[0];
            String itemName = itemParts[1];
            try {
                String location = "boot/"+thisItem+(itemType.equals("OD")?".xsd":".xml");
                verifyResource(ns, itemName, 0, itemType, itemPath, location, reset);
            } catch (Exception e) {
                Logger.error(e);
                Logger.die("Error importing bootstrap items. Unsafe to continue.");
            }
        }
    }

    
	/**
	* Create a resource item from its module definition. The item should not already exist.
	* 
	* @param ns
	* @param itemName
	* @param version
	* @param itemType
	* @param outcomes
	* @param reset
	* @return
	* @throws Exception
	*/
	public static DomainPath createResource(String ns, String itemName, int version, String itemType, Set<Outcome> outcomes, boolean reset)
			throws Exception
	{
		return verifyResource(ns, itemName, version, itemType, null, outcomes, null, reset);
	}

    /**
     * Verify a resource item against a module version, using a ResourceImportHandler configured to find outcomes at the given dataLocation
     * @param ns
     * @param itemName
     * @param version
     * @param itemType
     * @param itemPath
     * @param dataLocation
     * @param reset
     * @return
     * @throws Exception
     */
    public static DomainPath verifyResource(String ns, String itemName, int version, String itemType, ItemPath itemPath, 
            String dataLocation, boolean reset)
                    throws Exception
    {
        return verifyResource(ns, itemName, version, itemType, itemPath, null, dataLocation, reset);
    }

    /**
     * Verify a resource item against a module version, but supplies the resource outcomes directly instead of through a 
     * 
     * @param ns
     * @param itemName
     * @param version
     * @param itemType
     * @param itemPath
     * @param outcomes
     * @param reset
     * @return
     * @throws Exception
     */
    public static DomainPath verifyResource(String ns, String itemName, int version, String itemType, ItemPath itemPath, 
            Set<Outcome> outcomes, boolean reset)
                    throws Exception
    {
        return verifyResource(ns, itemName, version, itemType, itemPath, outcomes, null, reset);
    }

    /**
     * 
     * @param ns
     * @param itemName
     * @param version
     * @param itemType
     * @param itemPath
     * @param outcomes
     * @param dataLocation
     * @param reset
     * @return
     * @throws Exception
     */
    private static DomainPath verifyResource(String ns, String itemName, int version, String itemType, ItemPath itemPath,
            Set<Outcome> outcomes, String dataLocation, boolean reset)
                    throws Exception {
		ResourceImportHandler typeImpHandler = getHandler(itemType);

		Logger.msg(1, "Bootstrap.verifyResource() - Verifying version:'"
				+ version + "' type:'" + typeImpHandler.getName() + "' name:'" + itemName + "'");

		// Find or create Item for Resource
		ItemProxy thisProxy;
		DomainPath modDomPath = typeImpHandler.getPath(itemName, ns);

		Iterator<Path> en = Gateway.getLookup().search(typeImpHandler.getTypeRoot(), itemName);

		if (en.hasNext()) {
			Logger.msg(3, "Bootstrap.verifyResource() - Found "
					+ typeImpHandler.getName() + " " + itemName + ".");

			thisProxy = verifyPathAndModuleProperty(ns, itemType, itemName,
					itemPath, modDomPath, (DomainPath) en.next());
		} 
		else {
			if (itemPath == null)
				itemPath = new ItemPath();
		
			Logger.msg("Bootstrap.verifyResource() - " 
					+ typeImpHandler.getName() + " " + itemName + " not found. Creating new.");

			thisProxy = createResourceItem(typeImpHandler, itemName, ns, itemPath);
		}

		// Verify/Import Outcomes, creating events and views as necessary
		if (outcomes == null || outcomes.size() == 0) {
			outcomes = typeImpHandler.getResourceOutcomes(itemName, ns,
					dataLocation, version);
		}

		if (outcomes.size() == 0)
			Logger.warning("Bootstrap.verifyResource() - no Outcome found therefore nothing stored!");

		for (Outcome newOutcome : outcomes) {
			if (checkToStoreOutcomeVersion(thisProxy, newOutcome, version,
					reset)) {
				// validate it (but not for kernel objects because we need those
				// to validate the rest)
				if (ns != null) {
					String error = OutcomeValidator.getValidator(newOutcome).validate(newOutcome.getData());
					if (error.length() > 0) {
						Logger.error("Outcome not valid: \n " + error);
						throw new InvalidDataException(error);
					}
				}

				storeOutcomeEventAndViews(thisProxy, newOutcome, version);
			}
		}

		Gateway.getStorage().commit(thisProxy);
		return modDomPath;
	}

    /**
     * Verify module property and location
     * 
     * @param ns
     * @param itemName
     * @param itemPath
     * @param lookupManager
     * @param thisProxy
     * @param modDomPath
     * @param path
     * @throws PersistencyException
     * @throws ObjectCannotBeUpdated
     * @throws ObjectAlreadyExistsException
     * @throws CannotManageException 
     */
    private static ItemProxy verifyPathAndModuleProperty(String ns, String itemType, String itemName, ItemPath itemPath,
                                                         DomainPath modDomPath, DomainPath path) throws Exception {
    	
        LookupManager lookupManager = Gateway.getLookupManager();
        ItemProxy thisProxy = Gateway.getProxyManager().getProxy(path);

        if (itemPath != null && !path.getItemPath().equals(itemPath)) {
        	Logger.warning("Resource "+itemType+"/"+itemName+" should have path "+itemPath+" but was found with path "+path.getItemPath());
            itemPath = path.getItemPath();
        }
        
        if (itemPath == null) itemPath = path.getItemPath();
	          	
        String moduleName = (ns==null?"kernel":ns);
        String itemModule;
        try {
        	itemModule = thisProxy.getProperty("Module");
            if (itemModule != null && !itemModule.equals("") && !itemModule.equals("null") && !moduleName.equals(itemModule)) {
                String error = "Module clash! Resource '"+itemName+"' included in module "+moduleName+" but is assigned to '"+itemModule + "'.";
                Logger.error(error);
                throw new InvalidDataException(error);
            }
        }
        catch (ObjectNotFoundException ex) {
           	itemModule = "";
        }
	            
        if (!modDomPath.equals(path)) {	 // move item to module subtree
           	Logger.msg("Module item "+itemName+" found with path "+path.toString()+". Moving to "+modDomPath.toString());
           	modDomPath.setItemPath(itemPath);
            
            if (!modDomPath.exists()) lookupManager.add(modDomPath);
	            	lookupManager.delete(path);
	    }
        return thisProxy;
    }
            
    /**
     * @param itemName
     * @param version
     * @param itemPath
     * @param typeImpHandler
     * @param thisProxy
     * @param newOutcome
     * @throws PersistencyException
     */
    private static void storeOutcomeEventAndViews(ItemProxy item, Outcome newOutcome, int version)
            throws PersistencyException
    {
        Logger.msg("Bootstrap.storeOutcomeEventAndViews() - Writing new " + newOutcome.getSchemaType() + " v" + version /*+ " to " + "typeImpHandler.getName()" + " " + "itemName"*/);

        History hist = new History( item.getPath(), item);
        Transition transDone = new Transition(0, "Done", 0, 0);
        String viewName = String.valueOf(version);

        int eventID = hist.addEvent( systemAgents.get("system").getPath(), "Admin", "Bootstrap", "Bootstrap", "Bootstrap", 
                newOutcome.getSchemaType(), 0, "PredefinedStep", 0, transDone, viewName).getID();

        newOutcome.setID(eventID);

        Viewpoint newLastView   = new Viewpoint(item.getPath(), newOutcome.getSchemaType(), "last",   0, eventID);
        Viewpoint newNumberView = new Viewpoint(item.getPath(), newOutcome.getSchemaType(), viewName, 0, eventID);

        Gateway.getStorage().put(item.getPath(), newOutcome,    item);
        Gateway.getStorage().put(item.getPath(), newLastView,   item);
        Gateway.getStorage().put(item.getPath(), newNumberView, item);
    }

    /**
     * 
     * @param item
     * @param newOutcome
     * @param version
     * @param reset
     * @return
     * @throws PersistencyException
     * @throws InvalidDataException
     */
    private static boolean checkToStoreOutcomeVersion(ItemProxy item, Outcome newOutcome, int version, boolean reset)
            throws PersistencyException, InvalidDataException
 {
		try {
			Viewpoint currentData = (Viewpoint) item.getObject(ClusterStorage.VIEWPOINT + "/"
							+ newOutcome.getSchemaType() + "/" + version);
			Outcome oldData = currentData.getOutcome();
			
			XMLUnit.setIgnoreWhitespace(true);
			XMLUnit.setIgnoreComments(true);
			Diff xmlDiff = new Diff(newOutcome.getDOM(), oldData.getDOM());
			if (xmlDiff.identical()) {
				Logger.msg(5,
						"Bootstrap.checkToStoreOutcomeVersion() - Data identical, no update required");
				return false;
			} else {
				Logger.msg("Bootstrap.checkToStoreOutcomeVersion() - Difference found in item:"	+ item.getPath() + newOutcome.getSchemaType() + ": " + xmlDiff.toString());
				if (!reset	&& !currentData.getEvent().getStepPath().equals("Bootstrap")) {
					Logger.msg("Bootstrap.checkToStoreOutcomeVersion() - Version " + version + " was not set by Bootstrap, and reset not requested. Not overwriting.");
					return false;
				}
			}
		} catch (ObjectNotFoundException ex) {
			Logger.msg("Bootstrap.checkToStoreOutcomeVersion() - " +item.getName() + " " + newOutcome.getSchemaType()
					+ " v" + version + " not found! Attempting to insert new.");
		}
		return true;
	}
    
    private static ResourceImportHandler getHandler(String resType) throws Exception {
    	if (resHandlerCache.containsKey(resType))
    		return resHandlerCache.get(resType);
    	ResourceImportHandler handler = null;
    	if (Gateway.getProperties().containsKey("ResourceImportHandler."+resType)) {
    		try {
    			handler = (ResourceImportHandler) Gateway.getProperties().getInstance("ResourceImportHandler."+resType);
    		} catch (Exception ex) {
    			Logger.error(ex);
    			Logger.error("Exception loading ResourceHandler for "+resType+". Using default.");
    		}
    	}
    	
    	if (handler == null) 
    		handler = new DefaultResourceImportHandler(resType);
    	
    	resHandlerCache.put(resType, handler);
    	return handler;
    }

	/**
     * @param itemType
     * @param itemName
     * @param data
     */
    private static ItemProxy createResourceItem(ResourceImportHandler impHandler, String itemName, String ns, ItemPath itemPath) throws Exception {
        // create props
        PropertyDescriptionList pdList = impHandler.getPropDesc();
        PropertyArrayList props = new PropertyArrayList();
        LookupManager lookupManager = Gateway.getLookupManager();
        
        for (int i = 0; i < pdList.list.size(); i++) {
            PropertyDescription pd = pdList.list.get(i);
            String propName = pd.getName();
            String propVal = propName.equals("Name")?itemName:pd.getDefaultValue();
            props.list.add(new Property(propName, propVal, pd.getIsMutable()));
        }
        
        CompositeActivity ca = new CompositeActivity();
        if (ns!=null && Gateway.getProperties().getBoolean("Module.debug", false)) {
        	try {
        		ca = (CompositeActivity) ((CompositeActivityDef)LocalObjectLoader.getActDef(impHandler.getWorkflowName(), 0)).instantiate();
        	} catch (ObjectNotFoundException ex) {
        		Logger.error("Module resource workflow "+impHandler.getWorkflowName()+" not found. Using empty.");
        	}
        }


        Gateway.getCorbaServer().createItem(itemPath);
        lookupManager.add(itemPath);
        DomainPath newDomPath = impHandler.getPath(itemName, ns);
        newDomPath.setItemPath(itemPath);
        lookupManager.add(newDomPath);
        ItemProxy newItemProxy = Gateway.getProxyManager().getProxy(itemPath);
        newItemProxy.initialise( systemAgents.get("system").getPath(), props, ca, null);
        return newItemProxy;
    }

    /**************************************************************************
     * Checks for the existence of the admin users so you can use Cristal
     **************************************************************************/
     private static AgentProxy checkAgent(String name, String pass, RolePath rolePath, String uuid) throws Exception {
    	 
         Logger.msg(1, "Bootstrap.checkAgent() - Checking for existence of '"+name+"' user.");
         LookupManager lookup = Gateway.getLookupManager();
         
         try {
        	 AgentProxy agentProxy = Gateway.getProxyManager().getAgentProxy(lookup.getAgentPath(name));
             systemAgents.put(name, agentProxy);
             Logger.msg(3, "Bootstrap.checkAgent() - User '"+name+"' found.");
             return agentProxy;
         } catch (ObjectNotFoundException ex) { }
         
         Logger.msg("Bootstrap.checkAgent() - User '"+name+"' not found. Creating.");

         try {
             AgentPath agentPath = new AgentPath(new ItemPath(uuid), name);
             agentPath.setPassword(pass);
             Gateway.getCorbaServer().createAgent(agentPath);
             lookup.add(agentPath);

             // assign admin role
             Logger.msg("Bootstrap.checkAgent() - Assigning role '"+rolePath.getName()+"'");
             Gateway.getLookupManager().addRole(agentPath, rolePath);
             Gateway.getStorage().put(agentPath, new Property("Name", name, true), null);
             Gateway.getStorage().put(agentPath, new Property("Type", "Agent", false), null);
             AgentProxy agentProxy = Gateway.getProxyManager().getAgentProxy(agentPath);
             systemAgents.put(name, agentProxy);
             return agentProxy;
         } catch (Exception ex) {
             Logger.error("Unable to create "+name+" user.");
             throw ex;
         }
     }

    /**
     *
     */
    public static void checkAdminAgents() throws Exception {
    	
        // check for administrative user & admin role
    	RolePath rootRole = new RolePath();
    	if (!rootRole.exists()) Gateway.getLookupManager().createRole(rootRole);
    	RolePath adminRole = new RolePath(rootRole, "Admin", false);
    	if (!adminRole.exists()) Gateway.getLookupManager().createRole(adminRole);
    	
        // check for import user
    	AgentProxy system = checkAgent("system", null, adminRole, new UUID(0, 0).toString());
    	ScriptConsole.setUser(system);
    	
        // check for local usercode user & role
    	RolePath usercodeRole = new RolePath(rootRole, "UserCode", true);
    	if (!usercodeRole.exists()) Gateway.getLookupManager().createRole(usercodeRole);
        checkAgent(InetAddress.getLocalHost().getHostName(), "uc", usercodeRole, UUID.randomUUID().toString());
    }

    public static void createServerItem() throws Exception {
    	LookupManager lookupManager = Gateway.getLookupManager();
        String serverName = Gateway.getProperties().getString("ItemServer.name", InetAddress.getLocalHost().getHostName());
        thisServerPath = new DomainPath("/servers/"+serverName);
        ItemPath serverItem;
        try {
            serverItem = thisServerPath.getItemPath();
        } catch (ObjectNotFoundException ex) {
            Logger.msg("Creating server item "+thisServerPath);
            serverItem = new ItemPath();
            Gateway.getCorbaServer().createItem(serverItem);
            lookupManager.add(serverItem);
            thisServerPath.setItemPath(serverItem);
            lookupManager.add(thisServerPath);
        }
        Gateway.getStorage().put(serverItem, new Property("Name", serverName, false), null);
        Gateway.getStorage().put(serverItem, new Property("Type", "Server", false), null);
        Gateway.getStorage().put(serverItem, new Property("KernelVersion", Gateway.getKernelVersion(), true), null);
        int proxyPort = Gateway.getProperties().getInt("ItemServer.Proxy.port", 1553);
        Gateway.getStorage().put(serverItem,
                    new Property("ProxyPort", String.valueOf(proxyPort), false), null);
        Gateway.getStorage().put(serverItem,
                    new Property("ConsolePort", String.valueOf(Logger.getConsolePort()), true), null);
        Gateway.getProxyManager().connectToProxyServer(serverName, proxyPort);

    }

    public static void initServerItemWf() throws Exception {
    	CompositeActivityDef serverWfCa = (CompositeActivityDef)LocalObjectLoader.getActDef("ServerItemWorkflow", 0);
        Workflow wf = new Workflow((CompositeActivity)serverWfCa.instantiate(), new ServerPredefinedStepContainer());
        wf.initialise(thisServerPath.getItemPath(), systemAgents.get("system").getPath(), null);
        Gateway.getStorage().put(thisServerPath.getItemPath(), wf, null);
    }
}
