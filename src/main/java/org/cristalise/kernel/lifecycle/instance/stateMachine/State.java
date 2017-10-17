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
import java.util.Map.Entry;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class State {

    int     id;
    String  name;
    /**
     * If true, this state deactivates the current activity and the lifecycle/workflow proceeds
     */
    boolean finished = false;

    @Setter(AccessLevel.NONE)
    HashMap<Integer, Transition> possibleTransitions;

    public State() {
        possibleTransitions = new HashMap<Integer, Transition>();
    }

    public State(int id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    protected void addPossibleTransition(Transition possibleTransition) {
        possibleTransitions.put(possibleTransition.getId(), possibleTransition);
    }

    public Set<Integer> getPossibleTransitionIds() {
        return possibleTransitions.keySet();
    }

    public int getErrorTansitionId() {
        for (Entry<Integer, Transition> entry : possibleTransitions.entrySet()) {
            if (entry.getValue().isErrorHandler()) return entry.getKey();
        }
        return -1;
    }

    @Override
    public String toString() {
        return name+"[id:"+id+"]";
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
        if (id != ((State)other).id)        return false;

        return true;
    }
}
