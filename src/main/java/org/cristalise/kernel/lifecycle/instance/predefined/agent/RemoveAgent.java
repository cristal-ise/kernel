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
package org.cristalise.kernel.lifecycle.instance.predefined.agent;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lifecycle.instance.predefined.item.Erase;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


public class RemoveAgent extends Erase {

	public RemoveAgent() {
        super();
        getProperties().put("Agent Role", "Admin");
	}
	
	@Override
	protected String runActivityLogic(AgentPath agent, ItemPath itemPath,
			int transitionID, String requestData, Object locker) throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, PersistencyException {
	
		Logger.msg(1, "RemoveAgent::request() - Starting.");
		
		AgentPath targetAgent;
		try {
			targetAgent = new AgentPath(itemPath);
		} catch (InvalidAgentPathException ex) {
			throw new InvalidDataException("Could not resolve "+itemPath+" as an Agent.");
		}
		String agentName = targetAgent.getAgentName();
		
		//remove from roles
		for (RolePath role: targetAgent.getRoles()) {
			try {
				Gateway.getLookupManager().removeRole(targetAgent, role);
			} catch (ObjectCannotBeUpdated e) {
				Logger.error(e);
				throw new InvalidDataException("Error removing "+agentName+" from Role "+role.getName());
			} catch (ObjectNotFoundException e) {
				Logger.error(e);
				throw new InvalidDataException("Tried to remove "+agentName+" from Role "+role.getName()+" that doesn't exist.");
			} catch (CannotManageException e) {
				throw new InvalidDataException("Tried to alter roles in a non-server process.");
			}
		}
		
		return super.runActivityLogic(agent, itemPath, transitionID, requestData, locker);

	}

}
