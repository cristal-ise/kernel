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


import java.util.ArrayList;
import java.util.List;

import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.Logger;


/**
 * A Collection implementation that contains a variable number of members of the
 * same type, like a variable-length array. CollectionMembers are created and 
 * destroyed as needed. A Dependency never contains empty slots, nor duplicated
 * members.
 * 
 * <p>ClassProps are stored at the collection level and duplicated in each slot.
 * Slots may still have their own individual properties annotating their link.
 * 
 * Predefined steps managing Dependencies:
 * 
 * <ul>
 * <li>
 */
public class Dependency extends Collection<DependencyMember>
{

    protected CastorHashMap mProperties = new CastorHashMap();
    protected String mClassProps = "";

    public Dependency()
    {
    	setName("Dependency");
    }

    public Dependency(String name)
    {
    	setName(name);
    }

    public Dependency(BuiltInCollections collection)
    {
        setName(collection.getName());
    }

    public CastorHashMap getProperties() {
        return mProperties;
    }

    public void setProperties(CastorHashMap props) {
        mProperties = props;
    }

    public KeyValuePair[] getKeyValuePairs()
    {
        return mProperties.getKeyValuePairs();
    }
    public void setKeyValuePairs(KeyValuePair[] pairs)
    {
        mProperties.setKeyValuePairs(pairs);
    }

    public void setClassProps(String classProps) {
        this.mClassProps = classProps;
    }

    public String getClassProps() {
        return mClassProps;
    }

	public DependencyMember addMember(ItemPath itemPath) throws InvalidCollectionModification, ObjectAlreadyExistsException {
    	if (contains(itemPath)) throw new ObjectAlreadyExistsException("Item "+itemPath+" already exists in Dependency "+getName());
    	if (itemPath == null) throw new InvalidCollectionModification("Cannot add empty slot to Dependency collection");
        // create member object
        DependencyMember depMember = new DependencyMember();
        depMember.setID(getCounter());
        depMember.setProperties((CastorHashMap)mProperties.clone());
        depMember.setClassProps(mClassProps);

        // assign entity
        depMember.assignItem(itemPath);
        mMembers.list.add(depMember);
        Logger.msg(8, "Dependency::addMember(" + itemPath + ") added to children.");
        return depMember;
    }
	
	/**
	 * Returns all ItemPaths that are members of the other collection but not members of this one.
	 * 
	 * @param other - The collection to compare
	 * @return List of ItemPaths
	 */
	public List<ItemPath> compare(Dependency other) {
		ArrayList<ItemPath> newMembers = new ArrayList<ItemPath>();
		for (DependencyMember thisMember : other.getMembers().list) {
			ItemPath thisPath = thisMember.getItemPath();
			if (!contains(thisPath)) {
				newMembers.add(thisPath);
			}
		}
		return newMembers;
	}

    @Override
	public DependencyMember addMember(ItemPath itemPath, CastorHashMap props, String classProps)
        throws InvalidCollectionModification, ObjectAlreadyExistsException
    {
    	if (contains(itemPath)) throw new ObjectAlreadyExistsException("Item "+itemPath+" already exists in Dependency "+getName());
        if (classProps != null && !classProps.equals(mClassProps))
            throw new InvalidCollectionModification("Cannot change classProps in dependency member");
        DependencyMember depMember = new DependencyMember();
        depMember.setID(getCounter());

        // merge props
        CastorHashMap newProps = new CastorHashMap();
        for (Object name : props.keySet()) {
            String key = (String)name;
            newProps.put(key, props.get(key));

        }
        // class props override local
        for (Object name : mProperties.keySet()) {
            String key = (String)name;
            newProps.put(key, mProperties.get(key));

        }
        depMember.setProperties(newProps);
        depMember.setClassProps(mClassProps);

        // assign entity
        depMember.assignItem(itemPath);
        mMembers.list.add(depMember);
        Logger.msg(8, "Dependency::addMember(" + itemPath + ") added to children.");
        return depMember;
    }

    @Override
	public void removeMember(int memberId) throws ObjectNotFoundException {
        for (DependencyMember element : mMembers.list) {
            if (element.getID() == memberId) {
                mMembers.list.remove(element);
                return;
            }
        }
        throw new ObjectNotFoundException("Member "+memberId+" not found");
    }

}
