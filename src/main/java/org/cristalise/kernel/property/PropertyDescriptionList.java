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

import java.util.ArrayList;
import java.util.HashMap;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.utils.CastorArrayList;


public class PropertyDescriptionList extends CastorArrayList<PropertyDescription>
{
    public PropertyDescriptionList()
    {
        super();
    }

    public PropertyDescriptionList(ArrayList<PropertyDescription> aList)
    {
        super(aList);
    }

    public String getClassProps() {
        StringBuffer props = new StringBuffer();
        for (PropertyDescription element : list) {
            if (element.getIsClassIdentifier()) {
                if (props.length()>0)
                    props.append(",");
                props.append(element.getName());
            }
        }
        return props.toString();
    }

    public boolean setDefaultValue(String name, String value) {
        for (PropertyDescription element : list) {
            if (element.getName().equals(name)) {
                element.setDefaultValue(value);
                return true;
            }
        }
        return false;
    }

    public void add(String name, String value, boolean isClassId, boolean isMutable) {
        for (PropertyDescription element : list) {
            if (element.getName().equals(name)) {
                list.remove(element);
                break;
            }
        }
        list.add(new PropertyDescription(name, value, isClassId, isMutable));
    }

    public boolean definesProperty(String name) {
        for (PropertyDescription element : list) {
            if (element.getName().equals(name))
                return true;
        }
        return false;
    }

    public PropertyArrayList instantiate(PropertyArrayList initProps) throws InvalidDataException {
        // check that supplied init properties exist in desc list
        HashMap<String, String> validatedInitProps = new HashMap<String, String>();
        for (Property initProp : initProps.list) {
            if (!definesProperty(initProp.getName()))
                throw new InvalidDataException("Property "+initProp.getName()+" has not been declared in the property descriptions");
            else
                validatedInitProps.put(initProp.getName(), initProp.getValue());
        }

        PropertyArrayList propInst = new PropertyArrayList();
        for (int i = 0; i < list.size(); i++) {
            PropertyDescription pd = list.get(i);
            String propName = pd.getName();
            String propVal = pd.getDefaultValue();
            if (validatedInitProps.containsKey(propName))
                propVal = validatedInitProps.get(propName);
            boolean isMutable = pd.getIsMutable();
            propInst.list.add( new Property(propName, propVal, isMutable));
        }
        return propInst;
    }
}
