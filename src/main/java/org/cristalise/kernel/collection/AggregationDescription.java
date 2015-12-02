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
 * The description of a Collection with a graph layout. Each slot is 
 * instantiated empty in the resulting Aggregation, with ClassProps taken from
 * the PropertyDescription outcome of the description slot's referenced Item.
 */

import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.property.PropertyUtility;
import org.cristalise.kernel.utils.Logger;


public class AggregationDescription extends Aggregation implements CollectionDescription<AggregationMember>
{

    public AggregationDescription()
    {
    	setName("AggregationDescription");
    }

    public AggregationDescription(String name)
    {
    	setName(name);
    }

    @Override
	public Aggregation newInstance() throws ObjectNotFoundException
    {
    	AggregationInstance newInstance = new AggregationInstance(getName());
    	//for each desc member
    	for (int i=0; i<size(); i++)
    	{
    		AggregationMember mem = mMembers.list.get(i);
			//get the propdesc of the member item and look for an explicit version
    		String descVer = getDescVer(mem);
			PropertyDescriptionList pdList = PropertyUtility.getPropertyDescriptionOutcome(mem.getItemPath(), descVer, null);
			if (pdList!=null)
			{
				//create the new props of the member object
                try {
                    Vertex v = getLayout().getVertexById(mem.getID());
                    newInstance.addMember(null, PropertyUtility.createProperty(pdList), pdList.getClassProps(),v.getCentrePoint(),v.getWidth(),v.getHeight());
                } catch (InvalidCollectionModification e) { 
                } catch (ObjectAlreadyExistsException e) { }
			}
			else
			{
				Logger.error("AggregationDescription::newInstance() There is no PropertyDescription. Cannot instantiate. " + mem.getItemPath());
				return null;
			}


    	}

    	return newInstance;
    }
}
