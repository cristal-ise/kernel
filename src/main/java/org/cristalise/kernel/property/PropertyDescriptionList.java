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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.utils.CastorArrayList;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;

import lombok.Getter;
import lombok.Setter;


@Getter @Setter
public class PropertyDescriptionList extends CastorArrayList<PropertyDescription> implements DescriptionObject {
    String   name;
    Integer  version;
    ItemPath itemPath;

    public PropertyDescriptionList() {
        super();
    }

    public PropertyDescriptionList(String name, Integer version) {
        super();
        this.name = name;
        this.version = version;
    }

    public PropertyDescriptionList(ArrayList<PropertyDescription> aList) {
        super(aList);
    }

    public PropertyDescriptionList(String name, Integer version, ArrayList<PropertyDescription> aList) {
        super(aList);
        this.name = name;
        this.version = version;
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

    public void add(String name, String value, boolean isClassId, boolean isMutable, boolean isTransitive) {
        for (PropertyDescription element : list) {
            if (element.getName().equals(name)) {
                list.remove(element);
                break;
            }
        }
        list.add(new PropertyDescription(name, value, isClassId, isMutable, isTransitive));
    }

    public void add(String name, String value, boolean isClassId, boolean isMutable) {
        add(name, value, isClassId, isMutable, false);
    }

    public boolean definesProperty(String name) {
        for (PropertyDescription element : list) {
            if (element.getName().equals(name)) return true;
        }
        return false;
    }

    /**
     * Before instantiating checks that supplied initial Properties exist in description list 
     * 
     * @param initProps initial list of Properties
     * @return instantiated PropertyArrayList for Item
     * @throws InvalidDataException data was inconsistent
     */
    public PropertyArrayList instantiate(PropertyArrayList initProps) throws InvalidDataException {
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

            if (validatedInitProps.containsKey(propName)) propVal = validatedInitProps.get(propName);

            propInst.list.add( new Property(propName, propVal, pd.getIsMutable()));
        }
        return propInst;
    }

    @Override
    public String getItemID() {
        return (itemPath != null) ? itemPath.getUUID().toString() : null;
    }

    @Override
    public CollectionArrayList makeDescCollections() throws InvalidDataException, ObjectNotFoundException {
        return new CollectionArrayList();
    }

    @Override
    public void export(Writer imports, File dir, boolean shallow) throws InvalidDataException, ObjectNotFoundException, IOException {
        String xml;
        String typeCode = BuiltInResources.PROPERTY_DESC_RESOURCE.getTypeCode();
        String fileName = getName() + (getVersion() == null ? "" : "_" + getVersion()) + ".xml";

        try {
            xml = Gateway.getMarshaller().marshall(this);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new InvalidDataException("Couldn't marshall PropertyDescriptionList name:" + getName());
        }

        FileStringUtility.string2File(new File(new File(dir, typeCode), fileName), xml);

        if (imports == null) return;

        if (Gateway.getProperties().getBoolean("Resource.useOldImportFormat", false)) {
            imports.write("<Resource "
                    + "name='" + getName() + "' "
                    + (getItemPath() == null ? "" : "id='"      + getItemID()  + "' ")
                    + (getVersion()  == null ? "" : "version='" + getVersion() + "' ")
                    + "type='" + typeCode + "'>boot/" + typeCode + "/" + fileName
                    + "</Resource>\n");
        }
        else {
            imports.write("<PropertyDescriptionResource "
                    + "name='" + getName() + "' "
                    + (getItemPath() == null ? "" : "id='"      + getItemID()  + "' ")
                    + (getVersion()  == null ? "" : "version='" + getVersion() + "'")
                    + "/>\n");
        }
    }
}
