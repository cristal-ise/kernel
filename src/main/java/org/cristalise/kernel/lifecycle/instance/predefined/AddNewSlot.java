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


import java.util.Arrays;

import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyDescription;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.property.PropertyUtility;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2004/10/21 08:02:19 $
 * @version $Revision: 1.8 $
 **************************************************************************/
public class AddNewSlot extends PredefinedStep
{
    /**************************************************************************
    * Constructor for Castor
    **************************************************************************/
    public AddNewSlot()
    {
        super();
    }


    /**
     * Creates a new slot in the given aggregation, that holds instances of the given item description
     * 
     * Params:
     * <ol><li>Collection name</li>
     * <li>Item Description key (optional)</li>
     * <li>Item Description version (optional)</li>
     * </ol>
     * 
     * @throws InvalidDataException Then the parameters were incorrect
     * @throws PersistencyException There was a problem loading or saving the collection from persistency
     * @throws ObjectNotFoundException A required object, such as the collection or a PropertyDescription outcome, wasn't found
     */
    @Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData, Object locker) throws InvalidDataException, PersistencyException, ObjectNotFoundException {
    	
        String collName;
        ItemPath descKey = null;
        String descVer = "last";
        Aggregation agg;

        // extract parameters
        String[] params = getDataList(requestData);
        if (Logger.doLog(3)) Logger.msg(3, "AddNewSlot: called by "+agent+" on "+item+" with parameters "+Arrays.toString(params));

        // resolve desc item path and version
        try {
            collName = params[0];
            if (params.length > 1 && params[1].length() > 0) {
            	try {
            		descKey = new ItemPath(params[1]);
            	} catch (InvalidItemPathException e) {
            		descKey = new DomainPath(params[1]).getItemPath();
            	}
            }
            if (params.length > 2 && params[2].length() > 0) descVer = params[2];
        } catch (Exception e) {
            throw new InvalidDataException("AddNewSlot: Invalid parameters "+Arrays.toString(params));
        }

        // load collection
    	C2KLocalObject collObj;
		try {
			collObj = Gateway.getStorage().get(item, ClusterType.COLLECTION+"/"+collName+"/last", locker);
		} catch (PersistencyException ex) {
			Logger.error(ex);
			throw new PersistencyException("AddNewSlot: Error loading collection '\"+collName+\"': "+ex.getMessage());
		}
    	if (!(collObj instanceof Aggregation)) throw new InvalidDataException("AddNewSlot: AddNewSlot operates on Aggregation collections only.");
        agg = (Aggregation)collObj;

        // get props
        CastorHashMap props = new CastorHashMap();
        StringBuffer classProps = new StringBuffer();
        if (descKey != null) {
        	PropertyDescriptionList propList;
			propList = PropertyUtility.getPropertyDescriptionOutcome(descKey, descVer, locker);
            for (PropertyDescription pd : propList.list) {
				props.put(pd.getName(), pd.getDefaultValue());
				if (pd.getIsClassIdentifier())
					classProps.append((classProps.length()>0?",":"")).append(pd.getName());
			}
        }
        
        agg.addSlot(props, classProps.toString());

		try {
            Gateway.getStorage().put(item, agg, locker);
        } catch (PersistencyException e) {
            Logger.error(e);
            throw new PersistencyException("AddNewSlot: Error saving collection '"+collName+"': "+e.getMessage());
        }

        return requestData;
    }
}
