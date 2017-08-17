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

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;

import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;

import lombok.Getter;
import lombok.Setter;

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

    public Viewpoint(ItemPath itemPath, String schemaName, String name, int schemaVersion, int eventId) {
        setName(name);
        this.itemPath = itemPath;
        this.schemaName = schemaName;
        this.schemaVersion = schemaVersion;
        this.eventId = eventId;
    }

    public Viewpoint(ItemPath itemPath, Schema schema, String name, int eventId) {
        setName(name);
        this.itemPath = itemPath;
        this.schemaName = schema.getName();
        this.schemaVersion = schema.getVersion();
        this.eventId = eventId;
    }

    public Outcome getOutcome() throws ObjectNotFoundException, PersistencyException {
        return getOutcome(null);
    }

    public Outcome getOutcome(Object locker) throws ObjectNotFoundException, PersistencyException {
        if (eventId == NONE) throw new ObjectNotFoundException("No last eventId defined for path:"+getClusterPath());

        return (Outcome) Gateway.getStorage().get(
                itemPath, 
                OUTCOME + "/" + schemaName + "/" + schemaVersion + "/" + eventId, 
                locker);
    }

    public void setName(String n) {
        if (!StringUtils.isAlphanumeric(n))
            throw new IllegalArgumentException("Viewpoint name='"+n+"' must be alphanumeric only");

        name = n;
    }

    @Override
    public ClusterType getClusterType() {
        return VIEWPOINT;
    }

    @Override
    public String getClusterPath() {
        return getClusterType()+"/"+schemaName+"/"+name;
    }

    public void setItemUUID(String uuid) throws InvalidItemPathException {
        setItemPath(new ItemPath(uuid));
    }

    public String getItemUUID() {
        return getItemPath().getUUID().toString();
    }

    public Event getEvent() throws InvalidDataException, PersistencyException, ObjectNotFoundException {
        if (eventId == NONE) throw new InvalidDataException("No last eventId defined for path:"+getClusterPath());

        return (Event) Gateway.getStorage().get(itemPath, HISTORY + "/" + eventId, null);
    }

    @Override
    public String toString() {
        return name;
    }
}
