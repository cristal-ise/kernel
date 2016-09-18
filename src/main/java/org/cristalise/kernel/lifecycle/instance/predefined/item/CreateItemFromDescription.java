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
package org.cristalise.kernel.lifecycle.instance.predefined.item;

import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.CREATOR;

import java.io.IOException;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.CollectionDescription;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.CorbaServer;
import org.cristalise.kernel.entity.TraceableEntity;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.property.PropertyUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

public class CreateItemFromDescription extends PredefinedStep {

    public CreateItemFromDescription() {
        super();
    }

    /**
     *  requestdata is xmlstring
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, Object locker)
            throws InvalidDataException,
                   ObjectNotFoundException,
                   ObjectAlreadyExistsException,
                   CannotManageException, 
                   ObjectCannotBeUpdated,
                   PersistencyException
    {
        String[] input = getDataList(requestData);
        String newName = input[0];
        String domPath = input[1];
        String descVer = input.length > 2 ? input[2] : "last";

        Logger.msg(1, "CreateItemFromDescription - Starting.");

        // check if the path is already taken
        DomainPath context = new DomainPath(new DomainPath(domPath), newName);

        if (context.exists()) throw new ObjectAlreadyExistsException("The path " + context + " exists already.");

        // generate new item path with random uuid
        Logger.msg(6, "CreateItemFromDescription - Requesting new item path");
        ItemPath newItemPath = new ItemPath();

        // create the Item object
        Logger.msg(3, "CreateItemFromDescription - Creating Item");
        CorbaServer factory = Gateway.getCorbaServer();

        if (factory == null) throw new CannotManageException("This process cannot create new Items");

        TraceableEntity newItem = factory.createItem(newItemPath);
        Gateway.getLookupManager().add(newItemPath);

        // initialise it with its properties and workflow
        Logger.msg(3, "CreateItemFromDescription - Initializing Item");

        try {
            PropertyArrayList initProps = input.length > 3 ? unmarshallInitProperties(input[3]) : new PropertyArrayList();

            PropertyArrayList   newProps    = instantiateProperties (itemPath, descVer, initProps, newName, agent, locker);
            CollectionArrayList newColls    = instantiateCollections(itemPath, descVer, newProps, locker);
            CompositeActivity   newWorkflow = instantiateWorkflow   (itemPath, descVer, locker);

            newItem.initialise( agent.getSystemKey(),
                                Gateway.getMarshaller().marshall(newProps),
                                Gateway.getMarshaller().marshall(newWorkflow),
                                Gateway.getMarshaller().marshall(newColls));
        }
        catch (MarshalException | ValidationException | AccessRightsException | IOException | MappingException e) {
            Logger.error(e);
            Gateway.getLookupManager().delete(newItemPath);
            throw new InvalidDataException("CreateItemFromDescription: Problem initializing new Item. See log: " + e.getMessage());
        }
        catch(Exception e) {
            Logger.error(e);
            Gateway.getLookupManager().delete(newItemPath);
            throw e;
        }

        // add its domain path
        Logger.msg(3, "CreateItemFromDescription - Creating " + context);
        context.setItemPath(newItemPath);
        Gateway.getLookupManager().add(context);
        return requestData;
    }

    /**
     * Unmarshalls initial Properties
     * 
     * @param initPropString
     * @return unmarshalled initial PropertyArrayList
     * @throws InvalidDataException
     */
    protected PropertyArrayList unmarshallInitProperties(String initPropString) throws InvalidDataException {
        try {
            return (PropertyArrayList) Gateway.getMarshaller().unmarshall(initPropString);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new InvalidDataException("Initial property parameter was not a marshalled PropertyArrayList: " + initPropString);
        }
    }

    /**
     * 
     * @param itemPath
     * @param descVer
     * @param initProps
     * @param newName
     * @param agent
     * @param locker
     * @return props
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    protected PropertyArrayList instantiateProperties(ItemPath itemPath, String descVer, PropertyArrayList initProps, String newName, AgentPath agent, Object locker)
            throws ObjectNotFoundException, InvalidDataException
    {
        // copy properties -- intend to create from propdesc
        PropertyDescriptionList pdList = PropertyUtility.getPropertyDescriptionOutcome(itemPath, descVer, locker);
        PropertyArrayList       props  = pdList.instantiate(initProps);

        // set Name prop or create if not present
        boolean foundName = false;
        for (Property prop : props.list) {
            if (prop.getName().equals("Name")) {
                foundName = true;
                prop.setValue(newName);
                break;
            }
        }

        if (!foundName) props.list.add(new Property(NAME, newName, true));
        props.list.add(new Property(CREATOR, agent.getAgentName(), false));

        return props;
    }

    /**
     * Retrieve the Workflow dependency for the given description version, instantiate the loaded CompositeActivityDef
     * 
     * @param itemPath
     * @param descVer
     * @param locker
     * @return the Workflow instance
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     * @throws PersistencyException
     */
    protected CompositeActivity instantiateWorkflow(ItemPath itemPath, String descVer, Object locker)
            throws ObjectNotFoundException, InvalidDataException, PersistencyException
    {
        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> thisCol = (Collection<? extends CollectionMember>) 
                Gateway.getStorage().get(itemPath, ClusterStorage.COLLECTION + "/"+WORKFLOW+"/" + descVer, locker);

        CollectionMember wfMember  = thisCol.getMembers().list.get(0);
        String           wfDefName = wfMember.resolveItem().getName();
        Object           wfVerObj  = wfMember.getProperties().getBuiltInProperty(VERSION);

        if (wfVerObj == null || String.valueOf(wfVerObj).length() == 0) {
            throw new InvalidDataException("Workflow version number not set");
        }

        try {
            Integer wfDefVer = Integer.parseInt(wfVerObj.toString());

            if (wfDefName == null) throw new InvalidDataException("No workflow given or defined");

            // load workflow def
            CompositeActivityDef wfDef = (CompositeActivityDef) LocalObjectLoader.getActDef(wfDefName, wfDefVer);
            return (CompositeActivity) wfDef.instantiate();
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("Invalid workflow version number: " + wfVerObj.toString());
        }
        catch (ClassCastException ex) {
            Logger.error(ex);
            throw new InvalidDataException("Activity def '" + wfDefName + "' was not Composite");
        }
    }

    /**
     * Copies the CollectionDescriptions of the Item requesting this predefined step.
     * 
     * @param itemPath
     * @param descVer
     * @param locker
     * @return the new collection
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     * @throws InvalidDataException 
     */
    protected CollectionArrayList instantiateCollections(ItemPath itemPath, String descVer, PropertyArrayList newProps , Object locker) 
            throws ObjectNotFoundException, PersistencyException, InvalidDataException
    {
        // loop through collections, collecting instantiated descriptions and finding the default workflow def
        CollectionArrayList colls = new CollectionArrayList();
        String[] collNames = Gateway.getStorage().getClusterContents(itemPath, ClusterStorage.COLLECTION);

        for (String collName : collNames) {
            @SuppressWarnings("unchecked")
            Collection<? extends CollectionMember> thisCol = (Collection<? extends CollectionMember>) 
                    Gateway.getStorage().get(itemPath, ClusterStorage.COLLECTION + "/" + collName + "/" + descVer, locker);

            if (thisCol instanceof CollectionDescription) {
                Logger.msg(5,"CreateItemFromDescription - Instantiating CollectionDescription:"+ collName);
                CollectionDescription<?> thisDesc = (CollectionDescription<?>) thisCol;
                colls.put(thisDesc.newInstance());
            }
            else if(thisCol instanceof Dependency) {
                Logger.msg(5,"CreateItemFromDescription - Instantiating Dependency:"+ collName);
                ((Dependency) thisCol).addToItemProperties(newProps);
            }
            else {
                Logger.warning("CreateItemFromDescription - CANNOT instantiate collection:"+ collName + " class:"+thisCol.getClass().getName());
            }
        }
        return colls;
    }
}
