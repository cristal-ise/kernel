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
package org.cristalise.kernel.events;


import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.RemoteMap;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.utils.Logger;


public class History extends RemoteMap<Event> {

    private static final long serialVersionUID = 3273324106002587993L;

    int lastID = -1;

    public History(ItemPath itemPath, Object locker) {
        super(itemPath, ClusterStorage.HISTORY, locker);
    }
    
    public Event getEvent(int id) {
    	return get(String.valueOf(id));
    }

	@Override
	public Event remove(Object key) {
		throw new UnsupportedOperationException();
	}
    
    private Event storeNewEvent(Event newEvent) {
        synchronized (this) {
            int newEventID = getLastId()+1;
            newEvent.setID(newEventID);
            put(newEvent.getName(), newEvent);
            lastID = newEventID;
            return newEvent;
        }
    }
	
    public Event addEvent(AgentPath agentPath, AgentPath delegatePath, String agentRole,
            String stepName,
            String stepPath,
            String stepType,
            StateMachine stateMachine,
            int transitionId) {
    	return storeNewEvent(new Event(mItemPath, 
    			agentPath, delegatePath, agentRole, 
    			stepName, stepPath, stepType, 
    			stateMachine, transitionId));
    }

    
	public Event addEvent(AgentPath agentPath, 
			AgentPath delegatePath, String agentRole,
			String stepName, 
			String stepPath, 
			String stepType,
			Schema schema, 
			StateMachine stateMachine, 
			int transitionId,
			String viewName) {
    	Event newEvent = new Event(mItemPath, 
    			agentPath, delegatePath, agentRole, 
    			stepName, stepPath,	stepType, 
    			stateMachine, transitionId);
    	newEvent.addOutcomeDetails(schema, viewName);
    	return storeNewEvent(newEvent);
	}

    
    public Event addEvent(AgentPath agentPath, AgentPath delegatePath,	String agentRole,
            String stepName, String stepPath, String stepType,
            StateMachine stateMachine,
            int transitionId,
            String timeString) throws InvalidDataException {
    	Event newEvent = new Event(mItemPath, 
    			agentPath, delegatePath, agentRole, 
    			stepName, stepPath,	stepType, 
    			stateMachine, transitionId);
    	newEvent.setTimeString(timeString);
    	return storeNewEvent(newEvent);
    }

    public Event addEvent(AgentPath agentPath, AgentPath delegatePath, String agentRole,
            String stepName, String stepPath, String stepType,
            Schema schema, StateMachine stateMachine,
            int transitionId,
            String viewName,
            String timeString) throws InvalidDataException {
    	
    	Event newEvent = new Event(mItemPath, 
    			agentPath, delegatePath, agentRole, 
    			stepName, stepPath, stepType, 
    			stateMachine, transitionId);
    	newEvent.addOutcomeDetails(schema, viewName);
    	newEvent.setTimeString(timeString);
    	return storeNewEvent(newEvent);
    }
    

	
	/*
	 * Deprecated event management methods
	 */
	
    @Deprecated
    public Event addEvent(AgentPath agentPath, String agentRole,
            String stepName,
            String stepPath,
            String stepType,
            String stateMachineName,
            Integer stateMachineVersion,
            Transition transition) {
    	return addEvent(agentPath, agentRole, stepName, stepPath, stepType, null, null, stateMachineName, stateMachineVersion, transition, null);
    }
    
    @Deprecated
    public Event addEvent(AgentPath agentPath, String agentRole,
                    String stepName,
                    String stepPath,
                    String stepType,
                    String schemaName,
                    Integer schemaVersion,
                    String stateMachineName,
                    Integer stateMachineVersion,
                    Transition transition,
                    String viewName) {
        Logger.msg(7, "History.addEvent() - creating new event for "+transition.getName()+" on "+stepName+" in "+mItemPath);
        Event newEvent = new Event();
        newEvent.setItemPath(mItemPath);
        newEvent.setAgentPath(agentPath);
        newEvent.setAgentRole(agentRole);
        newEvent.setStepName(stepName);
        newEvent.setStepPath(stepPath);
        newEvent.setStepType(stepType);
        if (schemaName != null && !schemaName.equals("")) {
        	newEvent.setSchemaName(schemaName);
        	newEvent.setSchemaVersion(schemaVersion);
            if (viewName == null || viewName.equals(""))
            	newEvent.setViewName("last");
            else
            	newEvent.setViewName(viewName);
        }
        newEvent.setOriginState(transition.getOriginStateId());
        newEvent.setTargetState(transition.getTargetStateId());
        newEvent.setTransition(transition.getId());
        newEvent.setStateMachineName(stateMachineName);
        newEvent.setStateMachineVersion(stateMachineVersion);
        newEvent.setTimeStamp(Event.getGMT());
        return storeNewEvent(newEvent);
    }

    @Deprecated
    public Event addEvent(AgentPath agentPath, String agentRole,
            String stepName,
            String stepPath,
            String stepType,
            String stateMachineName,
            Integer stateMachineVersion,
            Transition transition,
            String timeString) throws InvalidDataException {
    	return addEvent(agentPath, agentRole, stepName, stepPath, stepType, null, null, stateMachineName, stateMachineVersion, transition, null, timeString);
    }
    
    @Deprecated
    public Event addEvent(AgentPath agentPath, String agentRole,
                    String stepName,
                    String stepPath,
                    String stepType,
                    String schemaName,
                    Integer schemaVersion,
                    String stateMachineName,
                    Integer stateMachineVersion,
                    Transition transition,
                    String viewName,
                    String timeString) throws InvalidDataException {
        Logger.msg(7, "History.addEvent() - creating new event for "+transition.getName()+" on "+stepName+" in "+mItemPath);
        Event newEvent = new Event();
        newEvent.setItemPath(mItemPath);
        newEvent.setAgentPath(agentPath);
        newEvent.setAgentRole(agentRole);
        newEvent.setStepName(stepName);
        newEvent.setStepPath(stepPath);
        newEvent.setStepType(stepType);
        if (schemaName != null && !schemaName.equals("")) {
        	newEvent.setSchemaName(schemaName);
        	newEvent.setSchemaVersion(schemaVersion);
            if (viewName == null || viewName.equals(""))
            	newEvent.setViewName("last");
            else
            	newEvent.setViewName(viewName);
        }
        newEvent.setOriginState(transition.getOriginStateId());
        newEvent.setTargetState(transition.getTargetStateId());
        newEvent.setTransition(transition.getId());
        newEvent.setStateMachineName(stateMachineName);
        newEvent.setStateMachineVersion(stateMachineVersion);
        newEvent.setTimeString(timeString);
        return storeNewEvent(newEvent);
    }
}
