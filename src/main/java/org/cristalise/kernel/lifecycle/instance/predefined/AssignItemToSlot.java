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

import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.AggregationMember;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
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
public class AssignItemToSlot extends PredefinedStep
{
    /**************************************************************************
    * Constructor for Castor
    **************************************************************************/
    public AssignItemToSlot()
    {
        super();
    }


    /**
     * Params:
     * 0 - collection name
     * 1 - slot number
     * 2 - target entity key
     * @throws ObjectNotFoundException 
     * @throws PersistencyException 
     * @throws ObjectCannotBeUpdated 
     * @throws InvalidCollectionModification 
     */
    @Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData) throws InvalidDataException, ObjectNotFoundException, PersistencyException, ObjectCannotBeUpdated, InvalidCollectionModification {
    	
        String collName;
        int slotNo;
        ItemPath childItem;
        Aggregation agg;

        // extract parameters
        String[] params = getDataList(requestData);
        if (Logger.doLog(3)) Logger.msg(3, "AssignItemToSlot: called by "+agent+" on "+item+" with parameters "+Arrays.toString(params));

        try {
            collName = params[0];
            slotNo = Integer.parseInt(params[1]);
        	try {
        		childItem = new ItemPath(params[2]);
        	} catch (InvalidItemPathException e) {
        		childItem = new DomainPath(params[2]).getItemPath();
        	}
        } catch (Exception e) {
        	Logger.error(e);
            throw new InvalidDataException("AssignItemToSlot: Invalid parameters "+Arrays.toString(params));
        }

        // load collection
        C2KLocalObject collObj;
        try {
        	collObj = Gateway.getStorage().get(item, ClusterStorage.COLLECTION+"/"+collName+"/last", null);
		} catch (PersistencyException ex) {
			Logger.error(ex);
			throw new PersistencyException("AssignItemToSlot: Error loading collection '\"+collName+\"': "+ex.getMessage());
		}
    	if (!(collObj instanceof Aggregation)) throw new InvalidDataException("AssignItemToSlot: AssignItemToSlot operates on Aggregation collections only.");
        agg = (Aggregation)collObj;

        // find member and assign entity
        boolean stored = false;
        for (AggregationMember member : agg.getMembers().list) {
            if (member.getID() == slotNo) {
                if (member.getItemPath() != null)
                    throw new ObjectCannotBeUpdated("AssignItemToSlot: Member slot "+slotNo+" not empty");
                member.assignItem(childItem);
                stored = true;
                break;
            }
        }
        if (!stored) {
            throw new ObjectNotFoundException("AssignItemToSlot: Member slot "+slotNo+" not found.");
        }

		try {
            Gateway.getStorage().put(item, agg, null);
        } catch (PersistencyException e) {
        	throw new PersistencyException("AssignItemToSlot: Error saving collection '"+collName+"': "+e.getMessage());
        }
        return requestData;
    }
}
