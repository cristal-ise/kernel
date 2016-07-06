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
package org.cristalise.kernel.process.module;

import static org.cristalise.kernel.collection.BuiltInCollections.CONTENTS;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.imports.ImportDependency;
import org.cristalise.kernel.entity.imports.ImportDependencyMember;
import org.cristalise.kernel.entity.imports.ImportItem;
import org.cristalise.kernel.entity.imports.ImportOutcome;
import org.cristalise.kernel.entity.imports.ImportRole;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Bootstrap;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.Logger;


public class Module extends ImportItem {

	private ModuleInfo info;
	private String resURL;
	private ModuleImports imports = new ModuleImports();
	private ArrayList<ModuleConfig> config = new ArrayList<ModuleConfig>();
	private ArrayList<ModuleScript> scripts = new ArrayList<ModuleScript>();
    
	public Module() {
		super();
		// Module properties
		properties.add(new Property("Type", "Module", false));
		setInitialPath("/desc/modules/");
		setWorkflow("NoWorkflow");
		setWorkflowVer(0);
		imports.list.add(this);
	}
	
	public void runScript(String event, AgentProxy user, boolean isServer) throws ScriptingEngineException {
		for (ModuleScript script : scripts) {
			if (script.shouldRun(event, isServer)) {
				Logger.msg("Running "+script.event+" "+script.target+" script from "+name);
				Object result = script.getScript(ns, user).execute();
				if (result instanceof ErrorInfo) {
					ErrorInfo error = (ErrorInfo) result;
					Logger.error(error.toString());
					if (error.getFatal())
						throw new ScriptingEngineException("Fatal Script Error");
				}
				else if (result != null)
					Logger.msg(result.toString());
			}
		}
	}
	
	public void setModuleXML(String moduleXML) {
		ImportOutcome moduleOutcome = new ImportOutcome("Module", 0, "last", null);
		moduleOutcome.data = moduleXML;
		outcomes.add(moduleOutcome);
	}
	
	@Override
	public void setNamespace(String ns) {
		super.setNamespace(ns);
		replaceProp(new Property("Namespace", ns, false));
	}

	@Override
	public void setName(String name) {
		super.setName(name);
		replaceProp(new Property("Name", name, false));
	}

	private void replaceProp(Property newProp) {
		for (Property prop : properties) {
			if (prop.getName().equals("Namespace")) {
				prop.setMutable(newProp.isMutable());
				prop.setValue(newProp.getValue());
				return;
			}
		}
		properties.add(newProp);
	}
	public void importAll(ItemProxy serverEntity, AgentProxy systemAgent, String moduleXML, boolean reset) throws Exception {
		setModuleXML(moduleXML);
		
		for (ModuleResource thisRes : imports.getResources()) {
			if (Bootstrap.shutdown) return;
			try {
				thisRes.setNamespace(ns);
				thisRes.create(systemAgent.getPath(), reset);
			} catch (Exception ex) {
				Logger.error(ex);
				Logger.die("Error importing module resources. Unsafe to continue.");
			}
		}

		for (ImportRole thisRole : imports.getRoles()) {
			if (Bootstrap.shutdown) return;
			RolePath rolePath;
			try {
				String roleName = thisRole.name;
				if (roleName.indexOf('/') > -1) roleName = roleName.substring(roleName.indexOf('/')+1);
				rolePath = Gateway.getLookup().getRolePath(roleName);
				if (rolePath.hasJobList() != thisRole.hasJobList()) {
					Logger.msg("Module.importAll() - Role '"+thisRole.name+"' has incorrect joblist settings. Correcting.");
					rolePath.setHasJobList(thisRole.hasJobList());
					Gateway.getLookupManager().createRole(rolePath);
				}
			} catch (ObjectNotFoundException ex) {
				Logger.msg("Module.importAll() - Role '"+thisRole.name+"' not found. Creating.");
				thisRole.create(systemAgent.getPath(), reset);
			}
		}
		
		for (ImportAgent thisAgent : imports.getAgents()) {
			if (Bootstrap.shutdown) return;
			try {
				Gateway.getLookup().getAgentPath(thisAgent.name);
			    Logger.msg(3, "Module.importAll() - User '"+thisAgent.name+"' found.");
			    continue;
			 } catch (ObjectNotFoundException ex) { }
			 Logger.msg("Module.importAll() - User '"+thisAgent.name+"' not found. Creating.");
			thisAgent.create(systemAgent.getPath(), reset);
		}
		
		for (ImportItem thisItem : imports.getItems()) {
			if (Bootstrap.shutdown) return;
			thisItem.setNamespace(ns);
			thisItem.create(systemAgent.getPath(), reset);
		}
		
		// Import the module item
		this.create(systemAgent.getPath(), reset);
		
	}
	
	public Properties getProperties(boolean isServer) {
		Properties props = new Properties();
		for (ModuleConfig thisProp : config) {
			if (thisProp.include(isServer))
				props.put(thisProp.name, thisProp.value);
		}
		return props;
	}

	public ArrayList<ModuleScript> getScripts() {
		return scripts;
	}

	public void setResURL(String resURL) {
		this.resURL = resURL;
	}
	public String getDesc() {
		return info.desc;
	}
	public String getVersion() {
		return info.version;
	}
	public String getResURL() {
		return resURL;
	}
	public ArrayList<String> getDependencies() {
		return info.dependency;
	}
	public boolean hasDependency(String dep) {
		return info.dependency.contains(dep);
	}

	public ModuleInfo getInfo() {
		return info;
	}

	public void setInfo(ModuleInfo info) {
		this.info = info;
		replaceProp(new Property("Version", info.version, true));
	}

	public ModuleImports getImports() {
		return imports;
	}
	
	public void setImports(ModuleImports imp) {
		// Add dependency for all children
		imports = imp;
		ImportDependency children = new ImportDependency(CONTENTS);
		for (ModuleImport thisImport : imports.list) {
			DomainPath path = thisImport.domainPath;
			if (path != null)
			children.dependencyMemberList.add(new ImportDependencyMember(path.toString()));
		}
		dependencyList.add(children);
	}
	
	public void setImports(Collection<?> contents) throws ObjectNotFoundException, InvalidDataException {
		imports.list.clear();
		addImports(contents);
	}
	
	public void addImports(Collection<?> contents) throws ObjectNotFoundException, InvalidDataException {
		for (CollectionMember mem : contents.getMembers().list) {
			if (mem.getItemPath() != null) {
				ItemProxy child = mem.resolveItem();
				String name = child.getName();
				Integer version = Integer.valueOf(mem.getProperties().get("Version").toString());
				String type = child.getProperty("Type");
				ModuleImport newImport;
				switch (type) {
				case "ActivityDesc":
				    String complex = child.getProperty("Complexity");
				    if (complex.equals("Elementary")) {
						newImport = new ModuleActivity(child, version);
				    	break;
				    }
				    newImport = new ModuleWorkflow(child, version);
				    break;
				case "Script":
				case "StateMachine":
				case "OutcomeDesc":
					newImport = new ModuleResource();
					break;
				default:
					throw new InvalidDataException("Resource type '"+type+"' unknown for module export");
				}
				newImport.setName(name);
				newImport.setItemPath(mem.getItemPath());
				newImport.setNamespace(getNamespace());
				if (!imports.list.contains(newImport)) {
					try {
						String currentModule = child.getProperty("Module");
						if (currentModule != null && currentModule.length() > 0 &&
								!currentModule.equals(getNamespace())) // already assigned to a different module
							return;
					} catch (ObjectNotFoundException ex) { // no module property, ok to include
					}
					imports.list.add(newImport);
					String[] colls = child.getContents(ClusterStorage.COLLECTION);
					for (String collName : colls) {
						Collection<?> childColl = child.getCollection(collName, version);
						addImports(childColl);
					}
				}
						
			}
		}
	}
		
	public void setConfig(ArrayList<ModuleConfig> config) {
		this.config = config;
	}
	
	public void setScripts(ArrayList<ModuleScript> scripts) {
		this.scripts = scripts;
	}

	public ArrayList<ModuleConfig> getConfig() {
		return config;
	}
	
	public void export(File location) {
		
	}
}
