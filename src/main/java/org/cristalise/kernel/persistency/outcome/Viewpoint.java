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
package org.cristalise.kernel.persistency.outcome;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;


/**
 * @author Andrew Branson
 *
 * $Revision: 1.10 $
 * $Date: 2005/10/05 07:39:36 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 */

public class Viewpoint implements C2KLocalObject {

	// db fields
	ItemPath itemPath;
	String schemaName;
	String name;
	int schemaVersion;
	int eventId;
	public static final int NONE = -1;

	public Viewpoint() {
		eventId = NONE;
		itemPath = null;
		schemaVersion = NONE;
		schemaName = null;
		name = null;
	}

	public Viewpoint(ItemPath itemPath, String schemaName, String name, int schemaVersion, int eventId) {
		this.itemPath = itemPath;
		this.schemaName = schemaName;
		this.name = name;
		this.schemaVersion = schemaVersion;
		this.eventId = eventId;
	}

	public Outcome getOutcome() throws ObjectNotFoundException, PersistencyException {
		if (eventId == NONE) throw new ObjectNotFoundException("No last eventId defined");
		Outcome retVal = (Outcome)Gateway.getStorage().get(itemPath, ClusterStorage.OUTCOME+"/"+schemaName+"/"+schemaVersion+"/"+eventId, null);
		return retVal;
	}

	@Override
	public String getClusterType() {
		return ClusterStorage.VIEWPOINT;
	}


	/**
	 * Returns the eventId.
	 * @return int
	 */
	public int getEventId() {
		return eventId;
	}

	/**
	 * Returns the name.
	 * @return String
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Returns the schemaName.
	 * @return String
	 */
	public String getSchemaName() {
		return schemaName;
	}

	/**
	 * Returns the schemaVersion.
	 * @return int
	 */
	public int getSchemaVersion() {
		return schemaVersion;
	}

	/**
	 * Returns the sysKey.
	 * @return int
	 */
	public ItemPath getItemPath() {
		return itemPath;
	}

	/**
	 * Sets the eventId.
	 * @param eventId The eventId to set
	 */
	public void setEventId(int eventId) {
		this.eventId = eventId;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the schemaName.
	 * @param schemaName The schemaName to set
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * Sets the schemaVersion.
	 * @param schemaVersion The schemaVersion to set
	 */
	public void setSchemaVersion(int schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	/**
	 * Sets the sysKey.
	 * @param sysKey The sysKey to set
	 */
	public void setItemPath(ItemPath itemPath) {
		this.itemPath = itemPath;
	}
	
    public void setItemUUID( String uuid ) throws InvalidItemPathException
    {
    	setItemPath(new ItemPath(uuid));
    }
    
    public String getItemUUID() {
    	return getItemPath().getUUID().toString();
    }

	/**
	 * Method getEvent.
	 * @return GDataRecord
	 */
	public Event getEvent()
        throws InvalidDataException, PersistencyException, ObjectNotFoundException
	{
        if (eventId == NONE)
            throw new InvalidDataException("No last eventId defined");

        return (Event)Gateway.getStorage().get(itemPath, ClusterStorage.HISTORY+"/"+eventId, null);
	}

    @Override
	public String toString() {
        return name;
    }

}
