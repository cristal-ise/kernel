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
package org.cristalise.kernel.lifecycle.routingHelpers;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.ItemPath;

public interface DataHelper {

    /**
     * Data helpers are a mechanism that allows easy referencing of different types of data 
     * within an Item, in order to use that data for process control or generating new data. 
     * They are referenced using a URI-like syntax, usually in workflow vertex properties.
     * Check wiki for more information.
     * 
     * @param itemPath the current item to be used
     * @param actContext the current Activity path in which the DataHelper is used
     * @param dataPath its content is implementation specidifc
     * @param locker the transaction locker object used for ClusterStorage methods
     * @return The resolved value
     * @throws InvalidDataException
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     */
    public String get(ItemPath itemPath, String actContext, String dataPath, Object locker) 
            throws InvalidDataException, PersistencyException, ObjectNotFoundException;
}
