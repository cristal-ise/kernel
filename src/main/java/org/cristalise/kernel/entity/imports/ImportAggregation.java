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
package org.cristalise.kernel.entity.imports;

import java.util.ArrayList;

import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.AggregationDescription;
import org.cristalise.kernel.collection.AggregationInstance;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.property.PropertyDescription;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.property.PropertyUtility;


public class ImportAggregation {

    public boolean isDescription;
    public Integer version;
    public ArrayList<ImportAggregationMember> aggregationMemberList = new ArrayList<ImportAggregationMember>();
    public String name;

    public ImportAggregation() {
        super();
    }

    public ImportAggregation(String name, boolean isDescription) {
        this();
        this.name = name;
        this.isDescription = isDescription;
    }

	public org.cristalise.kernel.collection.Aggregation create() throws InvalidCollectionModification, ObjectNotFoundException, ObjectAlreadyExistsException {
        Aggregation newAgg = isDescription?new AggregationDescription(name):new AggregationInstance(name);
        if (version!= null) newAgg.setVersion(version);
        for (ImportAggregationMember thisMem : aggregationMemberList) {
            StringBuffer classProps = new StringBuffer();
            if (thisMem.itemDescriptionPath != null && thisMem.itemDescriptionPath.length()>0) {
            	ItemPath itemPath;
            	try {
            		itemPath = new ItemPath(thisMem.itemDescriptionPath);
            	} catch (InvalidItemPathException ex) {
            		itemPath = new DomainPath(thisMem.itemDescriptionPath).getItemPath();
            	}
            	
            	 String descVer = thisMem.itemDescriptionVersion==null?"last":thisMem.itemDescriptionVersion;
                 PropertyDescriptionList propList = PropertyUtility.getPropertyDescriptionOutcome(itemPath, descVer);
                 for (PropertyDescription pd : propList.list) {
					thisMem.props.put(pd.getName(), pd.getDefaultValue());
					if (pd.getIsClassIdentifier())
						classProps.append((classProps.length()>0?",":"")).append(pd.getName());
				}
             }
            ItemPath itemPath = null;
        	if (thisMem.itemPath != null && thisMem.itemPath.length()>0) {
        		
            	try {
            		itemPath = new ItemPath(thisMem.itemPath);
            	} catch (InvalidItemPathException ex) {
            		itemPath = new DomainPath(thisMem.itemPath).getItemPath();
            	}
        	}
        	newAgg.addMember(itemPath, thisMem.props, classProps.toString(), new GraphPoint(thisMem.geometry.x, thisMem.geometry.y), thisMem.geometry.width, thisMem.geometry.height);
        }
        return newAgg;
    }
}
