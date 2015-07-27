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

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


public class SetAgentPassword extends PredefinedStep {
	
	public SetAgentPassword() {
        super();
	}
	
	@Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData, Object locker) throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException {

		String[] params = getDataList(requestData);
        if (Logger.doLog(3)) Logger.msg(3, "SetAgentPassword: called by "+agent+" on "+item+" with parameters "+Arrays.toString(params));
        if (params.length != 1) throw new InvalidDataException("SetAgentPassword: Invalid parameters "+Arrays.toString(params));
		
		AgentPath targetAgent;
		try {
			targetAgent = new AgentPath(item);
		} catch (InvalidItemPathException ex) {
			throw new InvalidDataException("Can only set password on an Agent. "+item+" is an Item.");
		}
		
		if (!targetAgent.equals(agent) && !agent.hasRole("Admin")) {
			throw new InvalidDataException("Agent passwords may only be set by those Agents or by an Administrator");
		}
		
		try {
			Gateway.getLookupManager().setAgentPassword(targetAgent, params[0]);
		} catch (NoSuchAlgorithmException e) {
			Logger.error(e);
            throw new InvalidDataException("Cryptographic libraries for password hashing not found.");
		} 
		
		params[0] = "REDACTED"; // censor user's password from outcome
		return bundleData(params);
	}
	
}
