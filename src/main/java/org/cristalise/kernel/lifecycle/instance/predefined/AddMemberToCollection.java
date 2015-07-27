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

import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2004/10/21 08:02:19 $
 * @version $Revision: 1.8 $
 **************************************************************************/
public class AddMemberToCollection extends PredefinedStep
{
    /**************************************************************************
    * Constructor for Castor
    **************************************************************************/
    public AddMemberToCollection()
    {
        super();
    }


    /**
     * Generates a new slot in a Dependency for the given item
     * 
     * Params:
     * 0 - collection name
     * 1 - target entity key
     * 2 - slot properties
     * @throws ObjectAlreadyExistsException 
     * @throws PersistencyException 
     * @throws ObjectNotFoundException 
     * @throws InvalidCollectionModification 
     */
    @Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData, Object locker) throws InvalidDataException, ObjectAlreadyExistsException, PersistencyException, ObjectNotFoundException, InvalidCollectionModification {
    	
        String collName;
        ItemPath newChild;
        Dependency dep;
        CastorHashMap props = null;

        // extract parameters
        String[] params = getDataList(requestData);
        if (Logger.doLog(3)) Logger.msg(3, "AddMemberToCollection: called by "+agent+" on "+item+" with parameters "+Arrays.toString(params));
        try {
            collName = params[0];
        	try {
        		newChild = new ItemPath(params[1]);
        	} catch (InvalidItemPathException e) {
        		newChild = new DomainPath(params[1]).getItemPath();
        	}
            if (params.length > 2)
            	props = (CastorHashMap)Gateway.getMarshaller().unmarshall(params[2]);
            
        } catch (Exception e) {
            throw new InvalidDataException("AddMemberToCollection: Invalid parameters "+Arrays.toString(params));
        }

        // load collection
    	C2KLocalObject collObj;
		collObj = Gateway.getStorage().get(item, ClusterStorage.COLLECTION+"/"+collName+"/last", locker);
    	if (!(collObj instanceof Dependency)) throw new InvalidDataException("AddMemberToCollection: AddMemberToCollection operates on Dependency collections only.");
        dep = (Dependency)collObj;
        
        // find member and assign entity
    	if (props == null)
    		dep.addMember(newChild);
    	else
    		dep.addMember(newChild, props, dep.getClassProps());

        Gateway.getStorage().put(item, dep, locker);
        return requestData;
    }
}
