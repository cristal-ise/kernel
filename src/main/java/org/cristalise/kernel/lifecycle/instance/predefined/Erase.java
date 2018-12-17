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
package org.cristalise.kernel.lifecycle.instance.predefined;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_ROLE;

import java.util.Iterator;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

public class Erase extends PredefinedStep {
    public static final String description =  "Deletes all domain paths (aliases), roles (if agent) and clusters for this item or agent.";

    public Erase() {
        super();
        String extraRoles = Gateway.getProperties().getString("PredefinedStep.Erase.roles");
        getProperties().put(AGENT_ROLE.getName(), "Admin" + (extraRoles != null ? ","+extraRoles : ""));
    }

    /**
     * {@value #description}}
     * 
     * @param requestData is empty
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, PersistencyException
    {
        Logger.msg(1, "Erase.request() - Starting item:"+item);

        removeAliases(item);
        removeRolesIfAgent(item);
        Gateway.getStorage().removeCluster(item, "", locker); //removes all clusters

        Logger.msg(1, "Erase.request() - DONE item:"+item);

        return requestData;
    }

    /**
     * 
     * @param item
     * @throws ObjectNotFoundException
     * @throws ObjectCannotBeUpdated
     * @throws CannotManageException
     */
    private void removeAliases(ItemPath item) throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException {
        Iterator<Path> domPaths = Gateway.getLookup().searchAliases(item);

        while (domPaths.hasNext()) {
            DomainPath path = (DomainPath) domPaths.next();
            Gateway.getLookupManager().delete(path);
        }
    }

    /**
     * 
     * @param item
     * @throws InvalidDataException
     * @throws ObjectCannotBeUpdated
     * @throws ObjectNotFoundException
     * @throws CannotManageException
     */
    private void removeRolesIfAgent(ItemPath item) throws InvalidDataException, ObjectCannotBeUpdated, ObjectNotFoundException, CannotManageException {
        try {
            AgentPath targetAgent = new AgentPath(item);
            
            //This check if the item is an agent or not
            if (targetAgent.getAgentName() != null) {
                for (RolePath role : targetAgent.getRoles()) {
                    Gateway.getLookupManager().removeRole(targetAgent, role);
                }
            }
        }
        catch (InvalidAgentPathException e) {
            //this is actually never happens, new AgentPath(item) deos not throw InvalidAgentPathException
            //but the exception is needed for 'backward compability'
        }
    }
}
