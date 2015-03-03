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
package org.cristalise.kernel.entity.imports;


import java.util.ArrayList;

import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.TraceableEntity;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.module.ModuleImport;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;


/**
 * Complete Structure for new item
 *
 * @version $Revision: 1.8 $ $Date: 2006/03/03 13:52:21 $
 */

public class ImportItem extends ModuleImport {

    protected String initialPath;
    protected String workflow;
    protected Integer workflowVer;
    protected ArrayList<Property> properties  = new ArrayList<Property>();
    protected ArrayList<ImportAggregation> aggregationList = new ArrayList<ImportAggregation>();
    protected ArrayList<ImportDependency> dependencyList = new ArrayList<ImportDependency>();
    protected ArrayList<ImportOutcome> outcomes = new ArrayList<ImportOutcome>();

    public ImportItem() {
    }

    public ImportItem(String ns, String name, String initialPath, ItemPath itemPath, String wf, int wfVer) {
        this();
        setNamespace(ns);
        setName(name);
        setItemPath(itemPath);
        setInitialPath(initialPath);
        setWorkflow(wf);
        setWorkflowVer(wfVer);
    }
    
    @Override
    public ItemPath getItemPath() {
        if (itemPath == null) { // try to find item if it already exists
        	DomainPath existingItem = new DomainPath(initialPath+"/"+name);
        	if (existingItem.exists()) {
        		try {
        			itemPath = existingItem.getItemPath();
        		} catch (ObjectNotFoundException ex) { }
        	}
        }
        if (itemPath == null) itemPath = new ItemPath();
        return itemPath;
    }

	@Override
	public void setNamespace(String ns) {
		super.setNamespace(ns);
		if (initialPath == null) initialPath = "/desc/"+ns;
	}

	@Override
	public void setName(String name) {
		super.setName(name);
	}

	@Override
	public void create(AgentPath agentPath, boolean reset) throws ObjectCannotBeUpdated, ObjectNotFoundException, CannotManageException, ObjectAlreadyExistsException, InvalidCollectionModification {
        DomainPath domPath = new DomainPath(new DomainPath(initialPath), name);
        if (domPath.exists()) {
        	ItemPath domItem = domPath.getItemPath();
        	if (!getItemPath().equals(domItem))
        		throw new CannotManageException("Item "+domPath+" was found with the wrong itemPath ("+domPath.getItemPath()+" vs "+getItemPath()+")");
        }
        
        TraceableEntity newItem;
        if (getItemPath().exists()) {
        	Logger.msg(1, "ImportItem.create() - Verifying module item "+getItemPath()+" at "+domPath);
        	newItem = Gateway.getCorbaServer().getItem(getItemPath());
        } 
        else {
        	Logger.msg(1, "ImportItem.create() - Creating module item "+getItemPath()+" at "+domPath);
	        newItem = Gateway.getCorbaServer().createItem(getItemPath());
	        Gateway.getLookupManager().add(getItemPath());
        }
        
        // set the name property
        properties.add(new Property("Name", name, true));

        // find workflow def
        CompositeActivityDef compact;
    	// default workflow version is 0 if not given
    	int usedWfVer;
    	if (workflowVer == null) usedWfVer = 0;
    	else usedWfVer = workflowVer.intValue();
        try {
        	compact = (CompositeActivityDef)LocalObjectLoader.getActDef(workflow, usedWfVer);
        } catch (ObjectNotFoundException ex) {
        	throw new CannotManageException("Could not find workflow "+workflow+"v"+usedWfVer+" for item "+domPath);
        } catch (InvalidDataException e) {
        	throw new CannotManageException("Workflow def "+workflow+" v"+usedWfVer+" for item "+domPath+" was not valid");
		}
        
        // create collections
        CollectionArrayList colls = new CollectionArrayList();
        for (ImportDependency element: dependencyList) {
           	Dependency newDep = element.create();
           	colls.put(newDep);
        }

        for (ImportAggregation element : aggregationList) {
           	Aggregation newAgg = element.create();           	
           	colls.put(newAgg);
        }
 
    	// (re)initialise the new item with properties, workflow and collections
        try {
            newItem.initialise(
            		agentPath.getSystemKey(),
                    Gateway.getMarshaller().marshall(new PropertyArrayList(properties)),
                    Gateway.getMarshaller().marshall(compact.instantiate()),
                    Gateway.getMarshaller().marshall(colls));
        } catch (Exception ex) {
            Logger.error("Error initialising new item "+ns+"/"+name );
            Logger.error(ex);
            throw new CannotManageException("Problem initialising new item. See server log.");
        }

        // import outcomes
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        History hist = new History(getItemPath(), null);
        for (ImportOutcome thisOutcome : outcomes) {
            Outcome newOutcome;
			try {
				newOutcome = new Outcome(-1, thisOutcome.getData(ns), thisOutcome.schema, thisOutcome.version);
			} catch (InvalidDataException e1) {
				throw new ObjectCannotBeUpdated("XML is not valid in view "+thisOutcome.schema+"/"+thisOutcome.viewname+" in "+ns+"/"+name);
			}
        	Viewpoint impView;
        	try {
        		impView = (Viewpoint)Gateway.getStorage().get(getItemPath(), ClusterStorage.VIEWPOINT+"/"+thisOutcome.schema+"/"+thisOutcome.viewname, null);

                Diff xmlDiff = new Diff(newOutcome.getDOM(), impView.getOutcome().getDOM());
                if (xmlDiff.identical()) {
                    Logger.msg(5, "NewItem.create() - View "+thisOutcome.schema+"/"+thisOutcome.viewname+" in "+ns+"/"+name+" identical, no update required");
                    continue;
                }
                else {
                	Logger.msg("NewItem.create() - Difference found in view "+thisOutcome.schema+"/"+thisOutcome.viewname+" in "+ns+"/"+name+": "+xmlDiff.toString());
                	if (!reset && !impView.getEvent().getStepPath().equals("Import")) {
                		Logger.msg("Last edit was not done by import, and reset not requested. Not overwriting.");
                		continue;
                	}
                }
        	} catch (ObjectNotFoundException ex) { 
        		Logger.msg(3, "View "+thisOutcome.schema+"/"+thisOutcome.viewname+" not found in "+ns+"/"+name+". Creating.");
        		impView = new Viewpoint(getItemPath(), thisOutcome.schema, thisOutcome.viewname, thisOutcome.version, -1);
        	} catch (PersistencyException e) {
        		throw new ObjectCannotBeUpdated("Could not check data for view "+thisOutcome.schema+"/"+thisOutcome.viewname+" in "+ns+"/"+name);
			} catch (InvalidDataException e) {
				throw new ObjectCannotBeUpdated("Could not check previous event for view "+thisOutcome.schema+"/"+thisOutcome.viewname+" in "+ns+"/"+name);
			}
        	
        	// write new view/outcome/event
        	Transition predefDone = new Transition(0, "Done", 0, 0);
            Event newEvent = hist.addEvent(agentPath, "Admin", "Import", "Import", "Import", thisOutcome.schema, thisOutcome.version, "PredefinedStep", 0, predefDone, thisOutcome.viewname);
            newOutcome.setID(newEvent.getID());
            impView.setEventId(newEvent.getID());
            try {
				Gateway.getStorage().put(getItemPath(), newOutcome, null);
				Gateway.getStorage().put(getItemPath(), impView, null);
			} catch (PersistencyException e) {
				throw new ObjectCannotBeUpdated("Could not store data for view "+thisOutcome.schema+"/"+thisOutcome.viewname+" in "+ns+"/"+name);
			}
		}
        
        // register domain path (before collections in case of recursive collections)
        if (!domPath.exists()) {
        	domPath.setItemPath(getItemPath());
        	Gateway.getLookupManager().add(domPath);
        }
    }
	
    public String getInitialPath() {
		return initialPath;
	}

	public void setInitialPath(String initialPath) {
		this.initialPath = initialPath;
	}

	public String getWorkflow() {
		return workflow;
	}

	public void setWorkflow(String workflow) {
		this.workflow = workflow;
	}

	public Integer getWorkflowVer() {
		return workflowVer;
	}

	public void setWorkflowVer(Integer workflowVer) {
		this.workflowVer = workflowVer;
	}

	public ArrayList<Property> getProperties() {
		return properties;
	}

	public void setProperties(ArrayList<Property> properties) {
		this.properties = properties;
	}

	public ArrayList<ImportAggregation> getAggregationList() {
		return aggregationList;
	}

	public void setAggregationList(ArrayList<ImportAggregation> aggregationList) {
		this.aggregationList = aggregationList;
	}

	public ArrayList<ImportDependency> getDependencyList() {
		return dependencyList;
	}

	public void setDependencyList(ArrayList<ImportDependency> dependencyList) {
		this.dependencyList = dependencyList;
	}

	public ArrayList<ImportOutcome> getOutcomes() {
		return outcomes;
	}

	public void setOutcomes(ArrayList<ImportOutcome> outcomes) {
		this.outcomes = outcomes;
	}

}
