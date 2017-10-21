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
package org.cristalise.kernel.entity.agent;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.DELEGATE_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.OUTCOME_INIT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.OutcomeInitiator;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 */
@Getter @Setter
public class Job implements C2KLocalObject {
    // Persistent fields
    private int            id;
    private ItemPath       itemPath;
    private String         stepName;
    private String         stepPath;
    private String         stepType;
    private Transition     transition;
    private String         originStateName;
    private String         targetStateName;
    private String         agentRole;
    private AgentPath      agentPath;
    private AgentPath      delegatePath;
    private CastorHashMap  actProps = new CastorHashMap();
    private GTimeStamp     creationDate;

    // Non-persistent fields
    private ErrorInfo  error;
    private ItemProxy  item = null;
    private boolean    transitionResolved = false;

    private Outcome outcome = null;

    /**
     * OutcomeInitiator cache
     */
    static private HashMap<String, OutcomeInitiator> ocInitCache = new HashMap<String, OutcomeInitiator>();

    /**
     * Empty constructor required for Castor
     */
    public Job() {
        setCreationDate(DateUtility.getNow());
        setActProps(new CastorHashMap());
    }

    /**
     * Main constructor to create Job during workflow enactment
     */
    public Job(Activity act, ItemPath itemPath, Transition transition, AgentPath agent, AgentPath delegate, String role)
            throws InvalidDataException, ObjectNotFoundException, InvalidAgentPathException
    {
        setCreationDate(DateUtility.getNow());
        setItemPath(itemPath);
        setStepPath(act.getPath());
        setTransition(transition);
        setOriginStateName(act.getStateMachine().getState(transition.getOriginStateId()).getName());
        setTargetStateName(act.getStateMachine().getState(transition.getTargetStateId()).getName());
        setStepName(act.getName());
        setStepType(act.getType());
        setAgentPath(agent);
        setAgentRole(role);

        setActPropsAndEvaluateValues(act);

        getItem();
    }

    /**
     * Constructor for recreating Job from backend
     */
    public Job(int id, ItemPath itemPath, String stepName, String stepPath, String stepType, 
            Transition transition, String originStateName, String targetStateName, 
            String agentRole, AgentPath agentPath, AgentPath delegatePath, CastorHashMap actProps, GTimeStamp creationDate)
    {
        super();
        setId(id);
        setItemPath(itemPath);
        setStepName(stepName);
        setStepPath(stepPath);
        setStepType(stepType);
        setTransition(transition);
        setOriginStateName(originStateName);
        setTargetStateName(targetStateName);
        setAgentRole(agentRole);
        setAgentPath(agentPath);
        setDelegatePath(delegatePath);
        setActProps(actProps);
        setCreationDate(creationDate);
    }

    public void setItemPath(ItemPath path) {
        itemPath = path;
        item = null;
    }

    public void setItemUUID( String uuid ) throws InvalidItemPathException {
        setItemPath(new ItemPath(uuid));
    }

    public String getItemUUID() {
        return getItemPath().getUUID().toString();
    }

    public ItemProxy getItem() throws InvalidDataException {
        try {
            return getItemProxy();
        }
        catch (InvalidItemPathException | ObjectNotFoundException e) {
            throw new InvalidDataException(e.getMessage());
        }
    }

    public Transition getTransition() {
        if (transition != null && transitionResolved == false) {
            Logger.msg(8, "Job.getTransition() - actProps:"+actProps);
            try {
                StateMachine sm = LocalObjectLoader.getStateMachine(actProps);
                transition = sm.getTransition(transition.getId());
                transitionResolved = true;
            }
            catch (Exception e) {
                Logger.error(e);
                return transition;
            }
        }
        return transition;
    }

    public void setTransition(Transition transition) {
        this.transition = transition;
        transitionResolved = false;
    }

    /**
     * Used by castor to unmarshall from XML
     * 
     * @param uuid the string representation of UUID
     * @throws InvalidItemPathException Cannot set UUID of agent and delegate from parameter
     */
    public void setAgentUUID( String uuid ) throws InvalidItemPathException {
        if (StringUtils.isBlank(uuid)) { 
            agentPath = null; 
            delegatePath = null;
        }
        else if (uuid.contains(":")) {
            String[] agentStr = uuid.split(":");

            if (agentStr.length!=2) throw new InvalidItemPathException("Cannot set UUID of agent and delegate from string:"+uuid);

            setAgentPath(    new AgentPath(agentStr[0]) );
            setDelegatePath( new AgentPath(agentStr[1]) );
        }
        else
            setAgentPath(new AgentPath(uuid));
    }

    /**
    * Used by castor to marshall to XML
     * @return The stringified UUID of Agent concatenated with ':' and UUID of Delegate if exists
     */
    public String getAgentUUID() {
        if (agentPath != null) {
            if (delegatePath != null) return getAgentPath().getUUID().toString()+":"+getDelegatePath().getUUID().toString();
            else                      return getAgentPath().getUUID().toString();
        }
        return null;
    }

    public String getAgentName() {
        String agentName = null;

        if (agentPath != null) agentName = agentPath.getAgentName();
        if (agentName == null) agentName = (String) actProps.getBuiltInProperty(AGENT_NAME);

        return agentName;
    }

    public String getDelegateName() {
        String delegateName = null;

        if (delegatePath != null) delegateName = delegatePath.getAgentName();
        if (delegateName == null) delegateName = (String) actProps.getBuiltInProperty(DELEGATE_NAME);

        return delegateName;
    }

    public Schema getSchema() throws InvalidDataException, ObjectNotFoundException {
        if (getTransition().hasOutcome(actProps)) {
            return getTransition().getSchema(actProps);
        }
        return null;
    }

    @Deprecated
    public String getSchemaName() throws InvalidDataException, ObjectNotFoundException {
        try {
            return getSchema().getName();
        }
        catch (Exception e) {
            return null;
        }
    }

    @Deprecated
    public int getSchemaVersion() throws InvalidDataException, ObjectNotFoundException {
        try {
            return getSchema().getVersion();
        }
        catch (Exception e) {
            return -1;
        }
    }
    public boolean isOutcomeRequired() {
        return getTransition().hasOutcome(actProps) && getTransition().getOutcome().isRequired();
    }

    public Script getScript() throws ObjectNotFoundException, InvalidDataException {
        if (getTransition().hasScript(actProps)) {
            return getTransition().getScript(actProps);
        }
        return null;
    }

    public Query getQuery() throws ObjectNotFoundException, InvalidDataException {
        if (hasQuery()) {
            Query query = getTransition().getQuery(actProps);
            query.setParemeterValues(itemPath.getUUID().toString(), getSchemaName(), actProps);
            return query;
        }
        return null;
    }

    @Deprecated
    public String getScriptName() {
        try {
            return getScript().getName();
        }
        catch (Exception e) {
            return null;
        }
    }

    @Deprecated
    public int getScriptVersion() throws InvalidDataException {
        try {
            return getScript().getVersion();
        }
        catch (Exception e) {
            return -1;
        }
    }

    public KeyValuePair[] getKeyValuePairs() {
        return actProps.getKeyValuePairs();
    }

    public void setKeyValuePairs(KeyValuePair[] pairs) {
        actProps.setKeyValuePairs(pairs);
    }

    @Override
    public String getName() {
        return Integer.toString(id);
    }

    @Override
    public void setName(String name) {
        id = Integer.parseInt(name);
    }

    public ItemProxy getItemProxy() throws ObjectNotFoundException, InvalidItemPathException {
        if (item == null) item = Gateway.getProxyManager().getProxy(itemPath);
        return item;
    }

    public String getDescription() {
        String desc = (String) actProps.get("Description");
        if (desc == null) desc = "No Description";
        return desc;
    }

    public void setOutcome(String outcomeData) throws InvalidDataException, ObjectNotFoundException {
        setOutcome(new Outcome(-1, outcomeData, transition.getSchema(actProps)));
    }

    public void setOutcome(Outcome o) {
        outcome = o;
    }

    public void setError(ErrorInfo errors) {
        error = errors;
        try {
            setOutcome(Gateway.getMarshaller().marshall(error));
        }
        catch (Exception e) {
            Logger.error("Error marshalling ErrorInfo in job");
            Logger.error(e);
        } 
    }

    /**
     * Checks the value of the 'Viewpoint' ActivityProperty and return 'last' if value is blank 
     * or starts with 'xpath:'. In all other cases it returns its value.
     * 
     * @return the 'calculated' Viewpoint name
     */
    public String getValidViewpointName() {
        String viewName = getActPropString("Viewpoint");

        Logger.msg(5, "Job.getValidViewpointName() - Activity properties Viewpoint:'"+viewName+"'");

        if(StringUtils.isBlank(viewName) || viewName.startsWith("xpath:")) {
            viewName = "last";
        }

        Logger.msg(5, "Job.getValidViewpointName() - returning Viewpoint:'"+viewName+"'");

        return viewName;
    }

    /**
     * Returns the Outcome instance associated with the 'last' Viewpoint
     * 
     * @return Outcome instance
     * @throws InvalidDataException inconsistent data or persistency issue
     * @throws ObjectNotFoundException Schema or Outcome was not found
     */
    public Outcome getLastOutcome() throws InvalidDataException, ObjectNotFoundException {
        try {
            return item.getViewpoint(getSchema().getName(), getValidViewpointName()).getOutcome();
        }
        catch (PersistencyException e) {
            Logger.error(e);
            throw new InvalidDataException("Error loading viewpoint:"+e.getMessage()); 
        }
    }

    /**
     * Returns the Outcome string associated with the 'last' Viewpoint
     * 
     * @return XML data of the last version of Outcome
     * @throws InvalidDataException inconsistent data or persistency issue
     * @throws ObjectNotFoundException Schema or Outcome was not found
     */
    public String getLastView() throws InvalidDataException, ObjectNotFoundException {
        return getLastOutcome().getData();
    }

    /**
     * Retrieve the OutcomeInitiator associated with this Job.
     * 
     * @see BuiltInVertexProperties#OUTCOME_INIT
     * 
     * @return the configured OutcomeInitiator
     * @throws InvalidDataException OutcomeInitiator could not be created
     */
    public OutcomeInitiator getOutcomeInitiator() throws InvalidDataException {
        String ocInitName = getActPropString(OUTCOME_INIT);

        if (StringUtils.isNotBlank(ocInitName)) {
            String ocConfigPropName = OUTCOME_INIT.getName()+"."+ocInitName;
            OutcomeInitiator ocInit;

            synchronized (ocInitCache) {
                Logger.msg(5, "Job.getOutcomeInitiator() - ocConfigPropName:"+ocConfigPropName);
                ocInit = ocInitCache.get(ocConfigPropName);

                if (ocInit == null) {
                    if (!Gateway.getProperties().containsKey(ocConfigPropName)) {
                        throw new InvalidDataException("Property OutcomeInstantiator "+ocConfigPropName+" isn't defined. Check module.xml");
                    }

                    try {
                        ocInit = (OutcomeInitiator)Gateway.getProperties().getInstance(ocConfigPropName);
                        ocInitCache.put(ocConfigPropName, ocInit);
                    }
                    catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                        Logger.error(e);
                        throw new InvalidDataException("OutcomeInstantiator "+ocConfigPropName+" couldn't be instantiated:"+e.getMessage());
                    }
                }
            }
            return ocInit;
        }
        else
            return null;
    }

    /**
     * Returns the Outcome string. It is based on {@link Job#getOutcome()}
     * 
     * @return the Outcome xml or null
     * @throws InvalidDataException inconsistent data
     */
    public String getOutcomeString() throws InvalidDataException, ObjectNotFoundException {
        if(outcome != null) { 
            return outcome.getData();
        }
        else {
            getOutcome();

            //getOutcome() could return a null object
            if(outcome != null) return outcome.getData();
        }
        return null;
    }

    /**
     * Returns the Outcome if exists otherwise tries to read and duplicate the Outcome of 'last' ViewPoint. 
     * If that does not exists it tries to use an OutcomeInitiator.
     * 
     * @return the Outcome object or null
     * @throws InvalidDataException inconsistent data
     * @throws ObjectNotFoundException Schema was not found
     */
    public Outcome getOutcome() throws InvalidDataException, ObjectNotFoundException {
        if (outcome == null && transition.hasOutcome(actProps)) {
            if( getItem().checkViewpoint(getSchema().getName(), getValidViewpointName()) ) {
                Outcome tempOutcome = getLastOutcome();
                outcome = new Outcome(tempOutcome.getData(), tempOutcome.getSchema());
            }
            else {
                OutcomeInitiator ocInit = getOutcomeInitiator();
                if (ocInit != null) outcome = ocInit.initOutcomeInstance(this);
            }
        }
        else {
            Logger.msg(8, "Job.getOutcome() - Job does not require Outcome job:"+this);
        }
        return outcome;
    }

    public boolean hasOutcome() {
        return transition.hasOutcome(actProps);
    }

    public boolean hasScript() {
        return transition.hasScript(actProps);
    }

    public boolean hasQuery() {
        return transition.hasQuery(actProps);
    }

    public boolean isOutcomeSet() {
        return outcome != null;
    }

    @Override
    public ClusterType getClusterType() {
        return ClusterType.JOB;
    }

    @Override
    public String getClusterPath() {
        return getClusterType()+"/"+id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((itemPath == null)   ? 0 : itemPath.hashCode());
        result = prime * result + ((stepPath == null)   ? 0 : stepPath.hashCode());
        result = prime * result + ((transition == null) ? 0 : transition.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)                  return true;
        if (other == null)                  return false;
        if (getClass() != other.getClass()) return false;

        Job otherJob = (Job) other;

        if (itemPath == null) if (otherJob.itemPath != null) return false;
        else if (!itemPath.equals(otherJob.itemPath))        return false;

        if (stepPath == null) if (otherJob.stepPath != null) return false;
        else if (!stepPath.equals(otherJob.stepPath))        return false;

        if (transition == null) if (otherJob.transition != null) return false;
        else if (!transition.equals(otherJob.transition))        return false;

        return true;
    }

    /**
     * 
     * @param act
     * @throws InvalidDataException 
     */
    private void setActPropsAndEvaluateValues(Activity act) throws InvalidDataException {
        setActProps(act.getProperties());
        
        List<String> errors = new ArrayList<String>();

        for(Map.Entry<String, Object> entry: act.getProperties().entrySet()) {
            try {
                Object newVal = act.evaluatePropertyValue(null, entry.getValue(), null);
                if(newVal != null) actProps.put(entry.getKey(), newVal);
            }
            catch (InvalidDataException | PersistencyException | ObjectNotFoundException e) {
                Logger.error(e);
                errors.add(e.getMessage());
            }
        }

        if(errors.size() != 0) {
            StringBuffer buffer = new StringBuffer();
            for(String msg: errors) buffer.append(msg);
            throw new InvalidDataException(buffer.toString());
        }
    }

    private void setActProps(CastorHashMap actProps) {
        this.actProps = actProps;
    }

    public Object getActProp(String name) {
        return actProps.get(name);
    }

    public Object getActProp(String name, Object defaultValue) {
        Object value = getActProp(name);
        return (value == null) ? defaultValue : value;
    }

    public Object getActProp(BuiltInVertexProperties name) {
        return getActProp(name.getName());
    }

    public Object getActProp(BuiltInVertexProperties name, Object defaultValue) {
        return getActProp(name.getName(), defaultValue);
    }

    public String getActPropString(String name) {
        Object obj = getActProp(name);
        return obj == null ? null : String.valueOf(obj);
    }

    public void setActProp(BuiltInVertexProperties prop, Object value) {
        actProps.setBuiltInProperty(prop, value);
    }

    public void setActProp(String name, Object value) {
        actProps.put(name, value);
    }

    public String getActPropString(BuiltInVertexProperties name) {
        return getActPropString(name.getName());
    }

    /**
     * Searches Activity property names using {@link String#startsWith(String)} method
     * 
     * @param pattern the pattern to be matched
     * @return Map of property name and value
     */
    public Map<String, Object> matchActPropNames(String pattern) {
        Map<String, Object> result = new HashMap<String, Object>();

        for(String propName : actProps.keySet()) {
            if(propName.startsWith(pattern)) result.put(propName, actProps.get(propName));
            //if(propName.matches(pattern)) result.put(propName, actProps.get(propName));
        }

        if(result.size() == 0) {
            Logger.msg(5, "Job.matchActPropNames() - NO properties were found for propName.startsWith(pattern:'"+pattern+"')");
            actProps.dump(8);
        }

        return result;
    }

    @Override
    public String toString() {
        return "[item:"+itemPath+" step:"+stepName+" trans:"+getTransition()+" role:"+agentRole+"]";
    }
}