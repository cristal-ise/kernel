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

import static org.cristalise.kernel.persistency.ClusterType.ATTACHMENT;
import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OutcomeAttachment implements C2KLocalObject {

    public static final int NONE = -1;

    // db fields
    ItemPath  itemPath;
    String    schemaName;
    int       schemaVersion;
    int       eventId;

    String type;
    byte[] binaryData;

    public OutcomeAttachment() {
        eventId = NONE;
        itemPath = null;
        schemaVersion = NONE;
        schemaName = null;

        type = null;
        binaryData = new byte[0];
    }

    public OutcomeAttachment(ItemPath itemPath, String schemaName, int schemaVersion, int eventId, String type, byte[] binaryData) {
        this.itemPath = itemPath;
        this.schemaName = schemaName;
        this.schemaVersion = schemaVersion;
        this.eventId = eventId;

        this.type = type;
        this.binaryData = binaryData;
    }

    public OutcomeAttachment(ItemPath itemPath, Outcome outcome, String type, byte[] binaryData) {
        this.itemPath = itemPath;
        this.schemaName = outcome.getSchema().getName();
        this.schemaVersion = outcome.getSchema().getVersion();
        this.eventId = outcome.getID();

        this.type = type;
        this.binaryData = binaryData;
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

    @Override
    public ClusterType getClusterType() {
        return ATTACHMENT;
    }

    @Override
    public String getClusterPath() {
        return getClusterType()+"/"+schemaName+"/"+schemaVersion+"/"+eventId;
    }

    public void setItemUUID(String uuid) throws InvalidItemPathException {
        setItemPath(new ItemPath(uuid));
    }

    public String getItemUUID() {
        return getItemPath().getUUID().toString();
    }

    public Event getEvent() throws InvalidDataException, PersistencyException, ObjectNotFoundException {
        if (eventId == NONE) throw new InvalidDataException("No eventId defined for path:"+getClusterPath());

        return (Event) Gateway.getStorage().get(itemPath, HISTORY + "/" + eventId, null);
    }

    @Override
    public void setName(String name) {
        try {
            eventId = Integer.valueOf(name);
        }
        catch (NumberFormatException e) {
            Logger.error("Invalid id set on Outcome:"+name);
        }
    }

    @Override
    public String getName() {
        return ""+eventId;
    }
}
