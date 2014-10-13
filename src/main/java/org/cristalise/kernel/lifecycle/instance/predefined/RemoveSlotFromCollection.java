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

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2004/10/21 08:02:19 $
 * @version $Revision: 1.8 $
 **************************************************************************/
public class RemoveSlotFromCollection extends PredefinedStep
{
    /**************************************************************************
    * Constructor for Castor
    **************************************************************************/
    public RemoveSlotFromCollection()
    {
        super();
    }


    /**
     * Params:
     * 0 - collection name
     * 1 - slot number OR if -1:
     * 2 - target entity key
     * @throws ObjectNotFoundException 
     * @throws PersistencyException 
     */
    @Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData) throws InvalidDataException, ObjectNotFoundException, PersistencyException {

        String collName;
        int slotNo = -1;
        ItemPath currentChild = null;
        Collection<? extends CollectionMember> coll;

        // extract parameters
        String[] params = getDataList(requestData);
        if (Logger.doLog(3)) Logger.msg(3, "RemoveSlotFromCollection: called by "+agent+" on "+item+" with parameters "+Arrays.toString(params));

        try {
            collName = params[0];
            if (params.length>1 && params[1].length()>0) slotNo = Integer.parseInt(params[1]);
            if (params.length>2 && params[2].length()>0) {
            	try {
            		currentChild = new ItemPath(params[2]);
            	} catch (InvalidItemPathException e) {
            		currentChild = new DomainPath(params[2]).getItemPath();
            	}
            }
        } catch (Exception e) {
            throw new InvalidDataException("RemoveSlotFromCollection: Invalid parameters "+Arrays.toString(params));
        }
        
        if (slotNo == -1 && currentChild == null)
        	throw new InvalidDataException("RemoveSlotFromCollection: Must give either slot number or entity key");

        // load collection
        try {
			coll = (Collection<? extends CollectionMember>)Gateway.getStorage().get(item, ClusterStorage.COLLECTION+"/"+collName+"/last", null);
		} catch (PersistencyException ex) {
			Logger.error(ex);
			throw new PersistencyException("RemoveSlotFromCollection: Error loading collection '\"+collName+\"': "+ex.getMessage());
		}

        // check the slot is there if it's given by id
        CollectionMember slot = null;
        if (slotNo > -1) {
			slot = coll.getMember(slotNo);
        }

        // if both parameters are supplied, check the given item is actually in that slot
        if (slot != null && currentChild != null && !slot.getItemPath().equals(currentChild)) {
        		throw new ObjectNotFoundException("RemoveSlotFromCollection: Item "+currentChild+" was not in slot "+slotNo);
        }

        if (slotNo == -1) { // find slot from entity key
        	for (CollectionMember member : coll.getMembers().list) {
        		if (member.getItemPath().equals(currentChild)) {
        			slotNo = member.getID();
        			break;
        		}
            }
        }
        if (slotNo == -1) {
            throw new ObjectNotFoundException("Could not find "+currentChild+" in collection "+coll.getName());
        }
        
        // Remove the slot
		coll.removeMember(slotNo);

        // Store the collection
		try {
            Gateway.getStorage().put(item, coll, null);
        } catch (PersistencyException e) {
            Logger.error(e);
            throw new PersistencyException("Error storing collection");
        }
		
        return requestData;
        
    }
}
