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
package org.cristalise.kernel.lifecycle.instance.predefined;

import java.util.Arrays;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.CorbaServer;
import org.cristalise.kernel.entity.agent.ActiveEntity;
import org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2005/10/13 08:13:58 $
 * @version $Revision: 1.47 $
 **************************************************************************/
public class CreateAgentFromDescription extends CreateItemFromDescription
{
	public CreateAgentFromDescription()
	{
		super();
	}

	/**
	 * Params:
	 * <ol><li>New Agent name</li>
	 * <li>Description version to use</li>
	 * <li>Comma-delimited Role names to assign to the agent. Must already exist.</li>
	 * <li>Initial properties to set in the new Agent</li>
	 * </ol>
	 * @throws ObjectNotFoundException 
	 * @throws InvalidDataException The input parameters were incorrect
	 * @throws ObjectAlreadyExistsException The Agent already exists
	 * @throws CannotManageException The Agent could not be created
	 * @throws ObjectCannotBeUpdated The addition of the new entries into the LookupManager failed
	 * @see org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription#runActivityLogic(org.cristalise.kernel.lookup.AgentPath, int, int, java.lang.String)
	 */
	@Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData, Object locker) throws ObjectNotFoundException, InvalidDataException, ObjectAlreadyExistsException, CannotManageException, ObjectCannotBeUpdated {
		
		String[] input = getDataList(requestData);
		if (Logger.doLog(3)) Logger.msg(3, "CreateAgentFromDescription: called by "+agent+" on "+item+" with parameters "+Arrays.toString(input));
		if (input.length < 3 || input.length > 4) 
			throw new InvalidDataException("CreateAgentFromDescription: Invalid parameters "+Arrays.toString(input));
		
		String newName = input[0];
		String domPath = input[1];
		String descVer = input.length > 2 ? input[2]:"last";
		PropertyArrayList initProps = 
				input.length > 3?getInitProperties(input[3]):new PropertyArrayList();
				
		Logger.msg(1, "CreateAgentFromDescription::request() - Starting.");

        // check if the path is already taken
    	try {
    		Gateway.getLookup().getAgentPath(newName);
    		throw new ObjectAlreadyExistsException("The agent name " +newName+ " exists already.");
    	} catch (ObjectNotFoundException ex) { }
    	
		DomainPath context = new DomainPath(new DomainPath(domPath), newName);
		if (context.exists())
            throw new ObjectAlreadyExistsException("The path " +context+ " exists already.");

        // generate new agent path with new UUID
        Logger.msg(6, "CreateAgentFromDescription - Requesting new agent path");
        AgentPath newAgentPath = new AgentPath(new ItemPath(), newName);

        // create the Agent object
        Logger.msg(3, "CreateAgentFromDescription - Creating Agent");
        CorbaServer factory = Gateway.getCorbaServer();
        if (factory == null) throw new CannotManageException("This process cannot create new Items");
        ActiveEntity newAgent = factory.createAgent(newAgentPath);
        Gateway.getLookupManager().add(newAgentPath);
        // give it the base role
        Gateway.getLookupManager().addRole(newAgentPath, Gateway.getLookup().getRolePath(""));
        
        // initialise it with its properties and workflow

        Logger.msg(3, "CreateAgentFromDescription - Initializing Agent");

        try {
			newAgent.initialise(
			    agent.getSystemKey(),
				Gateway.getMarshaller().marshall(getNewProperties(item, descVer, initProps, newName, agent, locker)),
				Gateway.getMarshaller().marshall(getNewWorkflow(item, descVer, locker)),
				Gateway.getMarshaller().marshall(getNewCollections(item, descVer, locker))
				);
		} catch (PersistencyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			throw new InvalidDataException("CreateAgentFromDescription: Problem initializing new Agent. See log: "+e.getMessage());
		}
        // Create domain path
        context.setItemPath(newAgentPath);
        Gateway.getLookupManager().add(context);
        return requestData;
	}
}
