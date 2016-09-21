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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

public class Transition {

    int    id;
    String name;

    int    originStateId = -1;
    int    targetStateId = -1;
    State  originState;
    State  targetState;
    String reservation;

    /**
     * Boolean property that permits this transition e.g.'Skippable'
     */
    String enabledProp;
    /**
     * Whether the activity must be active for this transition to be available (activation property)
     */
    boolean requiresActive = true;
    /**
     * Whether the target state is a finishing state (activation property)
     */
    boolean finishing;
    boolean reinitializes  = false;
    /**
     * Overrides the permision specified in the Activity
     */
    String roleOverride;

    TransitionOutcome outcome;
    TransitionScript  script;

    public Transition() {}

    public Transition(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Transition(int id, String name, int originStateId, int targetStateId) {
        this(id, name);
        this.originStateId = originStateId;
        this.targetStateId = targetStateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public State getOriginState() {
        return originState;
    }

    public void setOriginState(State originState) {
        this.originState = originState;
    }

    public State getTargetState() {
        return targetState;
    }

    public void setTargetState(State targetState) {
        this.targetState = targetState;
        finishing = targetState.finished;
    }

    public String getEnabledProp() {
        return enabledProp;
    }

    public void setEnabledProp(String enabledProp) {
        this.enabledProp = enabledProp;
    }

    public boolean isRequiresActive() {
        return requiresActive;
    }

    public boolean isFinishing() {
        return finishing;
    }

    public boolean reinitializes() {
        return reinitializes;
    }

    public void setReinitializes(boolean reinit) {
        if (finishing) throw new RuntimeException("Transition cannot be both reinitializing and finishing");
        reinitializes = reinit;
    }

    public void setRequiresActive(boolean requiresActive) {
        this.requiresActive = requiresActive;
    }

    public String getRoleOverride() {
        return roleOverride;
    }

    public String getRoleOverride(CastorHashMap actProps) {
        return resolveValue(roleOverride, actProps);
    }

    public void setRoleOverride(String roleOverride) {
        this.roleOverride = roleOverride;
    }

    public TransitionOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(TransitionOutcome outcome) {
        this.outcome = outcome;
    }

    public TransitionScript getScript() {
        return script;
    }

    public void setScript(TransitionScript script) {
        this.script = script;
    }

    public String getReservation() {
        return reservation;
    }

    public void setReservation(String reservation) {
        this.reservation = reservation;
    }

    protected boolean resolveStates(HashMap<Integer, State> states) {
        boolean allFound = true;

        if (states.keySet().contains(originStateId)) {
            setOriginState(states.get(originStateId));
            originState.addPossibleTransition(this);
        }
        else allFound = false;

        if (states.keySet().contains(targetStateId)) setTargetState(states.get(targetStateId));
        else                                         allFound = false;

        return allFound;
    }

    public int getOriginStateId() {
        return originStateId;
    }

    public void setOriginStateId(int originStateId) {
        this.originStateId = originStateId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTargetStateId() {
        return targetStateId;
    }

    public void setTargetStateId(int targetStateId) {
        this.targetStateId = targetStateId;
    }

    public String getPerformingRole(Activity act, AgentPath agent) throws ObjectNotFoundException, AccessRightsException {
        // check available
        if (!isEnabled(act))
            throw new AccessRightsException("Transition '" + name + "' is disabled by the '" + enabledProp + "' property.");

        // check active
        if (isRequiresActive() && !act.getActive()) 
            throw new AccessRightsException("Activity must be active to perform this transition");

        String overridingRole = getRoleOverride(act.getProperties());

        RolePath role = null;
        boolean override = overridingRole != null;
        boolean isOwner = false, isOwned = true;

        // Check agent name
        String agentName = act.getCurrentAgentName();
        if (!StringUtils.isBlank(agentName) && agent.getAgentName().equals(agentName)) isOwner = true;

        // determine transition role
        if (override) {
            role = Gateway.getLookup().getRolePath(overridingRole);
        }
        else {
            String actRole = act.getCurrentAgentRole();
            if (!StringUtils.isBlank(actRole)) role = Gateway.getLookup().getRolePath(actRole);
        }

        // Decide the access
        if (isOwned && !override && !isOwner)
            throw new AccessRightsException("Agent '"+agent.getAgentName()+"' cannot perform this transition because the activity '"+act.getName()+"' is currently owned by "+agentName);

        if (role != null) {
            if (agent.hasRole(role))         return role.getName();
            else if (agent.hasRole("Admin")) return "Admin";
            else 
                throw new AccessRightsException("Agent '"+agent.getAgentName()+"' does not hold a suitable role '"+role.getName()+"' for the activity "+act.getName());
        }
        else return null;
    }

    public String getReservation(Activity act, AgentPath agent) {
        if (StringUtils.isBlank(reservation)) reservation = targetState.finished ? "clear" : "set";

        String reservedAgent = act.getCurrentAgentName();

        if (reservation.equals("set"))        reservedAgent = agent.getAgentName();
        else if (reservation.equals("clear")) reservedAgent = "";

        return reservedAgent;
    }

    private static String resolveValue(String key, CastorHashMap props) {
        if (key == null) return null;
        String result = key;
        Pattern propField = Pattern.compile("\\$\\{(.+?)\\}");
        Matcher propMatcher = propField.matcher(result);
        while (propMatcher.find()) {
            String propName = propMatcher.group(1);
            Object propValue = props.get(propName);
            String propValString = propValue == null ? "" : propValue.toString();
            result = result.replace("${" + propName + "}", propValString);
        }
        Logger.msg(8, "Transition.resolveValue() - returning key '" + key + "' as '" + result + "'");
        return result;
    }

    public boolean isEnabled(Activity act) throws ObjectNotFoundException {
        if (StringUtils.isBlank(enabledProp)) return true;

        try {
            Object propValue = act.evaluateProperty(null, enabledProp, null);
            return new Boolean(propValue.toString());
        }
        catch (InvalidDataException | PersistencyException e) {
            Logger.error(e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    public boolean hasOutcome(CastorHashMap actProps) {
        if (outcome == null || actProps == null) return false;

        String outcomeName = resolveValue(outcome.schemaName, actProps);
        if (StringUtils.isBlank(outcomeName)) return false;

        String outcomeVersion = resolveValue(outcome.schemaVersion, actProps);
        if (StringUtils.isBlank(outcomeVersion)) return false;

        return true;
    }

    public Schema getSchema(CastorHashMap actProps) throws InvalidDataException, ObjectNotFoundException {
        if (hasOutcome(actProps)) try {
            return LocalObjectLoader.getSchema(
                    resolveValue(outcome.schemaName, actProps),
                    Integer.parseInt(resolveValue(outcome.schemaVersion, actProps)));
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("Bad schema version number: "+outcome.schemaVersion+" ("+resolveValue(outcome.schemaVersion, actProps)+")");
        }
        else return null;
    }

    public Script getScript(CastorHashMap actProps) throws ObjectNotFoundException, InvalidDataException {
        if (hasScript(actProps)) {
            try {
                return LocalObjectLoader.getScript(
                        resolveValue(script.scriptName, actProps),
                        Integer.parseInt(resolveValue(script.scriptVersion, actProps)));
            }
            catch (NumberFormatException ex) {
                throw new InvalidDataException("Bad schema version number: " + outcome.schemaVersion + " (" + resolveValue(outcome.schemaVersion, actProps) + ")");
            }
        }
        else return null;
    }

    @Deprecated
    public String getScriptName(CastorHashMap actProps) {
        try {
            return getScript(actProps).getName();
        }
        catch (Exception e) {
            return null;
        }
    }

    @Deprecated
    public int getScriptVersion(CastorHashMap actProps) throws InvalidDataException {
        try {
            return getScript(actProps).getVersion();
        }
        catch (Exception e) {
            return -1;
        }
    }

    public boolean hasScript(CastorHashMap actProps) {
        if (script == null || actProps == null) return false;

        String scriptName = resolveValue(script.scriptName, actProps);
        if (StringUtils.isBlank(scriptName)) return false;

        String scriptVersion = resolveValue(script.scriptVersion, actProps);
        if (StringUtils.isBlank(scriptVersion)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)                  return true;
        if (other == null)                  return false;
        if (getClass() != other.getClass()) return false;
        if (id != ((Transition) other).id)  return false;

        return true;
    }
}
