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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;

import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.utils.CastorHashMap;

/**
 * Collections are Item local objects that reference other Items.
 * 
 * <p>
 * In parallel with the OO meta-model, Items can be linked to other Items in different ways. These links are modelled with Collections,
 * which are local objects stored in an Item which reference a number of other Items in the same server. The Collections holds a
 * CollectionMember, sometimes known as a slot, to reference each Item and store additional information about the link.
 * 
 * <p>
 * Features:
 * <ul>
 * <li><b>Typing</b> - Collections can restrict membership of based on type information derived from Item, Property and Collection
 * descriptions. This restriction may be per-slot or apply to the whole Collection.
 * 
 * <li><b>Fixed or flexible slots</b> - The CollectionMember objects of a Collection may be empty, individually typed, or created and
 * removed as required, simulating either array, structures or lists.
 * 
 * <li><b>Layout</b> - Collections can include a {@link GraphModel} to lay out its slots on a two-dimensional canvas, for modelling real
 * world compositions.
 * </ul>
 * 
 * <p>
 * Collections are managed through predefined steps.
 */
abstract public class Collection<E extends CollectionMember> implements C2KLocalObject {

    public static final short         EMPTY    = -1;
    private int                       mCounter = -1;   // Contains next available Member ID
    protected CollectionMemberList<E> mMembers = new CollectionMemberList<E>();
    protected String                  mName    = "";   // Not checked for uniqueness
    protected Integer                 mVersion = null;

    /**
     * Fetch the current highest member ID of the collection. This is found by scanning all the current members and kept in the mCounter
     * field, but is not persistent.
     * 
     * @return the current highest member ID
     */
    public int getCounter() {
        if (mCounter == -1)
            for (E element : mMembers.list) {
            if (mCounter < element.getID())
                mCounter = element.getID();
            }
        return ++mCounter;
    }

    /**
     * @return The total number of slots in this collection, including empty ones
     */
    public int size() {
        return mMembers.list.size();
    }

    /**
     * Sets the collection name
     */
    @Override
    public void setName(String name) {
        mName = name;
    }

    /**
     * @return The collection's name
     */
    @Override
    public String getName() {
        return mName;
    }

    /**
     * Get the collection version. Null if not set, and will be stored as 'last'
     * 
     * @return Integer version
     */
    public Integer getVersion() {
        return mVersion;
    }

    /**
     * Set a named version for this collection. Must be an integer or null. Named versions will be stored separately to the current version
     * ('last') and should not change once saved.
     * 
     * @param version the version to set
     */
    public void setVersion(Integer version) {
        this.mVersion = version;
    }

    /**
     * Get the version name for storage, which is 'last' unless the version number is set.
     * 
     * @return String
     */
    public String getVersionName() {
        return mVersion == null ? "last" : String.valueOf(mVersion);
    }

    @Override
    public String getClusterType() {
        return ClusterStorage.COLLECTION;
    }

    public void setMembers(CollectionMemberList<E> newMembers) {
        mMembers = newMembers;
    }

    public boolean contains(ItemPath itemPath) {
        for (E element : mMembers.list) {
            if (element.getItemPath().equals(itemPath))
                return true;
        }
        return false;
    }

    /**
     * Gets the description version referenced by the given collection member. Assumes 'last' if version not given.
     * 
     * @param mem
     *            The member in question
     * @return String version tag
     */
    public String getDescVer(E mem) {
        String descVer = "last";
        Object descVerObj = mem.getProperties().getBuiltInProperty(VERSION);
        if (descVerObj != null) descVer = descVerObj.toString();
        return descVer;
    }

    /**
     * Check if all slots have an assigned Item
     * 
     * @return boolean
     */
    public boolean isFull() {
        for (E element : mMembers.list) {
            if (element.getItemPath() == null)
                return false;
        }
        return true;
    }

    /**
     * Find collection member by integer ID
     * 
     * @param memberId
     *            to find
     * @return the CollectionMember with that ID
     * @throws ObjectNotFoundException
     *             when the ID wasn't found
     */
    public E getMember(int memberId) throws ObjectNotFoundException {
        for (E element : mMembers.list) {
            if (element.getID() == memberId)
                return element;
        }
        throw new ObjectNotFoundException("Member " + memberId + " not found in " + mName);
    }

    public CollectionMemberList<E> getMembers() {
        return mMembers;
    }

    /**
     * Add a member to this collection, with the given property and class properties and optionally an Item to assign, which may be null if
     * the collection allows empty slots.
     * 
     * @param itemPath
     *            the Item to assign to the new slot. Optional for collections that allow empty slots
     * @param props
     *            the Properties of the new member
     * @param classProps
     *            the names of the properties that dictate the type of assigned Items.
     * @return the new CollectionMember instance
     * @throws InvalidCollectionModification
     *             when the assignment was invalid because of collection constraints, such as global type constraints, or not allowing empty
     *             slots.
     * @throws ObjectAlreadyExistsException
     *             some collections don't allow multiple slots assigned to the same Item, and throw this Exception if it is attempted
     */
    public abstract E addMember(ItemPath itemPath, CastorHashMap props, String classProps)
            throws InvalidCollectionModification, ObjectAlreadyExistsException;

    /**
     * Removes the slot with the given ID from the collection.
     * 
     * @param memberId
     *            to remove
     * @throws ObjectNotFoundException
     *             when there was no slot with this ID found.
     */
    public abstract void removeMember(int memberId) throws ObjectNotFoundException;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((mMembers == null) ? 0 : mMembers.hashCode());
        result = prime * result + ((mName == null) ? 0 : mName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Collection<?> other = (Collection<?>) obj;
        if (mMembers == null) {
            if (other.mMembers != null)
                return false;
        }
        else if (!mMembers.equals(other.mMembers))
            return false;
        if (mName == null) {
            if (other.mName != null)
                return false;
        }
        else if (!mName.equals(other.mName))
            return false;
        return true;
    }
}
