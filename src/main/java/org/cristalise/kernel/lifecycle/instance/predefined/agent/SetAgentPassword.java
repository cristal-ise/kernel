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

import java.security.NoSuchAlgorithmException;

import org.cristalise.kernel.common.AccessRightsException;
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
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, AccessRightsException
    {
        String[] params = getDataList(requestData);

        Logger.msg(3, "SetAgentPassword: called by " + agent + " on " + item + " with parameters length:" + params.length);

        //FIXME params.length != 1 case is deprecated, shall enforce identity check
        if (params.length != 1 && params.length != 2) 
            throw new InvalidDataException("SetAgentPassword: Invalid number of parameters length:" + params.length);

        try {
            AgentPath targetAgent = new AgentPath(item);
            String newPwd;

            if (!targetAgent.equals(agent) && !agent.hasRole("Admin"))
                throw new AccessRightsException("Agent passwords may only be set by those Agents or by an Administrator");

            if (params.length == 1) {
                //FIXME these case is deprecated, shall enforce identity check
                newPwd = params[0];
                params[0] = "REDACTED"; // censor password from outcome
            }
            else {
                //Enforce identity check
                if (!Gateway.getAuthenticator().authenticate(agent.getAgentName(), params[0], null))
                    throw new AccessRightsException("Authentication failed");

                newPwd = params[1];
                params[0] = "REDACTED"; // censor password from outcome
                params[1] = "REDACTED"; // censor password from outcome
            }

            Gateway.getLookupManager().setAgentPassword(targetAgent, newPwd);

            return bundleData(params);
        }
        catch (InvalidItemPathException ex) {
            Logger.error(ex);
            throw new InvalidDataException("Can only set password on an Agent. " + item + " is an Item.");
        }
        catch (NoSuchAlgorithmException e) {
            Logger.error(e);
            throw new InvalidDataException("Cryptographic libraries for password hashing not found.");
        }
    }
}
