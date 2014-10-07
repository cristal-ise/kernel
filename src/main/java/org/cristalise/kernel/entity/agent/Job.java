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
package org.cristalise.kernel.entity.agent;

import java.util.HashMap;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.OutcomeInitiator;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.Logger;


/*******************************************************************************
 * @author $Author: abranson $ $Date: 2005/05/20 13:07:49 $
 * @version $Revision: 1.62 $
 ******************************************************************************/

public class Job implements C2KLocalObject
{
	// persistent
	
    private int id;

    private ItemPath itemPath;

    private String stepName;
    
    private String stepPath;
    
    private String stepType;
    
    private Transition transition;
    
    private String originStateName;
    
    private String targetStateName;

    private String agentRole;

    private CastorHashMap actProps = new CastorHashMap();
    
    // non-persistent
    
    private String name;
    
    private AgentPath agentPath;
    
    private String agentName;
    
    private String outcomeData;
    
    private ErrorInfo error;

    private ItemProxy item = null;

    private boolean outcomeSet;
    
    // outcome initiator cache
    
    static private HashMap<String, OutcomeInitiator> ocInitCache = new HashMap<String, OutcomeInitiator>();

    /***************************************************************************
     * Empty constructor for Castor
     **************************************************************************/
    public Job()
    {
    }

    public Job(Activity act, ItemPath itemPath, Transition transition, AgentPath agent, String role) throws InvalidDataException, ObjectNotFoundException, InvalidAgentPathException {
        
    	setItemPath(itemPath);
        setStepPath(act.getPath());
        setTransition(transition);
        setOriginStateName(act.getStateMachine().getState(transition.getOriginStateId()).getName());
        setTargetStateName(act.getStateMachine().getState(transition.getTargetStateId()).getName());
        setStepName(act.getName());
        setActProps(act.getProperties());
        setStepType(act.getType());
        if (agent != null) setAgentName(agent.getAgentName());
        setAgentRole(role);
    }

    
    // Castor persistent fields
    
    public String getOriginStateName() {
		return originStateName;
	}

	public void setOriginStateName(String originStateName) {
		this.originStateName = originStateName;
	}

	public String getTargetStateName() {
		return targetStateName;
	}

	public void setTargetStateName(String targetStateName) {
		this.targetStateName = targetStateName;
	}

	public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
        name = String.valueOf(id);
    } 
    
	public ItemPath getItemPath() {
        return itemPath;
    }
	
    public void setItemPath(ItemPath path) {
    	itemPath = path;
        item = null;
    }	

    public void setItemUUID( String uuid ) throws InvalidItemPathException
    {
    	setItemPath(new ItemPath(uuid));
    }
    
    public String getItemUUID() {
    	return getItemPath().getUUID().toString();
    }
    
    public String getStepName() {
        return stepName;
    }

    public void setStepName(String string) {
        stepName = string;
    }

    public String getStepPath() {
        return stepPath;
    }

    public void setStepPath(String string) {
        stepPath = string;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String actType) {
        stepType = actType;
    }
    
    public Transition getTransition() {
    	return transition;
    }
    
    public void setTransition(Transition transition) {
    	this.transition = transition;
    }

    public AgentPath getAgentPath() throws ObjectNotFoundException {
    	if (agentPath == null && getAgentName() != null) {
    		agentPath = Gateway.getLookup().getAgentPath(getAgentName());
    	}
        return agentPath;
    }

    public void setAgentPath(AgentPath agentPath) {
    	this.agentPath = agentPath;
    	agentName = agentPath.getAgentName();
    }
    
    public void setAgentUUID( String uuid )
    {
    	if (uuid != null) 
    		try {
				setAgentPath(AgentPath.fromUUIDString(uuid));
			} catch (InvalidAgentPathException e) {
				Logger.error("Invalid agent path in Job: "+uuid);
			}
    }
    
    public String getAgentUUID() {
    	try {
			if (getAgentPath() != null)
				return getAgentPath().getUUID().toString();
		} catch (ObjectNotFoundException e) { }
    	return null;
    }
    
    public String getAgentName()
    {
        if (agentName == null)
            agentName = (String) actProps.get("Agent Name");
        return agentName;
    }

    public void setAgentName(String agentName) throws ObjectNotFoundException
    {
        this.agentName = agentName;
        agentPath = Gateway.getLookup().getAgentPath(agentName);
    }
    
    public String getAgentRole() {
        return agentRole;
    }

    public void setAgentRole(String role) {
        agentRole = role;
    }
    
    public String getSchemaName() throws InvalidDataException, ObjectNotFoundException {
    	if (transition.hasOutcome(actProps)) {
			Schema schema = transition.getSchema(actProps);
			return schema.docType;
    	}
   		return null;
    }
    
    public int getSchemaVersion() throws InvalidDataException, ObjectNotFoundException {
    	if (transition.hasOutcome(actProps)) {
			Schema schema = transition.getSchema(actProps);
       		return schema.docVersion;
    	}
   		return -1;
    }

    public boolean isOutcomeRequired()
    {
        return transition.hasOutcome(actProps) && transition.getOutcome().isRequired();
    }
    
    public String getScriptName() {
    	if (transition.hasScript(actProps)) {
			return transition.getScript().getScriptName();
    	}
   		return null;
	}

	public int getScriptVersion() throws InvalidDataException {
    	if (transition.hasScript(actProps)) {
			return transition.getScriptVersion(actProps);
    	}
   		return -1;
	}
    
    public KeyValuePair[] getKeyValuePairs() {
        return actProps.getKeyValuePairs();
    }

    public void setKeyValuePairs(KeyValuePair[] pairs) {
        actProps.setKeyValuePairs(pairs);
    }
    
    // Non-persistent fields
    
    @Override
	public String getName() {
        return name;
    }
    
    @Override
	public void setName(String name) {
        this.name = name;
        try {
            id = Integer.parseInt(name);
        } catch (NumberFormatException ex) {
            id = -1;
        }
    }
    
    public ItemProxy getItemProxy() throws ObjectNotFoundException, InvalidItemPathException {
        if (item == null)
            item = Gateway.getProxyManager().getProxy(itemPath);
        return item;
    }

    public String getDescription()
    {
        String desc = (String) actProps.get("Description");
        if (desc == null)
            desc = "No Description";
        return desc;
    }
	public void setOutcome(String outcome)
    {
		outcomeData = outcome;
        outcomeSet = !(outcomeData == null);
    }
    
    public void setError(ErrorInfo errors)
    {
    	error = errors;
    	try {
			outcomeData = Gateway.getMarshaller().marshall(error);
		} catch (Exception e) {
			Logger.error("Error marshalling ErrorInfo in job");
			Logger.error(e);
		} 
    }
    
    public String getLastView() throws InvalidDataException {
		String viewName = (String) getActProp("Viewpoint");
		if (viewName.length() > 0) {
			// find schema
			String schemaName;
			try {
				schemaName = getSchemaName();
			} catch (ObjectNotFoundException e1) {
				throw new InvalidDataException("Schema "+getActProp("SchemaType")+" v"+getActProp("SchemaVersion")+" not found");
			}
			
			try	{
				Viewpoint view = (Viewpoint) Gateway.getStorage().get(itemPath, 
						ClusterStorage.VIEWPOINT + "/" + schemaName + "/" + viewName, null);
				return view.getOutcome().getData();
        	} catch (ObjectNotFoundException ex) { // viewpoint doesn't exist yet
        		return null;
			} catch (PersistencyException e) {
				Logger.error(e);
				throw new InvalidDataException("ViewpointOutcomeInitiator: PersistencyException loading viewpoint " 
						+ ClusterStorage.VIEWPOINT + "/" + schemaName + "/" + viewName+" in item "+itemPath.getUUID());
			}
		}
		else
			return null;
	}
    
    public OutcomeInitiator getOutcomeInitiator() throws InvalidDataException {
    	String ocInitName = (String) getActProp("OutcomeInit");
    	OutcomeInitiator ocInit;
    	if (ocInitName.length() > 0) {
    		String ocPropName = "OutcomeInit."+ocInitName;
    		synchronized (ocInitCache) {
    			ocInit = ocInitCache.get(ocPropName);
	    		if (ocInit == null) {
	    			Object ocInitObj;
	    			if (!Gateway.getProperties().containsKey(ocPropName)) {
	    				throw new InvalidDataException("Outcome instantiator "+ocPropName+" isn't defined");
	    			}
					try {
						ocInitObj = Gateway.getProperties().getInstance(ocPropName);
					} catch (Exception e) {
						Logger.error(e);
						throw new InvalidDataException("Outcome instantiator "+ocPropName+" couldn't be instantiated");
					}
    				ocInit = (OutcomeInitiator)ocInitObj; // throw runtime class cast if it isn't one
    				ocInitCache.put(ocPropName, ocInit);
	    		}
    		}
    		return ocInit;
    	}
    	else
    		return null;
    }

    public String getOutcomeString() throws InvalidDataException
    {
        if (outcomeData == null && transition.hasOutcome(actProps)) {
        	outcomeData = getLastView();
        	if (outcomeData == null) {
        		OutcomeInitiator ocInit = getOutcomeInitiator();
        		if (ocInit != null)
        			outcomeData = ocInit.initOutcome(this);
        	}
        	if (outcomeData != null) outcomeSet = true;
        }
        return outcomeData;
    }

    public Outcome getOutcome() throws InvalidDataException, ObjectNotFoundException
    {
        return new Outcome(-1, getOutcomeString(), getSchemaName(), getSchemaVersion());
    }
    
    public boolean hasOutcome() {
    	return transition.hasOutcome(actProps);
    }
    
    public boolean hasScript() {
    	return transition.hasScript(actProps);
    }
    
    public boolean isOutcomeSet() {
    	return outcomeSet;
    }   

    @Override
	public String getClusterType()
    {
        return ClusterStorage.JOB;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((itemPath == null) ? 0 : itemPath.hashCode());
		result = prime * result
				+ ((stepPath == null) ? 0 : stepPath.hashCode());
		result = prime * result
				+ ((transition == null) ? 0 : transition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Job other = (Job) obj;
		if (itemPath == null) {
			if (other.itemPath != null)
				return false;
		} else if (!itemPath.equals(other.itemPath))
			return false;
		if (stepPath == null) {
			if (other.stepPath != null)
				return false;
		} else if (!stepPath.equals(other.stepPath))
			return false;
		if (transition == null) {
			if (other.transition != null)
				return false;
		} else if (!transition.equals(other.transition))
			return false;
		return true;
	}

    
    private void setActProps(CastorHashMap actProps) {
    	this.actProps = actProps;
    }

    public Object getActProp(String name)
    {
        return actProps.get(name);
    }

    public String getActPropString(String name)
    {
        Object obj = getActProp(name);
        return obj==null?null:String.valueOf(obj);
    }
}