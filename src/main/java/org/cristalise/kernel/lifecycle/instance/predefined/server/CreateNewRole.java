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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SchemaType;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.imports.ImportRole;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


public class CreateNewRole extends PredefinedStep
{
    public CreateNewRole()
    {
        super();
        setBuiltInProperty(SchemaType, "Role");
    }

	//requestdata is xmlstring
    @Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData, Object locker) throws InvalidDataException, ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, ObjectNotFoundException {

    	ImportRole newRole;
		try {
			newRole = (ImportRole)Gateway.getMarshaller().unmarshall(requestData);
		} catch (Exception e) {
			Logger.error(e);
			throw new InvalidDataException("CreateNewRole: Couldn't unmarshall new Role: "+requestData);
		}
		if (Gateway.getLookup().getRolePath(newRole.getName()).exists())
			throw new ObjectAlreadyExistsException("Role '"+newRole.getName()+"' already exists.");
    	newRole.create(agent, true);
    	return requestData;
    }
}
