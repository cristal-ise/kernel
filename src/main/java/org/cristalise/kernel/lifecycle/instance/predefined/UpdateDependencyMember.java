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

import java.util.Map.Entry;

import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

/**
 * Params:
 * <ul>
 * <li>0: collection name</li>
 * <li>1: slot number</li>
 * <li>2: target UUID or DomainPath if slot number is -1</li>
 * <li>3: marshaled properties of member</li>
 * </ul>
 * @throws ObjectNotFoundException
 * @throws PersistencyException
 */
public class UpdateDependencyMember extends PredefinedStepCollectionBase {

    public static final String description = "";

    /**
     * Constructor for Castor
     */
    public UpdateDependencyMember() {
        super();
    }

    /**
     * 
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectNotFoundException, PersistencyException
    {
        unpackParamsAndGetCollection(item, requestData, locker);

        if (slotID == -1 && childPath == null) throw new InvalidDataException("Must give either slot number/item UUID to update member");
        if (memberNewProps == null)            throw new InvalidDataException("Must provide properties to update member");

        DependencyMember member = getDependencyMember();

        // Only update existing properties otherwise throw an exception
        for (Entry<String, Object> entry: memberNewProps.entrySet()) {
            if (member.getProperties().containsKey(entry.getKey())) {
                member.getProperties().put(entry.getKey(), entry.getValue());
            }
            else {
                String error = "Property "+entry.getKey()+" does not exists for slotID:" + slotID;
                Logger.error(error);
                throw new ObjectNotFoundException(error);
            }
        }

        Gateway.getStorage().put(item, collection, locker);

        return requestData;
    }
}
