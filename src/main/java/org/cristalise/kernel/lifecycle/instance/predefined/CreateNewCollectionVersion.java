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
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2004/10/21 08:02:19 $
 * @version $Revision: 1.8 $
 **************************************************************************/
public class CreateNewCollectionVersion extends PredefinedStep
{
    /**************************************************************************
    * Constructor for Castor
    **************************************************************************/
    public CreateNewCollectionVersion()
    {
        super();
    }


    /**
     * Generates a new snapshot of a collection from its current state. The 
     * new version is given the next available number, starting at 0.
     * 
     * Params:
     * 0 - Collection name
     * @throws InvalidDataException 
     * @throws PersistencyException 
     *  
     * @throws ObjectNotFoundException when there is no collection present with
     * that name 
     */
    @Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData, Object locker) throws InvalidDataException, PersistencyException, ObjectNotFoundException 
    {
        String collName;

        // extract parameters
        String[] params = getDataList(requestData);
        if (Logger.doLog(3)) Logger.msg(3, "CreateNewCollectionVersion: called by "+agent+" on "+item+" with parameters "+Arrays.toString(params));
        if (params.length == 0 || params.length > 2)
        	throw new InvalidDataException("CreateNewCollectionVersion: Invalid parameters "+Arrays.toString(params));

        collName = params[0];
        Collection<?> coll = (Collection<?>)Gateway.getStorage().get(item, ClusterStorage.COLLECTION+"/"+collName+"/last", locker);
        int newVersion;
        
        if (params.length > 1) {
        	newVersion = Integer.valueOf(params[1]);
        }
        else {
	        // find last numbered version
	        String[] versions = Gateway.getStorage().getClusterContents(item, ClusterStorage.COLLECTION+"/"+collName);
	        int lastVer = -1;
	        for (String thisVerStr : versions) {
				try {
					int thisVer = Integer.parseInt(thisVerStr);
					if (thisVer > lastVer) lastVer = thisVer;
				} catch (NumberFormatException ex) { } // ignore non-integer versions
			}
	        newVersion = lastVer + 1;
        }
        
        // Remove it from the cache before we change it
        Gateway.getStorage().clearCache(item, ClusterStorage.COLLECTION+"/"+collName+"/last");
        // Set the version
        coll.setVersion(newVersion);
        
        // store it
		try {
            Gateway.getStorage().put(item, coll, locker);
        } catch (PersistencyException e) {
        	throw new PersistencyException("CreateNewCollectionVersion: Error saving new collection '"+collName+"': "+e.getMessage());
        }
        return requestData;
    }
}
