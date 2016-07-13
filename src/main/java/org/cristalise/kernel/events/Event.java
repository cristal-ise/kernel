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

import java.util.Calendar;

import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.utils.Logger;



/**
 * The data structure of events, which are passed over the event service.
 *
 * Events are incrementaly numbered objects maintained by the History.
 *
 * @version $Revision: 1.13 $ $Date: 2004/11/22 09:12:28 $
 * @author  $Author: abranson $
 */
public class Event implements C2KLocalObject
{
	ItemPath mItemPath; AgentPath mAgentPath, mDelegatePath;
	int mOriginState, mTransition, mTargetState;
	Integer mID, mSchemaVersion, mStateMachineVersion;
    String mName, mStepName, mStepPath, mStepType, mSchemaName, mStateMachineName, mViewName, mAgentRole;
    GTimeStamp mTimeStamp;
    
    public Event(ItemPath itemPath, 
    		AgentPath agentPath, AgentPath delegatePath, String agentRole,
    		String stepName, String stepPath, String stepType,
            StateMachine stateMachine, int transitionId) {
    	
    	Transition transition = stateMachine.getTransition(transitionId);
		Logger.msg(7, "History.addEvent() - creating new event for "+transition.getName()+" on "+stepName+" in "+mItemPath);
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
		setTimeStamp(Event.getGMT());
    }
    
    public Event() { }
    
    public int getOriginState() {
		return mOriginState;
	}

	public void setOriginState(int originState) {
		this.mOriginState = originState;
	}

	public int getTargetState() {
		return mTargetState;
	}

	public void setTargetState(int targetState) {
		this.mTargetState = targetState;
	}

	public Integer getStateMachineVersion() {
		return mStateMachineVersion;
	}

	public void setStateMachineVersion(Integer stateMachineVersion) {
		this.mStateMachineVersion = stateMachineVersion;
	}

	public String getStateMachineName() {
		return mStateMachineName;
	}

	public void setStateMachineName(String stateMachineName) {
		this.mStateMachineName = stateMachineName;
	}

	public void setID( Integer id ) {
        mID = id;
        mName = id==null?null:String.valueOf(id);
    }
    
    /**
     */
    public void setItemPath( ItemPath itemPath )
    {
        mItemPath = itemPath;
    }
    
    public void setItemUUID( String uuid ) throws InvalidItemPathException
    {
    	setItemPath(new ItemPath(uuid));
    }
    
    public String getItemUUID() {
    	return getItemPath().getUUID().toString();
    }

    public void setAgentUUID( String uuid ) throws InvalidItemPathException
    {
    	if (uuid == null || uuid.length() == 0) 
    		mAgentPath = null;
    	else if (uuid.contains(":")) {
    		String[] agentStr = uuid.split(":");
    		if (agentStr.length!=2)
    			throw new InvalidItemPathException();
    		setAgentPath(AgentPath.fromUUIDString(agentStr[0]));
    		setDelegatePath(AgentPath.fromUUIDString(agentStr[1]));
    	}
    	else
			setAgentPath(AgentPath.fromUUIDString(uuid));
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
    
    /**
     * Set the Event Name, in parameter is a String
     */
    @Override
	public void setName(String name)
    {
        mName = name;
        try {
            mID = Integer.parseInt(name);
        } catch (NumberFormatException ex) {
            mID = null;
        }
    }

    /**
     * Set the StepPath of the Event, in parameter is a String
     */
    public void setStepName(String name)
    {
        mStepName = name;
    }

    /**
     * Set the StepPath of the Event, in parameter is a String
     */
    public void setStepPath(String path)
    {
        mStepPath = path;
    }

	/**
	 * Set the StepType of the Event, in parameter is a String
	 */
	public void setStepType(String type)
	{
		mStepType = type;
	}
	
	/**
	 * @param viewName the viewName to set
	 */
	public void setViewName(String viewName) {
		this.mViewName = viewName;
	}

    /**
     * Set the AgentInfo in the Event, in parameter is an AgentInfo
     */
    public void setAgentPath(AgentPath agentPath)
    {
        mAgentPath = agentPath;
    }
    
	public void setDelegatePath(AgentPath delegatorPath) {
		this.mDelegatePath = delegatorPath;
	}

	public void setAgentRole(String agentRole)
	{
		mAgentRole = agentRole;
	}

    /**
     * Set the TimeStamp in the Event, in parameter is an GTimeStamp
     */
    public void setTimeStamp(GTimeStamp inTimeStamp)
    {
        mTimeStamp = inTimeStamp;
    }


    /**
     * Return the Event's ID
     */
    public Integer getID()
    {
        return mID;
    }

    /**
     */
    public ItemPath getItemPath()
    {
        return mItemPath;
    }

    /**
     * Return the Event Name
     */
    @Override
	public String getName()
    {
        return mName;
    }

    /**
     * Return the StepPath of the Event.
     */
    public String getStepName()
    {
        return mStepName;
    }

    /**
     * Return the StepPath of the Event.
     */
    public String getStepPath()
    {
        return mStepPath;
    }

	/**
	 * Return the StepPath of the Event.
	 */
	public String getStepType()
	{
		return mStepType;
	}
	
    /**
	 * @return the mViewName
	 */
	public String getViewName() {
		return mViewName;
	}

    /**
     * Return the AgentInfo of the Event.
     */
    public AgentPath getAgentPath()
    {
        return mAgentPath;
    }

    public AgentPath getDelegatePath() {
		return mDelegatePath;
	}
	
	public String getAgentRole()
    {
    	return mAgentRole;
    }

    /**
     * Return the Event's TimeStamp.
     */
    public GTimeStamp getTimeStamp()
    {
        return mTimeStamp;
    }

    /**
     *  Return the TimeStamp in a form that will
     *  convert nicely to a String
     *  YYYY-MM-DD HH:MI:SS
     */
    public String getTimeString()
    {
        return Event.timeToString(mTimeStamp);
    }

    public static String timeToString(GTimeStamp timeStamp) {
        StringBuffer time = new StringBuffer().append(timeStamp.mYear).append("-");

        if (timeStamp.mMonth<10) time.append("0");
        time.append(timeStamp.mMonth).append("-");

        if (timeStamp.mDay<10) time.append("0");
        time.append(timeStamp.mDay).append(" ");

        if (timeStamp.mHour<10) time.append("0");
        time.append(timeStamp.mHour).append(":");

        if (timeStamp.mMinute<10) time.append("0");
        time.append(timeStamp.mMinute).append(":");

        if (timeStamp.mSecond<10) time.append("0");
        time.append(timeStamp.mSecond);

        return time.toString();
    }

    public void setTimeString(String time) throws InvalidDataException
    {
    	if (time.length() == 19)
    	    mTimeStamp = new GTimeStamp(
        					Integer.parseInt(time.substring(0,4)),
        					Integer.parseInt(time.substring(5,7)),
        					Integer.parseInt(time.substring(8,10)),
        					Integer.parseInt(time.substring(11,13)),
        					Integer.parseInt(time.substring(14,16)),
    	    				Integer.parseInt(time.substring(17,19)),
    		   				Calendar.getInstance().get(Calendar.ZONE_OFFSET));
        else if (time.length() == 14) // support for some sql formats
            mTimeStamp = new GTimeStamp(
                            Integer.parseInt(time.substring(0,4)),
                            Integer.parseInt(time.substring(4,6)),
                            Integer.parseInt(time.substring(6,8)),
                            Integer.parseInt(time.substring(8,10)),
                            Integer.parseInt(time.substring(10,12)),
                            Integer.parseInt(time.substring(12,14)),
                            Calendar.getInstance().get(Calendar.ZONE_OFFSET));
        else
            throw new InvalidDataException("Unknown time format: "+time);
    }



   static public GTimeStamp getGMT()
    {
        java.util.Calendar now = Calendar.getInstance();

        return new GTimeStamp( now.get(Calendar.YEAR),
                               now.get(Calendar.MONTH)+1,
                               now.get(Calendar.DAY_OF_MONTH),
                               now.get(Calendar.HOUR_OF_DAY),
                               now.get(Calendar.MINUTE),
                               now.get(Calendar.SECOND),
                               now.get(Calendar.ZONE_OFFSET) );
    }

	@Override
	public String getClusterType() {
		return ClusterStorage.HISTORY;
	}

    public int getTransition() {
        return mTransition;
    }

    public void setTransition(int i) {
        mTransition = i;
    }

	public Integer getSchemaVersion() {
		return mSchemaVersion;
	}

	public void setSchemaVersion(Integer schemaVersion) {
		this.mSchemaVersion = schemaVersion;
	}

	public String getSchemaName() {
		return mSchemaName;
	}

	public void setSchemaName(String schemaName) {
		this.mSchemaName = schemaName;
	}
    
    public void addOutcomeDetails(Schema schema,
            String viewName) {
    	
		setSchemaName(schema.getItemID());
		setSchemaVersion(schema.getVersion());
	    if (viewName == null || viewName.equals(""))
	    	setViewName("last");
	    else
	    	setViewName(viewName);
    }
}
