/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


public class SetAgentRoles extends PredefinedStep {

	public SetAgentRoles() {
        super();
        getProperties().put("Agent Role", "Admin");
	}
	
	@Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData) throws InvalidDataException {
	
		String[] params = getDataList(requestData);
		if (Logger.doLog(3)) Logger.msg(3, "AddC2KObject: called by "+agent+" on "+item+" with parameters "+Arrays.toString(params));
		AgentPath targetAgent;
		try {
			targetAgent = new AgentPath(item);
		} catch (InvalidItemPathException ex) {
			throw new InvalidDataException("Could not resolve syskey "+item+" as an Agent.");
		}
		
		RolePath[] currentRoles = targetAgent.getRoles();
		ArrayList<RolePath> requestedRoles = new ArrayList<RolePath>();
		for (int i=0; i<params.length; i++)
			try {
				requestedRoles.add(Gateway.getLookup().getRolePath(params[i]));
			} catch (ObjectNotFoundException e) {
				throw new InvalidDataException("Role "+params[i]+" not found");
			}
		
		ArrayList<RolePath> rolesToRemove = new ArrayList<RolePath>();
		for (RolePath existingRole : currentRoles) { //
			if (requestedRoles.contains(existingRole)) // if we have it, and it's requested, then it will be kept
				requestedRoles.remove(existingRole); // so remove it from request - this will be left with roles to be added
			else
				rolesToRemove.add(existingRole); // else this role will be removed
		}
		
		// remove roles not in new list
		for (RolePath roleToRemove : rolesToRemove)
			try {
				Gateway.getLookupManager().removeRole(targetAgent, roleToRemove);
			} catch (Exception e) {
				Logger.error(e);
				throw new InvalidDataException("Error removing role "+roleToRemove.getName());
			}
		
		// add requested roles we don't already have
		for (RolePath roleToAdd : requestedRoles)
			try {
				Gateway.getLookupManager().addRole(targetAgent, roleToAdd);
			} catch (Exception e) {
				Logger.error(e);
				throw new InvalidDataException("Error adding role "+roleToAdd.getName());
			}
		
		return requestData;
	}

}
