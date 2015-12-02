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
import java.util.Set;

public class State {

	int id;
	String name;
	boolean finished = false; // If true, this state deactivates the current activity and the lifecycle proceeds

	HashMap<Integer, Transition> possibleTransitions;

	public State() {
		possibleTransitions = new HashMap<Integer, Transition>();
	}

    public State(int id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return id+": "+name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	
	public HashMap<Integer, Transition> getPossibleTransitions() {
		return possibleTransitions;
	}

	protected void addPossibleTransition(Transition possibleTransition) {
		possibleTransitions.put(possibleTransition.getId(), possibleTransition);
	}

	public Set<Integer> getPossibleTransitionIds() {
		return possibleTransitions.keySet();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		State other = (State) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
