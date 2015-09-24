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
package org.cristalise.kernel.entity.imports;

import java.util.ArrayList;

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


public class ImportAgent extends ModuleImport {

    private String password;
	private ArrayList<Property> properties  = new ArrayList<Property>();
    private ArrayList<String> roles = new ArrayList<String>();

    public ImportAgent() {
    }

    public ImportAgent(String name, String password) {
        this.name = name;
        this.password = password;
    }

    @Override
	public Path create(AgentPath agentPath, boolean reset) throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, ObjectAlreadyExistsException {
        AgentPath newAgent = new AgentPath(getItemPath(), name);
        newAgent.setPassword(password);
        ActiveEntity newAgentEnt = Gateway.getCorbaServer().createAgent(newAgent);
        Gateway.getLookupManager().add(newAgent);
        // assemble properties
        properties.add(new Property("Name", name, true));
        properties.add(new Property("Type", "Agent", false));
        try {
            newAgentEnt.initialise(agentPath.getSystemKey(), Gateway.getMarshaller().marshall(new PropertyArrayList(properties)), null, null);
        } catch (Exception ex) {
            Logger.error(ex);
            throw new CannotManageException("Error initialising new agent");
        }
        for (String role : roles) {
            RolePath thisRole;
            try {
                thisRole = Gateway.getLookup().getRolePath(role);
            } catch (ObjectNotFoundException ex) {
                throw new ObjectNotFoundException("Role "+role+" does not exist.");
            }
            Gateway.getLookupManager().addRole(newAgent, thisRole);
        }
        return newAgent;
    }

    @Override
    public ItemPath getItemPath() {
        if (itemPath == null) { // try to find agent if it already exists
        	try {
        		AgentPath existAgent = Gateway.getLookup().getAgentPath(name);
        		itemPath = existAgent;
        	} catch (ObjectNotFoundException ex) {
        		itemPath = new AgentPath(new ItemPath(), name);
        	}
        }
        return itemPath;
    }
    
    public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public ArrayList<String> getRoles() {
		return roles;
	}

	public void setRoles(ArrayList<String> roles) {
		this.roles = roles;
	}

	public ArrayList<Property> getProperties() {
		return properties;
	}

	public void setProperties(ArrayList<Property> properties) {
		this.properties = properties;
	}

}
