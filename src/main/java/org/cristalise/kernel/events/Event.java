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

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.kernel.utils.Logger;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;


/**
 * The data structure of events, which are passed over the event service.
 * Events are incrementally numbered objects maintained by the History.
 * <br>
 * Events are generated and stored whenever a step in an Item's lifecycle 
 * changes state, and provide a full record of what was done, when, and by whom.
 */
@Accessors(prefix = "m") @Getter @Setter
public class Event implements C2KLocalObject {
    ItemPath   mItemPath;
    AgentPath  mAgentPath, mDelegatePath;
    int        mOriginState, mTransition, mTargetState;
    Integer    mID, mSchemaVersion, mStateMachineVersion;
    String     mName, mStepName, mStepPath, mStepType, mSchemaName, mStateMachineName, mViewName, mAgentRole;

    /**
     * It is always in UTC
     */
    GTimeStamp mTimeStamp;

//TODO: add these fields to persitency
//String mOriginStateName, mTransitionName, mTargetStateName;

    public Event(ItemPath itemPath, AgentPath agentPath, AgentPath delegatePath, String agentRole,
                 String stepName, String stepPath, String stepType, StateMachine stateMachine, int transitionId)
    {
        Transition transition = stateMachine.getTransition(transitionId);
        Logger.msg(7, "Event() - creating new event for "+transition.getName()+" on "+stepName+" in "+itemPath);

        setItemPath(itemPath);
        setAgentPath(agentPath);
        setDelegatePath(delegatePath);
        setAgentRole(agentRole);
        setStepName(stepName);
        setStepPath(stepPath);
        setStepType(stepType);
        setTransition(transitionId);
        setOriginState(transition.getOriginStateId());
        setTargetState(transition.getTargetStateId());
        setStateMachineName(stateMachine.getItemID());
        setStateMachineVersion(stateMachine.getVersion());
        setTimeStamp(DateUtility.getNow());
    }

    /**
     * Constructor for recreating object from backend
     */
    public Event(Integer id, ItemPath itemPath, AgentPath agentPath, AgentPath delegatePath, String agentRole,
            String stepName, String stepPath, String stepType, 
            String smName, Integer smVersion, int transitionId, int originState, int targetState,
            String schemaName, Integer schemaVersion, String viewName, GTimeStamp ts)
    {
        setID(id);
        setItemPath(itemPath);
        setAgentPath(agentPath);
        setDelegatePath(delegatePath);
        setAgentRole(agentRole);
        setStepName(stepName);
        setStepPath(stepPath);
        setStepType(stepType);
        setStateMachineName(smName);
        setStateMachineVersion(smVersion);
        setTransition(transitionId);
        setOriginState(originState);
        setTargetState(targetState);
        setSchemaName(schemaName);
        setSchemaVersion(schemaVersion);
        setViewName(viewName);
        setTimeStamp(ts);
    }

    public Event() { }

    public void setID( Integer id ) {
        mID = id;
        mName = id==null?null:String.valueOf(id);
    }

    public void setItemUUID( String uuid ) throws InvalidItemPathException {
        setItemPath(new ItemPath(uuid));
    }

    public String getItemUUID() {
        return getItemPath().getUUID().toString();
    }

    public void setAgentUUID( String uuid ) throws InvalidItemPathException {
        if (uuid == null || uuid.length() == 0) {
            mAgentPath = null;
        }
        else if (uuid.contains(":")) {
            String[] agentStr = uuid.split(":");
           
            if (agentStr.length!=2) throw new InvalidItemPathException();

            setAgentPath(new AgentPath(agentStr[0]));
            setDelegatePath(new AgentPath(agentStr[1]));
        }
        else
            setAgentPath(new AgentPath(uuid));
    }

    public String getAgentUUID() {
        if (mAgentPath != null) {
            if (mDelegatePath != null)
                return getAgentPath().getUUID().toString()+":"+getDelegatePath().getUUID().toString();
            else
                return getAgentPath().getUUID().toString();
        }
        else
            return null;
    }

    @Override
    public void setName(String name) {
        mName = name;

        if(mID == null) {
            try {
                mID = Integer.parseInt(name);
            }
            catch (NumberFormatException ex) {}
        }
    }

    /**
     * Return the TimeStamp in a form that will convert nicely to a String: YYYY-MM-DD HH:MI:SS
     * @return Return the formatted TimeStamp 
     */
    public String getTimeString() {
        return DateUtility.timeToString(mTimeStamp);
    }

    public void setTimeString(String time) throws InvalidDataException {
        mTimeStamp = DateUtility.parseTimeString(time);
    }

    @Override
    public ClusterType getClusterType() {
        return ClusterType.HISTORY;
    }

    @Override
    public String getClusterPath() {
        return getClusterType()+"/"+mID;
    }

    public void addOutcomeDetails(Schema schema, String viewName) {
        setSchemaName(schema.getItemID());
        setSchemaVersion(schema.getVersion());

        setViewName(viewName);
    }

    /**
     * Set the ViewName 
     * @param viewName name of the view or 'last if viewName is blank
     */
    public void setViewName(String viewName) {
        if (StringUtils.isBlank(viewName)) mViewName = "last";
        else                               mViewName = viewName;
    }
}
