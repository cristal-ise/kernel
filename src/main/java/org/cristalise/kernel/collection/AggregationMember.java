/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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

import java.util.StringTokenizer;

import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.Logger;


/**
* A CollectionMember, or slot, of an Aggregation instance or description. 
* Verifies type information of Items during assignment based on 
* PropertyDescription information stored in slot properties and listed as 
* ClassProps.
*/

public class AggregationMember extends GraphableVertex implements CollectionMember
{

    private ItemPath             mItemPath   = null;
    private ItemProxy            mItem       = null;
    private Aggregation          mCollection = null;
	private String 			     mClassProps = null;
	private String 				 mItemName   = null;


   /**************************************************************************
    *
    **************************************************************************/
    public AggregationMember()
    {
        super();
    }

    public void setCollection(Aggregation aggregation)
    {
        mCollection = aggregation;
    }

	public void setClassProps(String props)
    {
        mClassProps = props;
    }

    @Override
	public ItemPath getItemPath()
    {
        return mItemPath;
    }

    public Aggregation getCollection()
    {
        return mCollection;
    }

    @Override
	public String getClassProps()
    {
        return mClassProps;
    }

    @Override
	public void assignItem(ItemPath itemPath) throws InvalidCollectionModification
    {
        if (itemPath != null) {
            if (mClassProps == null || getProperties() == null)
                throw new InvalidCollectionModification("ClassProps not yet set. Cannot check membership validity.");

            //for each mandatory prop check if its in the member property and has the matching value
            StringTokenizer sub = new StringTokenizer(mClassProps, ",");
            while (sub.hasMoreTokens())
            {
                String aClassProp = sub.nextToken();
                try {
                    String memberValue = (String)getProperties().get(aClassProp);
                    Property ItemProperty = (Property)Gateway.getStorage().get(itemPath, ClusterStorage.PROPERTY+"/"+aClassProp, null);
                    if (ItemProperty == null)
                        throw new InvalidCollectionModification("Property "+aClassProp+ " does not exist for item " + itemPath );
                    if (ItemProperty.getValue() == null || !ItemProperty.getValue().equalsIgnoreCase(memberValue))
                        throw new InvalidCollectionModification("Value of mandatory prop "+aClassProp+" does not match: " + ItemProperty.getValue()+"!="+memberValue);
                 }
                catch (InvalidCollectionModification ex) {
                    throw ex;
                }
                catch (Exception ex)
                {
                    Logger.error(ex);
                    throw new InvalidCollectionModification("Error checking properties");
                }
            }
        }

        mItemPath = itemPath;
        mItem       = null;
        mItemName   = null;
    }

    @Override
	public void clearItem() {
        mItemPath   = null;
        mItem       = null;
        mItemName   = null;
    }

    @Override
	public ItemProxy resolveItem() throws ObjectNotFoundException {
        if (mItem == null && mItemPath != null) {
        	mItem = Gateway.getProxyManager().getProxy(mItemPath);
        }
        return mItem;

    }

    public String getItemName() {
    	if (mItemName == null) {
    		if (mItemPath != null) {
        		try {
        			mItemName = resolveItem().getName();
        		} catch (ObjectNotFoundException ex) {
        			Logger.error(ex);
        			mItemName = "Error ("+mItemPath+")";
        		}
    		}
    		else
    			mItemName = "Empty";
    	}

    	return mItemName;
    }

	public void setChildUUID(String uuid) throws InvalidCollectionModification, InvalidItemPathException {
		mItemPath = new ItemPath(uuid);
		mItemName = null;
	}


	@Override
	public String getChildUUID() {
		if (getItemPath() == null) return null;
		return getItemPath().getUUID().toString();
	}

}
