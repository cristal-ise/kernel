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

import org.cristalise.kernel.collection.AggregationDescription;
import org.cristalise.kernel.collection.CollectionDescription;
import org.cristalise.kernel.collection.DependencyDescription;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


/**
 * Generates a new empty collection description. Collection instances should
 * be added by an Admin, who can do so using AddC2KObject.
 */
public class AddNewCollectionDescription extends PredefinedStep {
    /**************************************************************************
     * Constructor for Castor
     **************************************************************************/
    public AddNewCollectionDescription() {
        super();
    }

    /**
     * Params: 0 - collection name 1 - collection type (Aggregation, Dependency)
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectAlreadyExistsException, PersistencyException
    {
        // extract parameters
        String[] params = getDataList(requestData);

        if (Logger.doLog(3))
            Logger.msg(3, "AddNewCollectionDescription: called by " + agent + " on " + item + " with parameters " + Arrays.toString(params));

        if (params.length != 2)
            throw new InvalidDataException("AddNewCollectionDescription: Invalid parameters " + Arrays.toString(params));

        String collName = params[0];
        String collType = params[1];

        // check if collection already exists
        try {
            Gateway.getStorage().get(item, ClusterType.COLLECTION + "/" + collName + "/last", locker);
            throw new ObjectAlreadyExistsException("Collection '" + collName + "' already exists");
        }
        catch (ObjectNotFoundException ex) {
            // collection doesn't exist
        }
        catch (PersistencyException ex) {
            Logger.error(ex);
            throw new PersistencyException("AddNewCollectionDescription: Error checking for collection '" + collName + "': "
                    + ex.getMessage());
        }

        CollectionDescription<?> newCollDesc;

        if (collType.equalsIgnoreCase("Aggregation"))
            newCollDesc = new AggregationDescription(collName);
        else if (collType.equalsIgnoreCase("Dependency"))
            newCollDesc = new DependencyDescription(collName);
        else
            throw new InvalidDataException("AddNewCollectionDescription: Invalid collection type specified: '" + collType
                    + "'. Must be Aggregation or Dependency.");

        // store it
        try {
            Gateway.getStorage().put(item, newCollDesc, locker);
        }
        catch (PersistencyException e) {
            throw new PersistencyException("AddNewCollectionDescription: Error saving new collection '" + collName + "': " + e.getMessage());
        }
        return requestData;
    }
}
