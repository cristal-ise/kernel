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
package org.cristalise.kernel.lifecycle.instance.predefined.server;

import java.util.Arrays;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

public class RemoveRole extends PredefinedStep {
    public RemoveRole() {
        super();
        getProperties().put("Agent Role", "Admin");
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, Object locker)
            throws InvalidDataException, CannotManageException, ObjectNotFoundException, ObjectCannotBeUpdated
    {
        String[] params = getDataList(requestData);

        Logger.msg(3, "RemoveRole: called by " + agent + " on " + item + " with parameters " + Arrays.toString(params));

        if (params.length != 1) throw new InvalidDataException("RemoveRole must have one paramater:" + Arrays.toString(params));

        LookupManager lookup = Gateway.getLookupManager();

        RolePath thisRole = lookup.getRolePath(params[0]);
        AgentPath[] agents = Gateway.getLookup().getAgents(thisRole);

        if (agents.length > 0)
            throw new ObjectCannotBeUpdated("Cannot remove role as " + agents.length + " other agents still hold it.");

        lookup.delete(thisRole);

        return requestData;
    }
}
