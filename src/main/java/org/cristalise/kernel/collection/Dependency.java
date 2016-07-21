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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCRIPT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCRIPT_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;

import java.util.ArrayList;
import java.util.List;

import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

/**
 * A Collection implementation that contains a variable number of members of the
 * same type, like a variable-length array. CollectionMembers are created and 
 * destroyed as needed. A Dependency never contains empty slots, nor duplicated
 * members.
 * 
 * <p>ClassProps are stored at the collection level and duplicated in each slot.
 * Slots may still have their own individual properties annotating their link.
 */
public class Dependency extends Collection<DependencyMember> {

    protected CastorHashMap mProperties = new CastorHashMap();
    protected String mClassProps = "";

    public Dependency() {
        setName("Dependency");
    }

    public Dependency(String name) {
        setName(name);
    }

    public Dependency(BuiltInCollections collection) {
        setName(collection.getName());
    }

    public CastorHashMap getProperties() {
        return mProperties;
    }

    public void setProperties(CastorHashMap props) {
        mProperties = props;
    }

    public KeyValuePair[] getKeyValuePairs() {
        return mProperties.getKeyValuePairs();
    }

    public void setKeyValuePairs(KeyValuePair[] pairs) {
        mProperties.setKeyValuePairs(pairs);
    }

    public void setClassProps(String classProps) {
        this.mClassProps = classProps;
    }

    public String getClassProps() {
        return mClassProps;
    }

    /**
     * 
     * @param itemPath
     * @return DependencyMember
     * @throws InvalidCollectionModification
     * @throws ObjectAlreadyExistsException
     */
    public DependencyMember addMember(ItemPath itemPath) throws InvalidCollectionModification, ObjectAlreadyExistsException {
        if (itemPath == null)   throw new InvalidCollectionModification("Cannot add empty slot to Dependency collection");
        if (contains(itemPath)) throw new ObjectAlreadyExistsException("Item "+itemPath+" already exists in Dependency "+getName());

        // create member object
        DependencyMember depMember = new DependencyMember();
        depMember.setID(getCounter());
        depMember.setProperties((CastorHashMap)mProperties.clone());
        depMember.setClassProps(mClassProps);

        // assign entity
        depMember.assignItem(itemPath);
        mMembers.list.add(depMember);
        
        Logger.msg(8, "Dependency.addMember(" + itemPath + ") added to children.");
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

    /**
     * 
     */
    @Override
    public DependencyMember addMember(ItemPath itemPath, CastorHashMap props, String classProps)
            throws InvalidCollectionModification, ObjectAlreadyExistsException
    {
        if (itemPath == null) throw new InvalidCollectionModification("Cannot add empty slot to Dependency collection");

        if (contains(itemPath))
            throw new ObjectAlreadyExistsException("Item "+itemPath+" already exists in Dependency "+getName());

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
        Logger.msg(8, "Dependency.addMember(" + itemPath + ") added to children.");
        return depMember;
    }

    /**
     * 
     */
    @Override
    public void removeMember(int memberId) throws ObjectNotFoundException {
        for (DependencyMember element : mMembers.list) {
            if (element.getID() == memberId) {
                mMembers.list.remove(element);
                return;
            }
        }
        throw new ObjectNotFoundException("Collection name:"+getName()+" does not contains Member id:"+memberId);
    }

    /**
     * 
     * @param props
     */
    public void addToItemProperties(PropertyArrayList props) throws InvalidDataException, ObjectNotFoundException {
        Logger.msg(2, "Dependency.addToItemProperties("+getName()+") - itemPath:" + "");

        BuiltInCollections coll = BuiltInCollections.getValue(getName());

        for (DependencyMember member : getMembers().list) {
            String memberUUID = member.getChildUUID();
            Integer memberVer = LocalObjectLoader.deriveVersionNumber(member.getBuiltInProperty(VERSION));

            if (memberVer == null) {
                throw new InvalidDataException("Version is null for Collection:" + getName() + ", DependencyMember:" + memberUUID);
            }

            if (coll != null) {
                Logger.msg(5, "Dependency.addToItemProperties() - BuiltIn Dependency:"+getName()+" memberUUID:"+memberUUID);

                switch (coll) {
                    case SCHEMA:
                        LocalObjectLoader.getSchema(memberUUID, memberVer);

                        props.put(new Property(SCHEMA_NAME.getName(),    memberUUID));
                        props.put(new Property(SCHEMA_VERSION.getName(), memberVer.toString()));
                        break;
    
                    case SCRIPT:
                        LocalObjectLoader.getScript(memberUUID, memberVer);

                        props.put(new Property(SCRIPT_NAME.getName(),    memberUUID));
                        props.put(new Property(SCRIPT_VERSION.getName(), memberVer.toString()));
                        break;
    
                    default:
                        convertToItemPropertyByScript(props, member);
                        break;
                }
            }
            else {
                convertToItemPropertyByScript(props, member);
            }
        }
    }

    /**
     * @param props
     * @param member
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    private void convertToItemPropertyByScript(PropertyArrayList props, DependencyMember member)  throws InvalidDataException, ObjectNotFoundException {
        Logger.msg(5, "Dependency.convertToItemPropertyByScript() - Trying to eval Script for Dependency:"+getName()+" memberUUID:"+member.getChildUUID());

        String scriptName = (String)member.getBuiltInProperty(SCRIPT_NAME);

        if (scriptName != null && scriptName.length() > 0) {
            PropertyArrayList newProps = (PropertyArrayList)member.evaluateScript();
            props.merge(newProps);
        }
    }

    /**
     * Add Dependency specific values to VertexProperties
     * 
     * @param props
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    public void addToVertexProperties(CastorHashMap props) throws InvalidDataException, ObjectNotFoundException {
        BuiltInCollections coll = BuiltInCollections.getValue(getName());
        
        //FIXME: This is a HACK to skip Activity collections, because they might not be complete, 
        //the Version property is missing from Members when created by Script CompositeActivityDefCollSetter
        if(coll != null && coll == BuiltInCollections.ACTIVITY) return;

        for (DependencyMember member : getMembers().list) {
            String memberUUID = member.getChildUUID();
            Integer memberVer = LocalObjectLoader.deriveVersionNumber(member.getBuiltInProperty(VERSION));

            if (memberVer == null) {
                throw new InvalidDataException("Version is null for Collection:" + getName() + ", DependencyMember:" + memberUUID);
            }

            if (coll != null) {
                Logger.msg(5, "Dependency.convertBuiltInCollectionMember() - Dependency:"+getName()+" memberUUID:"+memberUUID);
                //LocalObjectLoader checks if data is valid and loads object to cache
                switch (coll) {
                    case SCHEMA:
                        try {
                            LocalObjectLoader.getSchema(memberUUID, memberVer);
                            props.setBuiltInProperty(SCHEMA_NAME, memberUUID);
                            props.setBuiltInProperty(SCHEMA_VERSION, memberVer);
                        }
                        catch (ObjectNotFoundException e) {
                            //Schema dependency could be defined in Properties
                            if(props.containsKey(SCHEMA_NAME)) {
                                Logger.msg(8, "Dependency.convertBuiltInCollectionMember() - BACKWARD COMPABILITY: Dependency '"+getName()+"' is defined in Properties");
                                String uuid = LocalObjectLoader.getSchema(props).getItemPath().getUUID().toString();
                                props.setBuiltInProperty(SCHEMA_NAME, uuid);
                            }
                        }
                        break;

                    case SCRIPT:
                        try {
                            LocalObjectLoader.getScript(memberUUID, memberVer);
                            props.setBuiltInProperty(SCRIPT_NAME, memberUUID);
                            props.setBuiltInProperty(SCRIPT_VERSION, memberVer);
                        }
                        catch (ObjectNotFoundException e) {
                            //Backward compability: Script dependency could be defined in Properties
                            if(props.containsKey(SCRIPT_NAME)) {
                                Logger.msg(8, "Dependency.convertBuiltInCollectionMember() - BACKWARD COMPABILITY: Dependency '"+getName()+"' is defined in Properties");
                                String uuid = LocalObjectLoader.getScript(props).getItemPath().getUUID().toString();
                                props.setBuiltInProperty(SCRIPT_NAME, uuid);
                            }
                        }
                        break;

                    case STATE_MACHINE:
                        try {
                            LocalObjectLoader.getStateMachine(memberUUID, memberVer);
                            props.setBuiltInProperty(STATE_MACHINE_NAME, memberUUID);
                            props.setBuiltInProperty(STATE_MACHINE_VERSION, memberVer);
                        }
                        catch (ObjectNotFoundException e) {
                            if(props.containsKey(STATE_MACHINE_NAME)) {
                                Logger.msg(8, "Dependency.convertBuiltInCollectionMember() - Dependency '"+getName()+"' is defined in Properties");
                                String uuid = LocalObjectLoader.getStateMachine(props).getItemPath().getUUID().toString();
                                props.setBuiltInProperty(STATE_MACHINE_NAME, uuid);
                            }
                        }
                        break;

                    case ACTIVITY:
                        ActivityDef actDef = LocalObjectLoader.getActDef(memberUUID, memberVer);
                        //TODO: a better way is needed set the list of ActDef UUID and Version
                        props.put("ActivityDefName_"   +actDef.getActName(), memberUUID);
                        props.put("ActivityDefVersion_"+actDef.getActName(), memberVer);
                        break;

                    default:
                        convertToVertextPropsByScript(props, member);
                        break;
                }
            }
            else {
                convertToVertextPropsByScript(props, member);
            }
        }
    }

    /**
     * 
     * @param props
     * @param member
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    private void convertToVertextPropsByScript(CastorHashMap props, DependencyMember member) throws InvalidDataException, ObjectNotFoundException {
        Logger.msg(5, "Dependency.convertToVertextPropsByScript() - Trying to eval Script for Dependency:"+getName()+" memberUUID:"+member.getChildUUID());

        String scriptName = (String)member.getBuiltInProperty(SCRIPT_NAME);

        if (scriptName != null && scriptName.length() > 0) {
            CastorHashMap newProps = (CastorHashMap)member.evaluateScript();
            props.merge(newProps);
        }
    }
}
