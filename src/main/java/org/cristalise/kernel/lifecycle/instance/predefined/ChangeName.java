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

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

public class ChangeName extends PredefinedStep {
    public static final String description = "Removes Items old Name, add the new Name and changes the Name property";

    public ChangeName() {
        super();
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectCannotBeUpdated, ObjectAlreadyExistsException, CannotManageException
    {
        String[] params = getDataList(requestData);

        if (params.length != 2) throw new InvalidDataException("ChangeName: Invalid parameters: "+Arrays.toString(params));

        String oldName = params[0];
        String newName = params[1];

        Logger.msg(3, "ChangeName - oldName:%s newName:%s", oldName, newName);

        if (oldName.equals(newName)) {
            Logger.msg(3, "ChangeName - oldName:%s == newName:%s - NOTHING DONE", oldName, newName);
            return requestData;
        }

        PagedResult result = Gateway.getLookup().searchAliases(item, 0, 100);
        DomainPath currentDP = null;

        if (result.rows.size() > 0) {
            for (Path path: result.rows) {
                if (path.getName().equals(oldName)) {
                    currentDP = (DomainPath)path;
                    break;
                }
            }

            if (currentDP == null) throw new InvalidDataException(item + " does not domainPath with name:" + oldName);
        }
        else
            throw new InvalidDataException(item + " does not have any domainPath");

        DomainPath rootDP = currentDP.getParent();
        DomainPath newDP = new DomainPath(rootDP, newName);
        newDP.setItemPath(item);

        // Throws an exception if newName exists
        Gateway.getLookupManager().add(newDP);

        try {
            Gateway.getLookupManager().delete(currentDP);
        }
        catch (Exception e) {
            Logger.error(e);

            //recover original state
            Gateway.getLookupManager().delete(newDP);

            throw new CannotManageException(e.getMessage());
        }

        try {
            WriteProperty.write(item, "Name", newName, locker);
        }
        catch (Exception e) {
            Logger.error(e);

            //recover original state
            Gateway.getLookupManager().delete(newDP);
            Gateway.getLookupManager().add(currentDP);

            throw new CannotManageException(e.getMessage());
        }

        return requestData;
    }
}
