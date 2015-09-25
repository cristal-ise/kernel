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
package org.cristalise.kernel.process.module;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.persistency.outcome.OutcomeValidator;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Bootstrap;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;


public class ModuleManager {
	ArrayList<Module> modules = new ArrayList<Module>();
	HashMap<String, String> modulesXML = new HashMap<String, String>();
	Properties props = new Properties();
	AgentProxy user;
	boolean isServer;
	OutcomeValidator moduleValidator;

	public ModuleManager(Enumeration<URL> moduleEnum, boolean isServer) throws ModuleException {
		this.isServer = isServer;
		try {
			Schema moduleSchema = new Schema("Module", 0,
					FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/OD/Module.xsd")));
			moduleValidator = new OutcomeValidator(moduleSchema);
		} catch (InvalidDataException ex) {
			Logger.error(ex);
			throw new ModuleException("Module Schema is not valid");
		} catch (IOException ex) {
			throw new ModuleException("Could not load Module Schema from kernel resources");
		}
		ArrayList<String> loadedModules = new ArrayList<String>();
		ArrayList<String> moduleNs = new ArrayList<String>();
		while(moduleEnum.hasMoreElements()) {
			URL newModuleURL = moduleEnum.nextElement();
			try {
				String moduleXML = FileStringUtility.url2String(newModuleURL);
				String errors = moduleValidator.validate(moduleXML);
				if (errors.length() > 0)
					throw new ModuleException("Module XML found at "+newModuleURL+" was not valid: "+errors);
				Module newModule = (Module)Gateway.getMarshaller().unmarshall(moduleXML);
				if (newModule.getResURL() != null && newModule.getResURL().length()>0) Gateway.getResource().addModuleBaseURL(newModule.getNamespace(), newModule.getResURL());
				modules.add(newModule);
				modulesXML.put(newModule.getNamespace(), moduleXML);
				if (loadedModules.contains(newModule.getName())) throw new ModuleException("Module name clash: "+newModule.getName());
				if (moduleNs.contains(newModule.getNamespace())) throw new ModuleException("Module namespace clash: "+newModule.getNamespace());
				Logger.debug(4, "Module found: "+newModule.getNamespace()+" - "+newModule.getName());
				loadedModules.add(newModule.getName()); moduleNs.add(newModule.getNamespace());
			} catch (ModuleException e) {
				Logger.error("Could not load module description from "+newModuleURL);
				throw e;
			} catch (Exception e) {
				Logger.error(e);
				throw new ModuleException("Could not load module.xml from "+newModuleURL);
			}
		}
		
		Logger.debug(5, "Checking dependencies");
		boolean allDepsPresent = true;
		
		ArrayList<String> prevModules = new ArrayList<String>();
		for (int i=0; i<modules.size();i++) {
			boolean depClean = false;
			int skipped = 0;
			Module thisMod = modules.get(i);
			Logger.msg(5, "Checking dependencies of module "+thisMod.getName());
			while (!depClean) {
				ArrayList<String> deps = thisMod.getDependencies();
				depClean = true;
				for (String dep : deps) {
					Logger.msg(6, thisMod.getName()+" depends on "+dep);
					if (!loadedModules.contains(dep)) {
						Logger.error("UNMET MODULE DEPENDENCY: "+thisMod.getName()+" requires "+dep);
						allDepsPresent = false;
					}
					else if (!prevModules.contains(dep)) {
						Logger.msg(1, "ModuleManager: Shuffling "+thisMod.getName()+" to the end to fulfil dependency on "+dep);
						modules.remove(i);
						modules.add(thisMod);
						thisMod = modules.get(i);
						skipped++;
						depClean = false;
						break;
					}
				}
				if (skipped > modules.size()-i) {
					StringBuffer badMod = new StringBuffer();
					for (Module mod : modules.subList(i, modules.size())) {
						badMod.append(mod.getName()).append(" ");
					}
					Logger.die("Circular module dependencies involving: "+badMod);
				}
			}
			// Current module is 'next', this is the correct order to load the properties
			Properties modProp = thisMod.getProperties(isServer);
	        for (Enumeration<?> e = modProp.propertyNames(); e.hasMoreElements();) {
	            String propName = (String)e.nextElement();
	            props.put(propName, modProp.get(propName));
	        }
			prevModules.add(thisMod.getName());
		}
		if (!allDepsPresent) Logger.die("Unmet module dependencies. Cannot continue");
	}

	public void setUser(AgentProxy user) {
		this.user = user;
	}

	public String getModuleVersions() {
		StringBuffer ver = new StringBuffer();
		for (Module thisMod : modules) {
			if (ver.length()>0) ver.append("; ");
			ver.append(thisMod.getName()+" ("+thisMod.getVersion()+")");
		}
		return ver.toString();
	}
	
	
	public Properties getAllModuleProperties() {
		return props;
	}
	
	public void runScripts(String event) {
		for (Module thisMod : modules) {
			if (Bootstrap.shutdown) return;
			try {
				thisMod.runScript(event, user, isServer);
			} catch (ScriptingEngineException e) {
				Logger.error(e);
				Logger.die(e.getMessage());
			}
		}
	}
	
	public void registerModules() throws ModuleException {
		ItemProxy serverItem;
		try {
			serverItem = Gateway.getProxyManager().getProxy(new DomainPath("/servers/"+Gateway.getProperties().getString("ItemServer.name")));
		} catch (ObjectNotFoundException e) {
			throw new ModuleException("Cannot find local server name.");
		}
		Logger.debug(3, "Registering modules");

		boolean reset = Gateway.getProperties().getBoolean("Module.reset", false);

		for (Module thisMod : modules) {
			if (Bootstrap.shutdown) return; 
			Logger.msg("Registering module "+thisMod.getName());
			try {
				String thisResetKey = "Module."+thisMod.getNamespace()+".reset";
				boolean thisReset = reset;
				if (Gateway.getProperties().containsKey(thisResetKey))
					thisReset = Gateway.getProperties().getBoolean(thisResetKey);
				thisMod.importAll(serverItem, user, modulesXML.get(thisMod.getNamespace()), thisReset);
			} catch (Exception e) {
				Logger.error(e);
				throw new ModuleException("Error importing items for module "+thisMod.getName());
			}
			Logger.msg("Module "+thisMod.getName()+" registered");
			
			try {
				thisMod.runScript("startup", user, true);
			} catch (ScriptingEngineException e) {
				Logger.error(e);
				throw new ModuleException("Error in startup script for module "+thisMod.getName());
			}
			
		}
	}

	public void dumpModules() {
		for (Module thisMod : modules) {
			try {
				FileStringUtility.string2File(thisMod.getName()+".xml", Gateway.getMarshaller().marshall(thisMod));
			} catch (Exception e) {
				Logger.error(e);
			}
		}
		
	}
}
