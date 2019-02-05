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
package org.cristalise.kernel.property;

import static org.cristalise.kernel.process.resource.BuiltInResources.PROPERTY_DESC_RESOURCE;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;

import java.util.ArrayList;
import java.util.Iterator;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

public class PropertyUtility {
    static public String getValue(ArrayList<PropertyDescription> pdlist, String name) {
        for (PropertyDescription pd : pdlist) {
            if (name.equalsIgnoreCase(pd.getName())) return pd.getDefaultValue();
        }
        return null;
    }

    public static boolean propertyExists(ItemPath itemPath, String propName, Object locker) {
        try {
            String[] contents = Gateway.getStorage().getClusterContents(itemPath, ClusterType.PROPERTY);

            for (String name: contents) if(name.equals(propName)) return true;
        }
        catch (PersistencyException e) {
            Logger.error(e);
        }
        return false;
    }

    public static Property getProperty(ItemPath itemPath, BuiltInItemProperties prop, Object locker) throws ObjectNotFoundException {
        return getProperty(itemPath, prop.getName(), locker);
    }

    public static Property getProperty(ItemPath itemPath, String propName, Object locker) throws ObjectNotFoundException {
        try {
            return (Property)Gateway.getStorage().get(itemPath, ClusterType.PROPERTY+"/"+propName, locker);
        }
        catch (PersistencyException e) {
            Logger.error(e);
        }
        return null;
    }

    static public String getNames(ArrayList<PropertyDescription> pdlist) {
        StringBuffer names = new StringBuffer();

        for (PropertyDescription value : pdlist)
            names.append(value.getDefaultValue()).append(" ");

        return names.toString();
    }

    static public String getClassIdNames(ArrayList<PropertyDescription> pdlist) {
        StringBuffer names = new StringBuffer();

        for (Iterator<PropertyDescription> iter = pdlist.iterator(); iter.hasNext();) {
            PropertyDescription pd = iter.next();

            if (pd.getIsClassIdentifier()) names.append(pd.getName());
            if (iter.hasNext()) names.append(",");
        }
        return names.toString();
    }

    static public PropertyDescriptionList getPropertyDescriptionOutcome(ItemPath itemPath, String descVer, Object locker) throws ObjectNotFoundException {
        try {
            //the type of the Item is a PropertyDesc
            if (getProperty(itemPath, TYPE, locker).getValue().equals(PROPERTY_DESC_RESOURCE.getSchemaName())) {
                String name = getProperty(itemPath, NAME, locker).getValue();

                int version = 0;

                if ("last".equals(descVer)) {
                  //String[] versions = Gateway.getStorage().getClusterContents(itemPath, "", locker);
                }
                else 
                  version = Integer.parseInt(descVer);

                return LocalObjectLoader.getPropertyDescriptionList(name, version);
            }
            else  {
                Outcome outc = (Outcome) Gateway.getStorage().get(itemPath, ClusterType.VIEWPOINT+"/PropertyDescription/"+descVer+"/data", locker);
                return (PropertyDescriptionList) Gateway.getMarshaller().unmarshall(outc.getData());
            }
        }
        catch (Exception ex) {
            Logger.error(ex);
            throw new ObjectNotFoundException("Could not fetch PropertyDescription from " + itemPath);
        }
    }

    /**
     * Converts transitive PropertyDescriptions to VertexProprties (CastorHashMap). ClassIdentifiers are transitive by default.
     * 
     * @param pdList the PropertyDescriptions to be used
     * @return the initialised CastorHashMap
     */
    static public CastorHashMap convertTransitiveProperties(PropertyDescriptionList pdList) {
        CastorHashMap props = new CastorHashMap();

        for (int i = 0; i < pdList.list.size(); i++) {
            PropertyDescription pd = pdList.list.get(i);

            if (pd.isTransitive()) props.put(pd.getName(), pd.getDefaultValue());
        }
        return props;
    }
}
