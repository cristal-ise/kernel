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
package org.cristalise.kernel.lifecycle.instance;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_ROLE;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.BREAKPOINT;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.DESCRIPTION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VIEW_POINT;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lifecycle.WfCastorHashMap;
import org.cristalise.kernel.lifecycle.instance.predefined.WriteProperty;
import org.cristalise.kernel.lifecycle.instance.stateMachine.State;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

public class Activity extends WfVertex {
    protected static final String XPATH_TOKEN = "xpath:";

    /**
     * vector of errors (Strings) that is constructed each time verify() is
     * launched
     */
    protected Vector<String> mErrors;
    /**
     * @associates a StateMachine engine
     */
    private StateMachine     machine;
    protected int            state  = -1;
    /**
     * true is available to be executed
     */
    public boolean           active = false;
    /**
     * used in verify()
     */
    private boolean          loopTested;
    private GTimeStamp       mStateDate;
    private String           mType;
    private String           mTypeName;

    public Activity() {
        super();
        setProperties(new WfCastorHashMap());

        mErrors = new Vector<String>(0, 1);
        mStateDate = new GTimeStamp();
        DateUtility.setToNow(mStateDate);
    }

    /**
     * add the activity which id is idNext as next of the current one
     */
    Next addNext(String idNext) {
        return addNext((WfVertex) getParent().search(idNext));
    }

    /**
     * adds a New link between the current Activity and the WfVertex passed in
     * param
     */
    @Override
    public Next addNext(WfVertex vertex) {
        return new Next(this, vertex);
    }

    public StateMachine getStateMachine() throws InvalidDataException {
        if (machine == null) {
            try {
                machine = LocalObjectLoader.getStateMachine(getProperties());
            }
            catch (ObjectNotFoundException e) {
                throw new InvalidDataException(e.getMessage());
            }
        }
        return machine;
    }

    /**
     * @return The current State of the StateMachine (Used in Serialisation)
     * @throws InvalidDataException data was inconsistent
     */
    public int getState() throws InvalidDataException {
        if (state == -1) state = getStateMachine().getInitialStateCode();
        return state;
    }

    /**
     * Returns the id of the error Transition associated with the current state
     * @return the id of the error Transition associated with the current state or -1 of there is no error Transition
     */
    public int getErrorTransitionId() {
        try {
            return getStateMachine().getErrorTransitionIdForState(getState());
        }
        catch (InvalidDataException e) {
            return -1;
        }
    }

    public String getStateName() throws InvalidDataException {
        return getStateMachine().getState(getState()).getName();
    }

    /** Sets a new State */
    public void setState(int state) {
        this.state = state;
    }

    public boolean isFinished() throws InvalidDataException {
        return getStateMachine().getState(getState()).isFinished();
    }

    public String request(AgentPath agent, AgentPath delegate, ItemPath itemPath, int transitionID, String requestData, Object locker)
            throws AccessRightsException, InvalidTransitionException, InvalidDataException, ObjectNotFoundException, PersistencyException,
            ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, InvalidCollectionModification
    {
        // Find requested transition
        Transition transition = getStateMachine().getTransition(transitionID);

        // Check if the transition is possible
        String usedRole = transition.getPerformingRole(this, agent);

        // Verify outcome
        boolean storeOutcome = false;
        if (transition.hasOutcome(getProperties())) {
            if (StringUtils.isNotBlank(requestData))
                storeOutcome = true;
            else if (transition.getOutcome().isRequired())
                throw new InvalidDataException("Transition requires outcome data, but none was given");
        }

        // Get new state
        State oldState = getStateMachine().getState(this.state);
        State newState = getStateMachine().traverse(this, transition, agent);

        // Run extra logic in predefined steps here
        String outcome = runActivityLogic(agent, itemPath, transitionID, requestData, locker);

        // set new state and reservation
        setState(newState.getId());
        setBuiltInProperty(AGENT_NAME, transition.getReservation(this, agent));

        try {
            History hist = getWf().getHistory(locker);

            if (storeOutcome) {
                Schema schema = transition.getSchema(getProperties());
                Outcome newOutcome = new Outcome(-1, outcome, schema);
                // TODO: if we were ever going to validate outcomes on storage, it would be here.
                //newOutcome.validateAndCheck();

                String viewpoint = resolveViewpointName(newOutcome);

                int eventID = hist.addEvent(agent, delegate, usedRole, getName(), getPath(), getType(),
                        schema, getStateMachine(), transitionID, viewpoint).getID();
                newOutcome.setID(eventID);

                Gateway.getStorage().put(itemPath, newOutcome, locker);

                // update specific view if defined
                if (!viewpoint.equals("last")) {
                    Gateway.getStorage().put(itemPath, new Viewpoint(itemPath, schema, viewpoint, eventID), locker);
                }

                // update the default "last" view
                Gateway.getStorage().put(itemPath, new Viewpoint(itemPath, schema, "last", eventID), locker);

                updateItemProperties(itemPath, newOutcome, locker);
            }
            else {
                updateItemProperties(itemPath, null, locker);
                hist.addEvent(agent, delegate, usedRole, getName(), getPath(), getType(), getStateMachine(), transitionID);
            }
        }
        catch (PersistencyException ex) {
            Logger.error(ex);
            throw ex;
        }

        if (newState.isFinished() && !(getBuiltInProperty(BREAKPOINT).equals(Boolean.TRUE) && !oldState.isFinished())) {
            runNext(agent, itemPath, locker);
        }

        DateUtility.setToNow(mStateDate);
        pushJobsToAgents(itemPath);

        return outcome;
    }

    private String resolveViewpointName(Outcome outcome) throws InvalidDataException {
        String viewpointString = (String)getBuiltInProperty(VIEW_POINT);

        if (StringUtils.isBlank(viewpointString)) {
            return "last";
        }
        //FIXME: use DataHelper if possible, because it will make code more general
        else if(viewpointString.startsWith(XPATH_TOKEN)) {
            try {
                return outcome.getFieldByXPath(viewpointString.substring(6));
            }
            catch (XPathExpressionException e) {
                throw new InvalidDataException(e.getMessage());
            }
        }

        return viewpointString;
    }

    private void updateItemProperties(ItemPath itemPath, Outcome outcome, Object locker)
            throws InvalidDataException, PersistencyException, ObjectCannotBeUpdated, ObjectNotFoundException
    {
        for(java.util.Map.Entry<String, Object> entry: getProperties().entrySet()) {
            if(entry.getKey().startsWith("ItemProperty.")) {
                String propName = entry.getKey().substring(13);

                if(StringUtils.isNotBlank(propName)) {
                    String propValue = entry.getValue().toString();

                    //FIXME: use DataHelper if possible, because it will make code more general
                    if (outcome != null && StringUtils.isNotBlank(propValue) && propValue.startsWith(XPATH_TOKEN)) {
                        try {
                            propValue = outcome.getFieldByXPath(propValue.substring(6));
                        }
                        catch (XPathExpressionException e) {
                            throw new InvalidDataException(e.getMessage());
                        }
                    }

                    if(StringUtils.isNotBlank(propValue)) {
                        Logger.msg(5, "Activity.updateItemProperties() - propName:"+propName+" propValue:"+propValue);
                        WriteProperty.write(itemPath, propName, propValue, locker);
                    }
                }
                else {
                    throw new InvalidDataException("Incomplete vertex property name:" + entry.getKey());
                }
            }
        }
    }

    /**
     * Overridden in predefined steps
     *
     * @param agent
     * @param itemPath
     * @param transitionID
     * @param requestData
     * @param locker
     * @return
     * @throws InvalidDataException
     * @throws InvalidCollectionModification
     * @throws ObjectAlreadyExistsException
     * @throws ObjectCannotBeUpdated
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     * @throws CannotManageException
     * @throws AccessRightsException 
     */
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, Object locker)
            throws InvalidDataException, InvalidCollectionModification, ObjectAlreadyExistsException, ObjectCannotBeUpdated,
            ObjectNotFoundException, PersistencyException, CannotManageException, AccessRightsException
    {
        return requestData;
    }

    @Override
    public boolean verify() {
        mErrors.removeAllElements();
        int nbInEdgres = getInEdges().length;
        int nbOutEdges = getOutEdges().length;
        if (nbInEdgres == 0 && this.getID() != getParent().getChildrenGraphModel().getStartVertexId()) {
            mErrors.add("Unreachable");
            return false;
        }
        else if (nbInEdgres > 1) {
            mErrors.add("Bad nb of previous");
            return false;
        }
        else if (nbOutEdges > 1) {
            mErrors.add("too many next");
            return false;
        }
        else if (nbOutEdges == 0) {
            if (!((CompositeActivity) getParent()).hasGoodNumberOfActivity()) {
                mErrors.add("too many endpoints");
                return false;
            }
        }
        return true;
    }

    /**
     * Used in verify()
     */
    @Override
    public boolean loop() {
        boolean loop2 = false;
        if (!loopTested) {
            loopTested = true;
            if (getOutGraphables().length != 0) loop2 = ((WfVertex) getOutGraphables()[0]).loop();
        }
        loopTested = false;
        return loop2;
    }

    /**
     * sets the next activity available if possible
     */
    @Override
    public void runNext(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException {
        setActive(false);

        try {
            Vertex[] outVertices = getOutGraphables();
            Vertex[] outVertices2 = getOutGraphables();
            boolean hasNoNext = false;
            boolean out = false;

            while (!out) {
                if (outVertices2.length > 0) {
                    if (outVertices2[0] instanceof Join) outVertices2 = ((WfVertex) outVertices2[0]).getOutGraphables();
                    else                                 out = true;
                }
                else {
                    hasNoNext = true;
                    out = true;
                }
            }

            if (Logger.doLog(8)) Logger.msg("Activity.next() - " + Arrays.toString(outVertices) + " " + Arrays.toString(outVertices2));

            if (hasNoNext) {
                if (getParent() != null && getParent().getName().equals("domain")) {
                    // workflow finished
                    setActive(true);
                }
                else {
                    CompositeActivity parent = (CompositeActivity) getParent();
                    if (parent != null) parent.runNext(agent, itemPath, locker);
                }
            }
            else {
                ((WfVertex) outVertices[0]).run(agent, itemPath, locker);
            }
        }
        catch (InvalidDataException s) {
            setActive(true);
            throw s;
        }
    }

    /**
     *
     * @return the only Next of the Activity
     */
    public Next getNext() {
        if (getOutEdges().length > 0) return (Next) getOutEdges()[0];
        else return null;
    }

    /**
     * reinitialises the Activity and propagate (for Loop)
     */
    @Override
    public void reinit(int idLoop) throws InvalidDataException {
        Vertex[] outVertices = getOutGraphables();
        setState(getStateMachine().getInitialState().getId());
        if (outVertices.length > 0 && idLoop != getID()) {
            WfVertex nextAct = (WfVertex) outVertices[0];
            nextAct.reinit(idLoop);
        }
    }

    /**
     * return the String that identifies the errors found in th activity
     */
    @Override
    public String getErrors() {
        if (mErrors.size() == 0) return "No error";
        return mErrors.elementAt(0);
    }

    /**
     * called by precedent Activity runNext() for setting the activity able to
     * be executed
     */
    @Override
    public void run(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException {
        Logger.msg(8, "Activity.run() path:" + getPath() + " state:" + getState());

        if (!getActive()) setActive(true);
        boolean finished = getStateMachine().getState(getState()).isFinished();
        if (finished) {
            runNext(agent, itemPath, locker);
        }
        else {
            DateUtility.setToNow(mStateDate);
            pushJobsToAgents(itemPath);
        }
    }

    /**
     * sets the activity available to be executed on start of Workflow or
     * composite activity (when it is the first one of the (sub)process
     */
    @Override
    public void runFirst(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException {
        Logger.msg(8, "Activity.runFirst() - path:" + getPath());
        run(agent, itemPath, locker);
    }

    /**
     * @return the current ability to be executed
     */
    public boolean getActive() {
        return active;
    }

    /**
     * sets the ability to be executed
     */
    public void setActive(boolean acti) {
        active = acti;
    }

    /**
     * @return the Description field of properties
     */
    public String getDescription() {
        if (getProperties().containsKey("Description")) return (String) (getBuiltInProperty(DESCRIPTION));
        return "No description";
    }

    public String getCurrentAgentName() {
        return (String) getBuiltInProperty(AGENT_NAME);
    }

    public String getCurrentAgentRole() {
        return (String) getBuiltInProperty(AGENT_ROLE);
    }

    /**
     * Calculates the lists of jobs for the activity and its children (cf org.cristalise.kernel.entity.Job)
     */
    public ArrayList<Job> calculateJobs(AgentPath agent, ItemPath itemPath, boolean recurse)
            throws InvalidAgentPathException, ObjectNotFoundException, InvalidDataException
    {
        return calculateJobsBase(agent, itemPath, false);
    }

    public ArrayList<Job> calculateAllJobs(AgentPath agent, ItemPath itemPath, boolean recurse)
            throws InvalidAgentPathException, ObjectNotFoundException, InvalidDataException {
        return calculateJobsBase(agent, itemPath, true);
    }

    private ArrayList<Job> calculateJobsBase(AgentPath agent, ItemPath itemPath, boolean includeInactive)
            throws ObjectNotFoundException, InvalidDataException, InvalidAgentPathException
    {
        Logger.msg(7, "Activity.calculateJobsBase() - act:" + getPath());
        ArrayList<Job> jobs = new ArrayList<Job>();
        Map<Transition, String> transitions;
        if ((includeInactive || getActive()) && !getName().equals("domain")) {
            transitions = getStateMachine().getPossibleTransitions(this, agent);
            Logger.msg(7, "Activity.calculateJobsBase() - Got " + transitions.size() + " transitions.");
            for (Transition transition : transitions.keySet()) {
                Logger.msg(7, "Activity.calculateJobsBase() - Creating Job object for transition " + transition.getName());
                jobs.add(new Job(this, itemPath, transition, agent, null, transitions.get(transition)));
            }
        }
        return jobs;
    }

    /**
     * Collects all Role names which are associated with this Activity and the Transitions of the current State,
     * and ....
     *
     * @param itemPath
     */
    protected void pushJobsToAgents(ItemPath itemPath) {
        Set<String> roleNames = new TreeSet<String>(); //Shall contain a set of unique role names

        String role = getCurrentAgentRole();

        if (StringUtils.isNotBlank(role)) {
            for (String r: role.split(",")) roleNames.add(r);
        }

        try {
            for (Transition trans : getStateMachine().getState(getState()).getPossibleTransitions().values()) {
                role = trans.getRoleOverride(getProperties());
                if (StringUtils.isNotBlank(role)) roleNames.add(role);
            }

            Logger.msg(7,"Activity.pushJobsToAgents() - Pushing jobs to "+roleNames.size()+" roles");

            for (String roleName: roleNames) {
                pushJobsToAgents(itemPath, Gateway.getLookup().getRolePath(roleName));
            }
        }
        catch (InvalidDataException ex) {
            Logger.warning("Activity.pushJobsToAgents() - "+ex.getMessage());
        }
        catch (ObjectNotFoundException e) {
            Logger.warning("Activity.pushJobsToAgents() - Activity role '" + role + "' not found.");
        }
    }

    /**
     *
     * @param itemPath
     * @param role RolePath
     */
    protected void pushJobsToAgents(ItemPath itemPath, RolePath role) {
        if (role.hasJobList()) new JobPusher(this, itemPath, role).start();

        //Inform child roles as well
        Iterator<Path> childRoles = role.getChildren();

        while (childRoles.hasNext()) {
            pushJobsToAgents(itemPath, (RolePath) childRoles.next());
        }
    }

    /**
     * Returns the startDate.
     *
     * @return GTimeStamp startDate
     */
    public GTimeStamp getStateDate() {
        return mStateDate;
    }

    public void setStateDate(GTimeStamp startDate) {
        mStateDate = startDate;
    }

    @Deprecated
    public void setActiveDate(GTimeStamp date) {
    }

    @Deprecated
    public void setStartDate(GTimeStamp date) {
        setStateDate(date);
    }

    /**
     * Returns the type.
     *
     * @return String
     */
    public String getType() {
        return mType;
    }

    public String getTypeName() {
        if (mType == null) return null;
        if (mTypeName == null) {
            try {
                ItemPath actType = new ItemPath(mType);
                Property nameProp = (Property) Gateway.getStorage().get(actType, ClusterType.PROPERTY + "/" + NAME, null);
                mTypeName = nameProp.getValue();
            }
            catch (Exception e) {
                mTypeName = mType;
            }
        }
        return mTypeName;
    }

    /**
     * Sets the type.
     *
     * @param type
     *            The type to set
     */
    public void setType(String type) {
        mType = type;
        mTypeName = null;
    }

    @Override
    public void abort() {
        active = false;
    }
}
