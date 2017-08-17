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

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.Logger;

public class WriteProperty extends PredefinedStep {
    /**
     * Constructor for Castor
     */
    public WriteProperty() {
        super();
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectCannotBeUpdated, ObjectNotFoundException, PersistencyException
    {
        String[] params = getDataList(requestData);
        if (Logger.doLog(3))
            Logger.msg(3, "WriteProperty: called by " + agent + " on " + item + " with parameters " + Arrays.toString(params));

        if (params.length != 2)
            throw new InvalidDataException("WriteProperty: invalid parameters " + Arrays.toString(params));

        String name = params[0];
        String value = params[1];

        write(item, name, value, locker);

        return requestData;
    }

    public static void write(ItemPath item, String name, String value, Object locker)
            throws PersistencyException, ObjectCannotBeUpdated, ObjectNotFoundException
    {
        Property prop = (Property) Gateway.getStorage().get(item, ClusterType.PROPERTY + "/" + name, locker);

        if (!prop.isMutable() && !value.equals(prop.getValue()))
            throw new ObjectCannotBeUpdated("WriteProperty: Property '" + name + "' is not mutable.");

        prop.setValue(value);

        Gateway.getStorage().put(item, prop, locker);
    }
}
