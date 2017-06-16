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
import static org.cristalise.kernel.property.BuiltInItemProperties.COMPLEXITY;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAMESPACE;
import static org.cristalise.kernel.property.BuiltInItemProperties.MODULE;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;
import static org.cristalise.kernel.property.BuiltInItemProperties.VERSION;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
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
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Bootstrap;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.resource.BuiltInResources;
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
        properties.add(new Property(TYPE, "Module", false));

        setInitialPath(BuiltInResources.MODULE_RESOURCE.getTypeRoot());
        setWorkflow(   BuiltInResources.MODULE_RESOURCE.getWorkflowDef());
        setWorkflowVer(0);

        //Module has one built-in Dependency
        dependencyList.add(new ImportDependency(CONTENTS));
    }

    public void runScript(String event, AgentProxy agent, boolean isServer) throws ScriptingEngineException {
        for (ModuleScript script : scripts) {
            if (script.shouldRun(event, isServer)) {
                Logger.msg("Running "+script.event+" "+script.target+" script from "+name);
                Object result = script.getScript(ns, agent).execute();
                if (result instanceof ErrorInfo) {
                    ErrorInfo error = (ErrorInfo) result;
                    Logger.error(error.toString());

                    if (error.getFatal()) throw new ScriptingEngineException("Fatal Script Error");
                }
                else if (result != null) {
                    Logger.msg(result.toString());
                }
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
        replaceProp(new Property(NAMESPACE, ns, false));
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        replaceProp(new Property(NAME, name, false));
    }

    private void replaceProp(Property newProp) {
        for (Property prop : properties) {
            if (prop.getName().equals(newProp.getName())) {
                prop.setMutable(newProp.isMutable());
                prop.setValue(newProp.getValue());
                return;
            }
        }
        properties.add(newProp);
    }
    
    private void addItemToContents(Path itemPath) {
        ImportDependency contents = dependencyList.get(0);
        
        contents.dependencyMemberList.add(new ImportDependencyMember(itemPath.toString()));
    }

    /**
     * Imports all resources defined in the Module in this order: Resources, Roles, Agents, Items and Module itself
     * 
     * @param serverEntity not used at the moment but required to implement the import as the workflow of the serverItem
     * @param systemAgent system agent used during the import
     * @param reset whether to reset or not the version of the created/updated resource
     * @throws Exception All possible exceptions
     */
    public void importAll(ItemProxy serverEntity, AgentProxy systemAgent, boolean reset) throws Exception {
        if (!Bootstrap.shutdown) importResources(systemAgent, reset);
        if (!Bootstrap.shutdown) importRoles(    systemAgent, reset);
        if (!Bootstrap.shutdown) importAgents(   systemAgent, reset);
        if (!Bootstrap.shutdown) importItems(    systemAgent, reset);

        //Finally create this Module Item
        if (!Bootstrap.shutdown) this.create(systemAgent.getPath(), reset);
    }

    /**
     * @param systemAgent
     * @param reset
     * @throws Exception
     */
    private void importItems(AgentProxy systemAgent, boolean reset) throws Exception {
        for (ImportItem thisItem : imports.getItems()) {
            if (Bootstrap.shutdown) return;

            thisItem.setNamespace(ns);
            addItemToContents( thisItem.create(systemAgent.getPath(), reset) );
        }
    }

    /**
     * @param systemAgent
     * @param reset
     * @throws Exception
     */
    private void importAgents(AgentProxy systemAgent, boolean reset) throws Exception {
        for (ImportAgent thisAgent : imports.getAgents()) {
            if (Bootstrap.shutdown) return;

            try {
                Gateway.getLookup().getAgentPath(thisAgent.name);
                Logger.msg(3, "Module.importAgents() - Agent '"+thisAgent.name+"' found.");
                continue;
            }
            catch (ObjectNotFoundException ex) { }

            Logger.msg("Module.importAgents() - Agent '"+thisAgent.name+"' not found. Creating.");
            addItemToContents( thisAgent.create(systemAgent.getPath(), reset) );
        }
    }

    /**
     * @param systemAgent
     * @param reset
     * @throws Exception
     */
    private void importRoles(AgentProxy systemAgent, boolean reset) throws Exception {
        for (ImportRole thisRole : imports.getRoles()) {
            if (Bootstrap.shutdown) return;

            thisRole.create(systemAgent.getPath(), reset);
        }
    }

    /**
     * @param systemAgent
     * @param reset
     */
    private void importResources(AgentProxy systemAgent, boolean reset) {
        for (ModuleResource thisRes : imports.getResources()) {
            if (Bootstrap.shutdown) return;

            try {
                thisRes.setNamespace(ns);
                addItemToContents( thisRes.create(systemAgent.getPath(), reset) );
            }
            catch (Exception ex) {
                Logger.error(ex);
                Logger.die("Error importing module resources. Unsafe to continue.");
            }
        }
    }

    /**
     * Returns all Properties
     * 
     * @param isServer is it a server Property or not
     * @return the Properties
     */
    public Properties getProperties(boolean isServer) {
        Properties props = new Properties();

        for (ModuleConfig thisProp : config) {
            if (thisProp.include(isServer)) props.put(thisProp.name, thisProp.value);
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
        replaceProp(new Property(VERSION, info.version, true));
    }

    public ModuleImports getImports() {
        return imports;
    }

    /**
     * Add dependency for all children
     * 
     * @param theImports imported resources
     */
    public void setImports(ModuleImports theImports) {
        imports = theImports;
    }

    /**
     * Overwrites the imports with the content of this Collection  
     * 
     * @param contents the Collection to be used as a list of imports
     * @throws ObjectNotFoundException the data was not found
     * @throws InvalidDataException the data was invalid
     */
    public void setImports(Collection<?> contents) throws ObjectNotFoundException, InvalidDataException {
        imports.list.clear();
        addImports(contents);
    }

    /**
     * Adds the members of this Collection recursively to the imports of this Module. It checks if the Item
     * referenced by the member has a Collections or not, and adds all of members of those Collection as well.
     * 
     * @param contents the Collection to be added as a list of imports
     * @throws ObjectNotFoundException the data was not found
     * @throws InvalidDataException the data was invalid
     */
    public void addImports(Collection<?> contents) throws ObjectNotFoundException, InvalidDataException {
        for (CollectionMember mem : contents.getMembers().list) {
            if (mem.getItemPath() != null) {
                ItemProxy    child   = mem.resolveItem();
                String       name    = child.getName();
                Integer      version = Integer.valueOf(mem.getProperties().get(VERSION.getName()).toString());
                String       type    = child.getProperty(TYPE);
                ModuleImport newImport;
                
                switch (type) {
                    case "ActivityDesc":
                        String complex = child.getProperty(COMPLEXITY);

                        if (complex.equals("Elementary")) newImport = new ModuleActivity(child, version);
                        else                              newImport = new ModuleWorkflow(child, version);

                        break;

                    case "Script":
                    case "Query":
                    case "StateMachine":
                    case "Schema":
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
                        // check if child already assigned to a different module
                        String childModule = child.getProperty(MODULE);
                        if (StringUtils.isNotBlank(childModule) && !childModule.equals(getNamespace())) 
                            return;
                    }
                    catch (ObjectNotFoundException ex) { }// no module property, ok to include

                    imports.list.add(newImport);

                    for (String collName : child.getContents(ClusterStorage.COLLECTION)) {
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
