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

import java.util.StringTokenizer;

import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.Logger;


/**
 * 
 */
public class DependencyMember implements CollectionMember {
    private ItemPath      mItemPath   = null;
    private ItemProxy     mItem       = null;
    private int           mId         = -1;
    private CastorHashMap mProperties = null;
    private String        mClassProps;


    /**************************************************************************
     *
     **************************************************************************/
    public DependencyMember()
    {
        mProperties = new CastorHashMap();
    }

    @Override
    public ItemPath getItemPath()
    {
        return mItemPath;
    }

    public void setProperties(CastorHashMap props)
    {
        mProperties = props;
    }

    @Override
    public CastorHashMap getProperties()
    {
        return mProperties;
    }

    public KeyValuePair[] getKeyValuePairs()
    {
        return mProperties.getKeyValuePairs();
    }
    public void setKeyValuePairs(KeyValuePair[] pairs)
    {
        mProperties.setKeyValuePairs(pairs);
    }

    @Override
    public int getID() {
        return mId;
    }

    public void setID(int id) {
        mId = id;
    }

    public void setClassProps(String props)
    {
        mClassProps = props;
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
                    if (!ItemProperty.getValue().equalsIgnoreCase(memberValue))
                        throw new InvalidCollectionModification("DependencyMember::checkProperty() Values of mandatory prop "+aClassProp+" do not match " + ItemProperty.getValue()+"!="+memberValue);
                }
                catch (Exception ex)
                {
                    Logger.error(ex);
                    throw new InvalidCollectionModification("Error checking properties");
                }
            }
        }

        mItemPath = itemPath;
        mItem = null;
    }

    @Override
    public void clearItem() {
        mItemPath = null;
        mItem = null;
    }

    @Override
    public ItemProxy resolveItem() throws ObjectNotFoundException {
        if (mItem == null && mItemPath != null)
            mItem = Gateway.getProxyManager().getProxy(mItemPath);
        return mItem;
    }

    public void setChildUUID(String uuid) throws InvalidCollectionModification, InvalidItemPathException {
        mItemPath = new ItemPath(uuid);
    }


    @Override
    public String getChildUUID() {
        return mItemPath.getUUID().toString();
    }

    public Object getBuiltInProperty(BuiltInVertexProperties prop) {
        return mProperties.get(prop.getAlternativeName());
    }

    public void setBuiltInProperty(BuiltInVertexProperties prop, Object val) {
        mProperties.put(prop.getAlternativeName(), val);
    }
}
