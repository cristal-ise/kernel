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
package org.cristalise.kernel.collection;

/**
 * Instance of an Aggregation. Unlike in the description, Items may only be 
 * assigned to one slot.
 */
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.CastorHashMap;


public class AggregationInstance extends Aggregation {

    public AggregationInstance() {
        setName("AggregationInstance");
    }

    public AggregationInstance(String name) {
        setName(name);
    }

    public AggregationInstance(String name, Integer version) {
        setName(name);
        setVersion(version);
    }

    @Override
    public AggregationMember addMember(ItemPath itemPath, CastorHashMap props, String classProps)
        throws InvalidCollectionModification, ObjectAlreadyExistsException
    {
        if( itemPath != null && exists(itemPath))
            throw new ObjectAlreadyExistsException(itemPath+" already exists in this collection.");
        else
            return super.addMember(itemPath, props, classProps);
    }

    @Override
    public AggregationMember addMember(ItemPath itemPath, CastorHashMap props, String classProps, GraphPoint location, int w, int h)
        throws InvalidCollectionModification, ObjectAlreadyExistsException
    {
        if( itemPath != null && exists(itemPath))
            throw new ObjectAlreadyExistsException(itemPath+" already exists in this collection.");
        else
            return super.addMember(itemPath, props, classProps, location, w, h);
    }
}
