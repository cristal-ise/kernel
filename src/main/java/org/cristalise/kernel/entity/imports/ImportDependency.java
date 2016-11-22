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
package org.cristalise.kernel.entity.imports;

import java.util.ArrayList;

import org.cristalise.kernel.collection.BuiltInCollections;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyDescription;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.property.PropertyDescription;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.property.PropertyUtility;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;

public class ImportDependency {

    public String                            name;
    public Integer                           version;
    public boolean                           isDescription;
    public String                            itemDescriptionPath;
    public String                            itemDescriptionVersion = null;
    public ArrayList<ImportDependencyMember> dependencyMemberList   = new ArrayList<ImportDependencyMember>();
    public CastorHashMap                     props                  = new CastorHashMap();

    public ImportDependency() {}

    public ImportDependency(BuiltInCollections collection) {
        this(collection.getName());
    }

    public ImportDependency(String name) {
        this();
        this.name = name;
    }

    public KeyValuePair[] getKeyValuePairs() {
        return props.getKeyValuePairs();
    }

    public void setKeyValuePairs(KeyValuePair[] pairs) {
        props.setKeyValuePairs(pairs);
    }

    public Dependency create() throws InvalidCollectionModification, ObjectNotFoundException, ObjectAlreadyExistsException {
        Dependency newDep = isDescription ? new DependencyDescription(name) : new Dependency(name);
        if (version != null) newDep.setVersion(version);
        if (itemDescriptionPath != null && itemDescriptionPath.length() > 0) {
            ItemPath itemPath;
            try {
                itemPath = new ItemPath(itemDescriptionPath);
            }
            catch (InvalidItemPathException ex) {
                itemPath = new DomainPath(itemDescriptionPath).getItemPath();
            }
            String descVer = itemDescriptionVersion == null ? "last" : itemDescriptionVersion;
            PropertyDescriptionList propList = PropertyUtility.getPropertyDescriptionOutcome(itemPath, descVer, null);
            StringBuffer classProps = new StringBuffer();
            for (PropertyDescription pd : propList.list) {
                props.put(pd.getName(), pd.getDefaultValue());
                if (pd.getIsClassIdentifier()) classProps.append((classProps.length() > 0 ? "," : "")).append(pd.getName());
            }
            newDep.setProperties(props);
            newDep.setClassProps(classProps.toString());
        }

        for (ImportDependencyMember thisMem : dependencyMemberList) {
            ItemPath itemPath;
            try {
                itemPath = new ItemPath(thisMem.itemPath);
            }
            catch (InvalidItemPathException ex) {
                itemPath = new DomainPath(thisMem.itemPath).getItemPath();
            }

            org.cristalise.kernel.collection.DependencyMember newDepMem = newDep.addMember(itemPath);
            newDepMem.getProperties().putAll(thisMem.props);
        }
        return newDep;
    }

}
