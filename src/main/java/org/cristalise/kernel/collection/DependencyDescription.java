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

import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.property.PropertyUtility;
import org.cristalise.kernel.utils.CastorHashMap;


public class DependencyDescription extends Dependency implements CollectionDescription<DependencyMember>{
	
    public DependencyDescription()
    {
    	setName("DependencyDescription");
    }
	
    public DependencyDescription(String name)
    {
    	setName(name);
    }
    
	@Override
	public Collection<DependencyMember> newInstance() throws ObjectNotFoundException{
		String depName = getName().replaceFirst("\'$", ""); // HACK: Knock the special 'prime' off the end for the case of descriptions of descriptions
		Dependency newDep = new Dependency(depName);
		if (mMembers.list.size() == 1) { // constrain the members based on the property description
			DependencyMember mem = mMembers.list.get(0);
			String descVer = getDescVer(mem);
			PropertyDescriptionList pdList = PropertyUtility.getPropertyDescriptionOutcome(mem.getItemPath(), descVer, null);
			if (pdList!=null) {
				newDep.setProperties(PropertyUtility.createProperty(pdList));
				newDep.setClassProps(pdList.getClassProps());
			}
		}
		return newDep;
	}

	
	@Override
	public DependencyMember addMember(ItemPath itemPath) throws InvalidCollectionModification, ObjectAlreadyExistsException {
		checkMembership();
		return super.addMember(itemPath);
	}
	
	@Override
	public DependencyMember addMember(ItemPath itemPath, CastorHashMap props, String classProps)
			throws InvalidCollectionModification, ObjectAlreadyExistsException {
		checkMembership();
		return super.addMember(itemPath, props, classProps);
	}
	
	public void checkMembership() throws InvalidCollectionModification {
		if (mMembers.list.size() > 0)
			throw new InvalidCollectionModification("Dependency descriptions may not have more than one member.");
	}

}
