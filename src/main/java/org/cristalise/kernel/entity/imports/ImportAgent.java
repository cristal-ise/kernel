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
package org.cristalise.kernel.entity.imports;

import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.ActiveEntity;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.module.ModuleImport;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.utils.Logger;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ImportAgent extends ModuleImport {

    private String                password;
    private ArrayList<Property>   properties = new ArrayList<Property>();
    private ArrayList<ImportRole> roles      = new ArrayList<ImportRole>();

    public ImportAgent() {}

    public ImportAgent(String name, String password) {
        this.name = name;
        this.password = password;
    }

    @Override
    public Path create(AgentPath agentPath, boolean reset)
            throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, ObjectAlreadyExistsException
    {
        if (roles.isEmpty()) throw new ObjectNotFoundException("Agent '"+name+"' must declare at least one Role ");

        AgentPath newAgent = new AgentPath(getItemPath(), name);

        ActiveEntity newAgentEnt = Gateway.getCorbaServer().createAgent(newAgent);
        Gateway.getLookupManager().add(newAgent);

        // assemble properties
        properties.add(new Property(NAME, name, true));
        properties.add(new Property(TYPE, "Agent", false));

        try {
            if (StringUtils.isNotBlank(password)) Gateway.getLookupManager().setAgentPassword(newAgent, password);

            newAgentEnt.initialise(
                    agentPath.getSystemKey(), 
                    Gateway.getMarshaller().marshall(new PropertyArrayList(properties)), 
                    null, 
                    null);
        }
        catch (Exception ex) {
            Logger.error(ex);
            throw new CannotManageException("Error initialising new agent name:"+name);
        }

        for (ImportRole role : roles) {
            RolePath thisRole = (RolePath)role.create(agentPath, reset);
            Gateway.getLookupManager().addRole(newAgent, thisRole);
        }
        return newAgent;
    }

    /**
     * Sets the ItemPath representing the Agent. Tries to find Agent if it already exists, 
     * otherwise creates  new ItemPath, i.e. it creates new UUID.
     */
    @Override
    public ItemPath getItemPath() {
        if (itemPath == null) {
            try {
                itemPath = Gateway.getLookup().getAgentPath(name);
            }
            catch (ObjectNotFoundException ex) {
                itemPath = new AgentPath(new ItemPath(), name);
            }
        }
        return itemPath;
    }

    public void setRoles(List<RolePath> newRoles) {
        for (RolePath rp: newRoles) roles.add(ImportRole.getImportRole(rp));
    }
}
