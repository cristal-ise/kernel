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

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;


public class WriteViewpoint extends PredefinedStep {

    public WriteViewpoint() {
        super();
    }

    /**
     * SchemaName, name and event Id. Event and Outcome should be checked so schema version should be discovered.
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectNotFoundException, PersistencyException
    {
        String[] params = getDataList(requestData);
        if (Logger.doLog(3)) Logger.msg(3, "WriteViewpoint: called by "+agent+" on "+item+" with parameters "+Arrays.toString(params));

        if (params.length != 3) {
            throw new InvalidDataException("WriteViewpoint: Invalid parameters "+Arrays.toString(params));
        }

        String schemaName = params[0];
        String viewName   = params[1];
        int eventId;

        try {
            eventId = Integer.parseInt(params[2]);
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("WriteViewpoint: Parameter 3 (EventId) must be an integer");
        }

        write(item, schemaName, viewName, eventId, locker);

        return requestData;
    }

    public static void write(ItemPath item, String schemaName, String viewName, int eventId, Object locker)
            throws PersistencyException, ObjectNotFoundException, InvalidDataException
    {
        Event event = (Event)Gateway.getStorage().get(item, ClusterType.HISTORY+"/"+eventId, locker);

        if (StringUtils.isBlank(event.getSchemaName())) {
            throw new InvalidDataException("Event "+eventId+" does not reference an Outcome, so cannot be assigned to a Viewpoint.");
        }

        //checks Schema name/version
        Schema thisSchema = LocalObjectLoader.getSchema(schemaName, event.getSchemaVersion());

        if (!event.getSchemaName().equals(thisSchema.getItemID())) { 
            throw new InvalidDataException("Event outcome schema is "+event.getSchemaName()+", and cannot be used for a "+schemaName+" Viewpoint");
        }

        Gateway.getStorage().put(item, new Viewpoint(item, thisSchema, viewName, eventId), locker);
    }
}
