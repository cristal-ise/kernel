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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ACTIVITY_DEF_URN;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.QUERY_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.QUERY_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCRIPT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCRIPT_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;
import static org.cristalise.kernel.property.BuiltInItemProperties.QUERY_URN;
import static org.cristalise.kernel.property.BuiltInItemProperties.SCHEMA_URN;
import static org.cristalise.kernel.property.BuiltInItemProperties.SCRIPT_URN;
import static org.cristalise.kernel.property.BuiltInItemProperties.STATE_MACHINE_URN;
import static org.cristalise.kernel.property.BuiltInItemProperties.WORKFLOW_URN;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
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

    public Dependency(String name, Integer version) {
        setName(name);
        setVersion(version);
    }

    public Dependency(BuiltInCollections collection) {
        this(collection.getName());
    }

    public Dependency(BuiltInCollections collection, Integer version) {
        this(collection.getName(), version);
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
     * @return array of Property instances fully initialised from classProps (comma separated values)
     */
    public Property[] getClassProperties() {
        String[] classPropNames = getClassProps().split(",");

        Property[] props = new Property[classPropNames.length];
        int i = 0;

        for (String propName: classPropNames) {
            props[i++] = new Property(propName, (String)getProperties().get(propName));
        }

        return props;
    }

    public void updateMember(ItemPath childPath, CastorHashMap memberNewProps)
            throws ObjectNotFoundException, InvalidDataException
    {
        updateMember(childPath, -1, memberNewProps);
    }

    public void updateMember(ItemPath childPath, int memberID, CastorHashMap memberNewProps)
            throws ObjectNotFoundException, InvalidDataException
    {
        List<CollectionMember> members = resolveMembers(memberID, childPath);

        if (members.size() != 1) throw new InvalidDataException("Child item '"+childPath+"' apperars more them once in collection " + mName);

        DependencyMember member = (DependencyMember) members.get(0);

        // Only update existing properties otherwise throw an exception
        for (Entry<String, Object> entry: memberNewProps.entrySet()) {
            if (member.getProperties().containsKey(entry.getKey())) {
                member.getProperties().put(entry.getKey(), entry.getValue());
            }
            else {
                String error = "Property "+entry.getKey()+" does not exists for slotID:" + memberID;
                Logger.error(error);
                throw new ObjectNotFoundException(error);
            }
        }
        
    }

    /**
     * Add a member to the Dependency
     * 
     * @param itemPath the Item to be added as a Member
     * @return DependencyMember the newly created DependencyMember 
     * @throws InvalidCollectionModification if Item is null or the Properties of the Item (e.g. Type) does not match the Collection's
     * @throws ObjectAlreadyExistsException Item is already a member
     */
    public DependencyMember addMember(ItemPath itemPath) 
            throws InvalidCollectionModification, ObjectAlreadyExistsException
    {
        if (itemPath == null) 
            throw new InvalidCollectionModification("Cannot add empty slot to Dependency collection");

        if (contains(itemPath))
            throw new ObjectAlreadyExistsException("Item "+itemPath+" already exists in Dependency "+getName());

        // create member object
        DependencyMember depMember = new DependencyMember();
        depMember.setID(getCounter());
        depMember.setProperties((CastorHashMap)mProperties.clone());
        depMember.setClassProps(mClassProps);

        // assign entity
        depMember.assignItem(itemPath);
        mMembers.list.add(depMember);
        
        Logger.msg(8, "Dependency.addMember(" + itemPath + ") added to children with slotId:"+depMember.getID());
        return depMember;
    }

    /**
     * Returns all DependencyMember that are members of the other collection but not members of this one.
     * 
     * @param other - The collection to compare
     * @return List of Members
     */
    public List<DependencyMember> compare(Dependency other) {
        ArrayList<DependencyMember> newMembers = new ArrayList<DependencyMember>();
        for (DependencyMember thisMember : other.getMembers().list) {
            if (!contains(thisMember.getItemPath())) {
                newMembers.add(thisMember);
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
        if (itemPath == null)
            throw new InvalidCollectionModification("Cannot add empty slot to Dependency collection");

        boolean checkUniqueness = Gateway.getProperties().getBoolean("Dependency.checkMemberUniqueness", true);
        if (checkUniqueness && contains(itemPath))
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
        Logger.msg(8, "Dependency.addMember(" + itemPath + ") added to children with slotId:"+depMember.getID());
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
     * Add Dependency specific values to ItemProperties. First checks if there is a Script to be executed,
     * if no Script defined it will use the default conversion implemented for BuiltInCollections
     * 
     * @param props the current list of ItemProperties
     */
    public void addToItemProperties(PropertyArrayList props) throws InvalidDataException, ObjectNotFoundException {
        Logger.msg(2, "Dependency.addToItemProperties("+getName()+") - Starting ...");

        //convert to BuiltInCollections
        BuiltInCollections builtInColl = BuiltInCollections.getValue(getName());

        for (DependencyMember member : getMembers().list) {
            String memberUUID = member.getChildUUID();
            Integer memberVer = LocalObjectLoader.deriveVersionNumber(member.getBuiltInProperty(VERSION));

            if (memberVer == null) {
                throw new InvalidDataException("Version is null for Collection:" + getName() + ", MemberUUID:" + memberUUID);
            }

            //Do not process this member further
            //  - if Script has done the job already
            //  - or this is not a BuiltInCollection
            if (convertToItemPropertyByScript(props, member) || builtInColl == null) continue;

            Logger.msg(5, "Dependency.addToItemProperties() - BuiltIn Dependency:"+getName()+" memberUUID:"+memberUUID);
            //LocalObjectLoader checks if data is valid and loads object to cache
            switch (builtInColl) {
                //***************************************************************************************************
                case SCHEMA:
                    LocalObjectLoader.getSchema(memberUUID, memberVer);
                    props.put(new Property(SCHEMA_URN, memberUUID+":"+memberVer));
                    break;
                //***************************************************************************************************
                case SCRIPT:
                    LocalObjectLoader.getScript(memberUUID, memberVer);
                    props.put(new Property(SCRIPT_URN, memberUUID+":"+memberVer));
                    break;
                //***************************************************************************************************
                case QUERY:
                    LocalObjectLoader.getQuery(memberUUID, memberVer);
                    props.put(new Property(QUERY_URN, memberUUID+":"+memberVer));
                    break;
                //***************************************************************************************************
                case STATE_MACHINE:
                    if (Gateway.getProperties().getBoolean("Dependency.addStateMachineURN", false) ) {
                        LocalObjectLoader.getStateMachine(memberUUID, memberVer);
                        props.put(new Property(STATE_MACHINE_URN, memberUUID+":"+memberVer));
                    }
                    break;
                //***************************************************************************************************
                case WORKFLOW:
                    if (Gateway.getProperties().getBoolean("Dependency.addWorkflowURN", false) ) {
                        LocalObjectLoader.getCompActDef(memberUUID, memberVer);
                        props.put(new Property(WORKFLOW_URN, memberUUID+":"+memberVer));
                    }
                    break;
                //***************************************************************************************************
                default:
                    Logger.msg(8, "Dependency.addToItemProperties() - Cannot handle BuiltIn Dependency:"+getName());
                    break;
            }
        }
    }

    /**
     * Executes Script if it was defined in the Member properties
     * 
     * @param props the current list of ItemProperties
     * @param member the current DependencyMember
     * @return true when Script was executed
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    private boolean convertToItemPropertyByScript(PropertyArrayList props, DependencyMember member)  throws InvalidDataException, ObjectNotFoundException {
        Logger.msg(5, "Dependency.convertToItemPropertyByScript() - Dependency:"+getName()+" memberUUID:"+member.getChildUUID());

        String scriptName = (String)member.getBuiltInProperty(SCRIPT_NAME);

        if (scriptName != null && scriptName.length() > 0) {
            PropertyArrayList newProps = (PropertyArrayList)member.evaluateScript();
            props.merge(newProps);
            return true;
        }
        return false;
    }

    /**
     * Add Dependency specific values to VertexProperties (CastorHashMap). First checks if there is a Script 
     * to be executed, if no Script defined it will use the default conversion implemented for BuiltInCollections
     * 
     * @param props the current list of VertexProperties
     * @throws InvalidDataException inconsistent data was provided
     * @throws ObjectNotFoundException objects were not found while reading the properties
     */
    public void addToVertexProperties(CastorHashMap props) throws InvalidDataException, ObjectNotFoundException {
        Logger.msg(2, "Dependency.addToVertexProperties("+getName()+") - Starting ...");

        BuiltInCollections builtInColl = BuiltInCollections.getValue(getName());

        for (DependencyMember member : getMembers().list) {
            String memberUUID = member.getChildUUID();
            Integer memberVer = LocalObjectLoader.deriveVersionNumber(member.getBuiltInProperty(VERSION));

            if (memberVer == null) {
                throw new InvalidDataException("Version is null for Collection:" + getName() + ", DependencyMember:" + memberUUID);
            }

            //Do not process this member further
            //  - if Script has done the job already
            //  - or this is not a BuiltInCollection
            if (convertToVertextPropsByScript(props, member) || builtInColl == null) continue;

            Logger.msg(5, "Dependency.addToVertexProperties() - Dependency:"+getName()+" memberUUID:"+memberUUID);
            //LocalObjectLoader checks if data is valid and loads object to cache
            switch (builtInColl) {
                //***************************************************************************************************
                case SCHEMA:
                    try {
                        LocalObjectLoader.getSchema(memberUUID, memberVer);
                        props.setBuiltInProperty(SCHEMA_NAME, memberUUID);
                        props.setBuiltInProperty(SCHEMA_VERSION, memberVer);
                    }
                    catch (ObjectNotFoundException e) {
                        //Schema dependency could be defined in Properties
                        if(props.containsKey(SCHEMA_NAME)) {
                            Logger.msg(8, "Dependency.addToVertexProperties() - BACKWARD COMPABILITY: Dependency '"+getName()+"' is defined in Properties");
                            String uuid = LocalObjectLoader.getSchema(props).getItemPath().getUUID().toString();
                            props.setBuiltInProperty(SCHEMA_NAME, uuid);
                        }
                    }
                    break;
                //***************************************************************************************************
                case SCRIPT:
                    try {
                        LocalObjectLoader.getScript(memberUUID, memberVer);
                        props.setBuiltInProperty(SCRIPT_NAME, memberUUID);
                        props.setBuiltInProperty(SCRIPT_VERSION, memberVer);
                    }
                    catch (ObjectNotFoundException e) {
                        //Backward compability: Script dependency could be defined in Properties
                        if(props.containsKey(SCRIPT_NAME)) {
                            Logger.msg(8, "Dependency.addToVertexProperties() - BACKWARD COMPABILITY: Dependency '"+getName()+"' is defined in Properties");
                            String uuid = LocalObjectLoader.getScript(props).getItemPath().getUUID().toString();
                            props.setBuiltInProperty(SCRIPT_NAME, uuid);
                        }
                    }
                    break;
                //***************************************************************************************************
                case QUERY:
                    try {
                        LocalObjectLoader.getQuery(memberUUID, memberVer);
                        props.setBuiltInProperty(QUERY_NAME, memberUUID);
                        props.setBuiltInProperty(QUERY_VERSION, memberVer);
                    }
                    catch (ObjectNotFoundException e) {
                        //Backward compability: Query dependency could be defined in Properties
                        if(props.containsKey(QUERY_NAME)) {
                            Logger.msg(8, "Dependency.addToVertexProperties() - BACKWARD COMPABILITY: Dependency '"+getName()+"' is defined in Properties");
                            String uuid = LocalObjectLoader.getQuery(props).getItemPath().getUUID().toString();
                            props.setBuiltInProperty(QUERY_NAME, uuid);
                        }
                    }
                    break;
                //***************************************************************************************************
                case STATE_MACHINE:
                    try {
                        LocalObjectLoader.getStateMachine(memberUUID, memberVer);
                        props.setBuiltInProperty(STATE_MACHINE_NAME, memberUUID);
                        props.setBuiltInProperty(STATE_MACHINE_VERSION, memberVer);
                    }
                    catch (ObjectNotFoundException e) {
                        if(props.containsKey(STATE_MACHINE_NAME)) {
                            Logger.msg(8, "Dependency.addToVertexProperties() -  BACKWARD COMPABILITY: Dependency '"+getName()+"' is defined in Properties");
                            String uuid = LocalObjectLoader.getStateMachine(props).getItemPath().getUUID().toString();
                            props.setBuiltInProperty(STATE_MACHINE_NAME, uuid);
                        }
                    }
                    break;
                //***************************************************************************************************
                case ACTIVITY:
                    ActivityDef actDef = LocalObjectLoader.getActDef(memberUUID, memberVer);
                    CastorHashMap chm = null;

                    if(props.containsKey(ACTIVITY_DEF_URN)) {
                        chm = (CastorHashMap)props.getBuiltInProperty(ACTIVITY_DEF_URN);
                    }
                    else {
                        chm = new CastorHashMap();
                        props.setBuiltInProperty(ACTIVITY_DEF_URN, chm);
                    }

                    Logger.msg(8, "Dependency.addToVertexProperties("+getName()+") - actDef:"+actDef.getActName());

                    chm.put(actDef.getActName(), memberUUID+"~"+memberVer);
                    break;
                //***************************************************************************************************
                default:
                    Logger.msg(8, "Dependency.addToVertexProperties() - Cannot handle BuiltIn Dependency:"+getName());
                    break;
            }
        }
    }

    /**
     * Executes Script if it was defined in the Member properties
     * 
     * @param props the current list of VertexProperties
     * @param member the current DependencyMember
     * @return true when Script was executed
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    private boolean convertToVertextPropsByScript(CastorHashMap props, DependencyMember member) throws InvalidDataException, ObjectNotFoundException {
        Logger.msg(5, "Dependency.convertToVertextPropsByScript() - Dependency:"+getName()+" memberUUID:"+member.getChildUUID());

        String scriptName = (String)member.getBuiltInProperty(SCRIPT_NAME);

        if (scriptName != null && scriptName.length() > 0) {
            CastorHashMap newProps = (CastorHashMap)member.evaluateScript();
            props.merge(newProps);
            return true;
        }
        return false;
    }
}
