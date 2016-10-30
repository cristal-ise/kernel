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
package org.cristalise.kernel.persistency.outcome;

import lombok.Getter;
import lombok.Setter;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;

@Getter @Setter
public class Viewpoint implements C2KLocalObject {

    public static final int NONE = -1;

    // db fields
    ItemPath  itemPath;
    String    schemaName;
    String    name;
    int       schemaVersion;
    int       eventId;

    public Viewpoint() {
        eventId = NONE;
        itemPath = null;
        schemaVersion = NONE;
        schemaName = null;
        name = null;
    }

    @Deprecated
    public Viewpoint(ItemPath itemPath, String schemaName, String name, int schemaVersion, int eventId) {
        this.itemPath = itemPath;
        this.schemaName = schemaName;
        this.name = name;
        this.schemaVersion = schemaVersion;
        this.eventId = eventId;
    }

    public Viewpoint(ItemPath itemPath, Schema schema, String name, int eventId) {
        this.itemPath = itemPath;
        this.schemaName = schema.getName();
        this.name = name;
        this.schemaVersion = schema.getVersion();
        this.eventId = eventId;
    }

    public Outcome getOutcome() throws ObjectNotFoundException, PersistencyException {
        if (eventId == NONE)
            throw new ObjectNotFoundException("No last eventId defined for path:" + itemPath + "/OUTCOME/" + schemaName + "/"
                    + schemaVersion + "/" + eventId);
        return (Outcome) Gateway.getStorage().get(itemPath,
                ClusterStorage.OUTCOME + "/" + schemaName + "/" + schemaVersion + "/" + eventId, null);
    }

    @Override
    public String getClusterType() {
        return ClusterStorage.VIEWPOINT;
    }

    public void setItemUUID(String uuid) throws InvalidItemPathException {
        setItemPath(new ItemPath(uuid));
    }

    public String getItemUUID() {
        return getItemPath().getUUID().toString();
    }

    public Event getEvent() throws InvalidDataException, PersistencyException, ObjectNotFoundException {
        if (eventId == NONE) throw new InvalidDataException("No last eventId defined");

        return (Event) Gateway.getStorage().get(itemPath, ClusterStorage.HISTORY + "/" + eventId, null);
    }

    @Override
    public String toString() {
        return name;
    }
}
