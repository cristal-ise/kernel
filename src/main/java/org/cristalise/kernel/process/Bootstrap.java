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

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
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
                    Logger.msg("Bootstrap.run() - Verifying kernel boot data items");
					verifyBootDataItems();

					// verify the server item's wf
					Logger.msg("Bootstrap.run() - Initialising Server Item Workflow");
					initServerItemWf();
					
					Gateway.getModuleManager().setUser(systemAgents.get("system"));
		            Gateway.getModuleManager().registerModules();

					Logger.msg("Bootstrap.run() - Bootstrapping complete");
				} catch (Throwable e) {
					Logger.error(e);
					Logger.die("Exception performing bootstrap. Check that everything is OK.");
				}
			}
		}).start();
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
        while (str.hasMoreTokens()) {
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
                
     public static DomainPath verifyResource(String ns, String itemName, int version, String itemType, ItemPath itemPath, String dataLocation, boolean reset) throws Exception {
    	 	LookupManager lookupManager = Gateway.getLookupManager();
            ResourceImportHandler typeImpHandler = getHandler(itemType);
    	 	Logger.msg(1, "Bootstrap.verifyResource() - Verifying version "+version+" of "+typeImpHandler.getName()+" "+itemName);
            
            // Find or create Item for Resource
    	 	DomainPath modDomPath = typeImpHandler.getPath(itemName, ns);
            ItemProxy thisProxy;
            Iterator<Path> en = Gateway.getLookup().search(typeImpHandler.getTypeRoot(), itemName);
            if (!en.hasNext()) {
            	if (itemPath == null) itemPath = new ItemPath();
                Logger.msg("Bootstrap.verifyResource() - "+typeImpHandler.getName()+" "+itemName+" not found. Creating new.");
                thisProxy = createResourceItem(typeImpHandler, itemName, ns, itemPath);
            }
            else {
                DomainPath path = (DomainPath)en.next();
                thisProxy = Gateway.getProxyManager().getProxy(path);
                if (itemPath != null && !path.getItemPath().equals(itemPath)) {
                	Logger.warning("Resource "+itemType+"/"+itemName+" should have path "+itemPath+" but was found with path "+path.getItemPath());
                	itemPath = path.getItemPath();
                }
                if (itemPath == null) itemPath = path.getItemPath();
	            // Verify module property and location
	          	
	            String moduleName = (ns==null?"kernel":ns);
	            String itemModule;
	            try{
	            	itemModule = thisProxy.getProperty("Module");
	                if (!itemModule.equals("") && !itemModule.equals("null") && !moduleName.equals(itemModule)) {
	                	Logger.error("Bootstrap.verifyResource() - Module clash! Resource '"+itemName+"' included in module "+moduleName+" but is assigned to '"+itemModule+"'. Not overwriting.");
	                	return path;
	                }
	            } catch (ObjectNotFoundException ex) { 
	            	itemModule = "";
	            }
	            
	            if (!moduleName.equals(itemModule)) { // write module property
	            	Gateway.getStorage().put(itemPath, new Property("Module", moduleName, false), thisProxy);
	            }
	            
	            if (!modDomPath.equals(path)) {	 // move item to module subtree
	            	Logger.msg("Module item "+itemName+" found with path "+path.toString()+". Moving to "+modDomPath.toString());
	            	modDomPath.setItemPath(itemPath);
	            	if (!modDomPath.exists())
	            		lookupManager.add(modDomPath);
	            	lookupManager.delete(path);
	            }
            }
            
            // Verify/Import Outcomes,  creating events and views as necessary
            Set<Outcome> impList = typeImpHandler.getResourceOutcomes(itemName, ns, dataLocation, version);
            for (Outcome newOutcome : impList) {
            	try {
	                Viewpoint currentData = (Viewpoint)thisProxy.getObject(ClusterStorage.VIEWPOINT+"/"+newOutcome.getSchemaType()+"/"+version);
                    Outcome oldData = currentData.getOutcome();
                    XMLUnit.setIgnoreWhitespace(true);
                    XMLUnit.setIgnoreComments(true);
                    Diff xmlDiff = new Diff(newOutcome.getDOM(), oldData.getDOM());
                    if (xmlDiff.identical()) {
                        Logger.msg(5, "Bootstrap.verifyResource() - Data identical, no update required");
                        continue;
                    }
                    else {
                    	Logger.msg("Difference found in "+itemName+": "+xmlDiff.toString());
                    	if (!reset && !currentData.getEvent().getStepPath().equals("Bootstrap")) {
                    		Logger.msg("Version "+version+" was not set by Bootstrap, and reset not requested. Not overwriting.");
                    		continue;
                    	}
                    }
                    
                } catch (ObjectNotFoundException ex) {
                    Logger.msg("Bootstrap.verifyResource() - Item "+itemName+" exists but version "+version+" not found! Attempting to insert new.");
                }
            
	            // data was missing or doesn't match
            	// validate it (but not for kernel objects because we need those to validate the rest)
            	if (ns!= null) {
            		OutcomeValidator validator = OutcomeValidator.getValidator(LocalObjectLoader.getSchema(newOutcome.getSchemaType(), newOutcome.getSchemaVersion()));
            		String error = validator.validate(newOutcome.getData());
            		if (error.length() > 0) {
            			Logger.error("Outcome not valid: \n " + error);
            			throw new InvalidDataException(error);
            		}
            	}
                
                // store
	            Logger.msg("Bootstrap.verifyResource() - Writing new "+newOutcome.getSchemaType()+" v"+version+" to "+typeImpHandler.getName()+" "+itemName);
	            History hist = new History(itemPath, thisProxy);
	            Transition predefDone = new Transition(0, "Done", 0, 0);
	            Event newEvent = hist.addEvent(systemAgents.get("system").getPath(), "Admin", "Bootstrap", "Bootstrap", "Bootstrap", newOutcome.getSchemaType(), 0, "PredefinedStep", 0, predefDone, String.valueOf(version));
	            newOutcome.setID(newEvent.getID());
	            Viewpoint newLastView = new Viewpoint(itemPath, newOutcome.getSchemaType(), "last", 0, newEvent.getID());
	            Viewpoint newNumberView = new Viewpoint(itemPath, newOutcome.getSchemaType(), String.valueOf(version), 0, newEvent.getID());
	            Gateway.getStorage().put(itemPath, newOutcome, thisProxy);
	            Gateway.getStorage().put(itemPath, newLastView, thisProxy);
	            Gateway.getStorage().put(itemPath, newNumberView, thisProxy);
            }
            Gateway.getStorage().commit(thisProxy);
            return modDomPath;
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
        if (ns!=null && Gateway.getProperties().getBoolean("Module.debug", false))
        	try {
        		ca = (CompositeActivity) ((CompositeActivityDef)LocalObjectLoader.getActDef(impHandler.getWorkflowName(), 0)).instantiate();
        	} catch (ObjectNotFoundException ex) {
        		Logger.error("Module resource workflow "+impHandler.getWorkflowName()+" not found. Using empty.");
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
     private static void checkAgent(String name, String pass, RolePath rolePath, String uuid) throws Exception {
         Logger.msg(1, "Bootstrap.checkAgent() - Checking for existence of '"+name+"' user.");
         LookupManager lookup = Gateway.getLookupManager();
         
         try {
             systemAgents.put(name, Gateway.getProxyManager().getAgentProxy(lookup.getAgentPath(name)));
             Logger.msg(3, "Bootstrap.checkAgent() - User '"+name+"' found.");
             return;
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
             systemAgents.put(name, Gateway.getProxyManager().getAgentProxy(agentPath));
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
    	String adminPassword = Gateway.getProperties().getString("AdminPassword", "admin12345");
    	RolePath rootRole = new RolePath();
    	if (!rootRole.exists()) Gateway.getLookupManager().createRole(rootRole);
    	RolePath adminRole = new RolePath(rootRole, "Admin", false);
    	if (!adminRole.exists()) Gateway.getLookupManager().createRole(adminRole);
    	
        // check for import user
    	checkAgent("system", adminPassword, adminRole, new UUID(0, 0).toString());
    	
    	checkAgent("admin", adminPassword, adminRole, new UUID(0, 1).toString());

        // check for local usercode user & role
    	RolePath usercodeRole = new RolePath(rootRole, "UserCode", true);
    	if (!usercodeRole.exists()) Gateway.getLookupManager().createRole(usercodeRole);
        checkAgent(InetAddress.getLocalHost().getHostName(), "uc", usercodeRole, UUID.randomUUID().toString());
    }

    public static void createServerItem() throws Exception {
    	LookupManager lookupManager = Gateway.getLookupManager();
        String serverName = Gateway.getProperties().getString("ItemServer.name", InetAddress.getLocalHost().getHostName());
        thisServerPath = new DomainPath("/servers/"+serverName);
        ItemPath serverEntity;
        try {
            serverEntity = thisServerPath.getItemPath();
        } catch (ObjectNotFoundException ex) {
            Logger.msg("Creating server item "+thisServerPath);
            serverEntity = new ItemPath();
            Gateway.getCorbaServer().createItem(serverEntity);
            lookupManager.add(serverEntity);
            thisServerPath.setItemPath(serverEntity);
            lookupManager.add(thisServerPath);
        }
        Gateway.getStorage().put(serverEntity, new Property("Name", serverName, false), null);
        Gateway.getStorage().put(serverEntity, new Property("Type", "Server", false), null);
        Gateway.getStorage().put(serverEntity, new Property("KernelVersion", Gateway.getKernelVersion(), true), null);
        int proxyPort = Gateway.getProperties().getInt("ItemServer.Proxy.port", 1553);
        Gateway.getStorage().put(serverEntity,
                    new Property("ProxyPort", String.valueOf(proxyPort), false), null);
        Gateway.getStorage().put(serverEntity,
                    new Property("ConsolePort", String.valueOf(Logger.getConsolePort()), true), null);
        Gateway.getProxyManager().connectToProxyServer(serverName, proxyPort);

    }

    public static void initServerItemWf() throws Exception {
    	CompositeActivityDef serverWfCa = (CompositeActivityDef)LocalObjectLoader.getActDef("ServerItemWorkflow", 0);
        Workflow wf = new Workflow((CompositeActivity)serverWfCa.instantiate(), new ServerPredefinedStepContainer());
        wf.initialise(thisServerPath.getItemPath(), systemAgents.get("system").getPath());
        Gateway.getStorage().put(thisServerPath.getItemPath(), wf, null);
    }
}
