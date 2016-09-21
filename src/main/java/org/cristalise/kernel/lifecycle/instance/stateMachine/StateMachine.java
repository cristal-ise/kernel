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
package org.cristalise.kernel.lifecycle.instance.stateMachine;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;

public class StateMachine implements DescriptionObject {
    public String   name;
    public Integer  version;
    public ItemPath itemPath;

    private ArrayList<State>                   states;
    private ArrayList<Transition>              transitions;
    private final HashMap<Integer, State>      stateCodes;
    private final HashMap<Integer, Transition> transitionCodes;

    State   initialState;
    int     initialStateCode;
    boolean isCoherent = false;

    public StateMachine() {
        states = new ArrayList<State>();
        transitions = new ArrayList<Transition>();
        stateCodes = new HashMap<Integer, State>();
        transitionCodes = new HashMap<Integer, Transition>();
    }

    public StateMachine(String name, Integer version) {
        this();
        this.name = name;
        this.version = version;
    }

    /**
     * Stores the next State id. -1 means that the value was not initialized yet
     * (e.g. after unmarshall from xml)
     */
    private int nextStateId = -1;

    /**
     * Stores the next Transition id. -1 means that the value was not
     * initialized yet (e.g. after unmarshall from xml)
     */
    private int nextTransId = -1;

    /**
     * Computes the next State id. When loaded from XML, the next id calculated
     * from the existing States
     * 
     * @return the next state id
     */
    private int getNextStateId() {
        if (nextStateId == -1) {
            for (State s : states) {
                if (s.id > nextStateId) nextStateId = s.id;
            }
            nextStateId++;
        }
        return nextStateId++;
    }

    /**
     * Computes the next Transition id. When loaded from XML, the next id
     * calculated from the existing Transitions
     * 
     * @return the next state id
     */
    private int getNextTransId() {
        if (nextTransId == -1) {
            for (Transition t : transitions) {
                if (t.id > nextTransId) nextTransId = t.id;
            }
            nextTransId++;
        }
        return nextTransId++;
    }

    /**
     * Factory method to create a new State for the given name. It does NOT
     * check whether the name exists or not
     * 
     * @param name
     *            the name of the State
     * @return the new State
     */
    public State createState(String name) {
        State newState = new State(getNextStateId(), name);
        states.add(newState);
        Logger.msg(5, "StateMachine.createState() - created:" + name + " id:" + newState.id);
        return newState;
    }

    /**
     * Factory method to create a new Transition for the given name. It does NOT
     * check whether the name exists or not
     * 
     * @param name
     *            the name of the Transition
     * @return the new Transition
     */
    public Transition createTransition(String name) {
        Transition newTrans = new Transition(getNextTransId(), name);
        transitions.add(newTrans);
        Logger.msg(5, "StateMachine.createTransition() - created:" + name + " id:" + newTrans.id);
        return newTrans;
    }

    public void setStates(ArrayList<State> newStates) {
        this.states = newStates;
        validate();
    }

    public void setTransitions(ArrayList<Transition> newTransitions) {
        this.transitions = newTransitions;
        validate();
    }

    public boolean validate() {
        stateCodes.clear();
        transitionCodes.clear();
        isCoherent = true;

        Logger.msg(5, "StateMachine.validate() - name:'" + name + "'");

        for (State state : states) {
            Logger.debug(8, "State " + state.id + ": " + state.name);
            stateCodes.put(state.getId(), state);
        }

        if (stateCodes.containsKey(initialStateCode)) initialState = stateCodes.get(initialStateCode);
        else                                          isCoherent = false;

        for (Transition trans : transitions) {
            Logger.debug(8, "Transition " + trans.id + ": " + trans.name);
            transitionCodes.put(trans.getId(), trans);
            isCoherent = isCoherent && trans.resolveStates(stateCodes);
        }
        return isCoherent;
    }

    public ArrayList<State> getStates() {
        return states;
    }

    public ArrayList<Transition> getTransitions() {
        return transitions;
    }

    public State getInitialState() {
        return initialState;
    }

    public void setInitialState(State initialState) {
        this.initialState = initialState;
        initialStateCode = initialState.getId();
    }

    public int getInitialStateCode() {
        return initialStateCode;
    }

    public void setInitialStateCode(int initialStateCode) {
        this.initialStateCode = initialStateCode;
        initialState = stateCodes.get(initialStateCode);
        if (initialState == null) isCoherent = false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public ItemPath getItemPath() {
        return itemPath;
    }

    @Override
    public void setItemPath(ItemPath path) {
        itemPath = path;
    }

    @Override
    public String getItemID() {
        return itemPath.getUUID().toString();
    }

    public Transition getTransition(int transitionID) {
        return transitionCodes.get(transitionID);
    }

    public State getState(int stateID) {
        return stateCodes.get(stateID);
    }

    @Override
    public CollectionArrayList makeDescCollections() {
        return new CollectionArrayList();
    }

    public Map<Transition, String> getPossibleTransitions(Activity act, AgentPath agent) throws ObjectNotFoundException, InvalidDataException {
        HashMap<Transition, String> returnList = new HashMap<Transition, String>();
        State currentState = getState(act.getState());
        for (Integer transCode : currentState.getPossibleTransitionIds()) {
            Transition possTrans = currentState.getPossibleTransitions().get(transCode);

            try {
                String role = possTrans.getPerformingRole(act, agent);
                returnList.put(possTrans, role);
            }
            catch (AccessRightsException ex) {
                if (Logger.doLog(6))
                    Logger.msg(5, "Transition '" + possTrans + "' not possible for " + agent.getAgentName() + ": " + ex.getMessage());
            }
        }
        return returnList;
    }

    public State traverse(Activity act, Transition transition, AgentPath agent)
            throws InvalidTransitionException, AccessRightsException, ObjectNotFoundException, InvalidDataException {
        State currentState = getState(act.getState());
        if (transition.originState.equals(currentState)) {
            transition.getPerformingRole(act, agent);
            return transition.targetState;
        }
        else throw new InvalidTransitionException(
                "Transition '" + transition.getName() + "' not valid from state '" + currentState.getName() + "'");

    }

    public boolean isCoherent() {
        return isCoherent;
    }

    @Override
    public void export(Writer imports, File dir) throws IOException, InvalidDataException {
        String smXML;
        try {
            smXML = Gateway.getMarshaller().marshall(this);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new InvalidDataException("Couldn't marshall state machine " + getName());
        }
        FileStringUtility.string2File(new File(new File(dir, "SM"), getName() + (getVersion() == null ? "" : "_" + getVersion()) + ".xml"),
                smXML);
        if (imports != null)
            imports.write("<Resource name=\"" + getName() + "\" " + (getItemPath() == null ? "" : "id=\"" + getItemID() + "\" ")
                    + (getVersion() == null ? "" : "version=\"" + getVersion() + "\" ") + "type=\"SM\">boot/SM/" + getName()
                    + (getVersion() == null ? "" : "_" + getVersion()) + ".xml</Resource>\n");
    }

    /**
     * Helper method to get transition ID by name
     * 
     * @param name
     * @return the integer ID associated with the Transition name. Returns -1 in
     *         case the name does not exist
     */
    public int getTransitionID(String name) {
        for (Transition t : transitions) {
            if (t.getName().equals(name)) return t.getId();
        }
        return -1;
    }
}