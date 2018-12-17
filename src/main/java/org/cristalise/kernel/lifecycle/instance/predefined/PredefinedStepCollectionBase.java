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
package org.cristalise.kernel.lifecycle.instance.predefined;

import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.Logger;

public abstract class PredefinedStepCollectionBase extends PredefinedStep {

    public PredefinedStepCollectionBase() {
        super();
    }

    protected String        collectionName = null;
    protected int           slotID         = -1;
    protected ItemPath      childPath      = null;
    protected CastorHashMap memberNewProps = null;

    Collection<? extends CollectionMember> collection;
    

    /**
     * 
     * @param data
     * @throws InvalidItemPathException
     * @throws ObjectNotFoundException
     */
    protected void setChildPath(String data) throws InvalidItemPathException, ObjectNotFoundException {
        if (ItemPath.isUUID(data)) childPath = new ItemPath(data);
        else                       childPath = new DomainPath(data).getItemPath();
    }

    /**
     * 
     * @return
     * @throws InvalidDataException
     */
    protected Dependency getDependency() throws InvalidDataException {
        if (collection != null) {
            if (collection instanceof Dependency) {
                return (Dependency) collection;
            }
            else {
                String error = collectionName + " is not Dependency (class:" + collection.getClass().getSimpleName()+")";
                Logger.error(error);
                throw new InvalidDataException(error);
            }
        }
        else {
            String error = "Collection '" + collectionName + "' was not found or not initilaised.";
            Logger.error(error);
            throw new InvalidDataException(error);
        }
    }

    /**
     * 
     * @return
     * @throws InvalidDataException
     */
    protected Aggregation getAggregation() throws InvalidDataException {
        if (collection != null) {
            if (collection instanceof Aggregation) {
                return (Aggregation) collection;
            }
            else {
                String error = collectionName + " is not Aggregation (class:" + collection.getClass().getSimpleName()+")";
                Logger.error(error);
                throw new InvalidDataException(error);
            }
        }
        else {
            String error = "Collection '" + collectionName + "' was not found or not initilaised.";
            Logger.error(error);
            throw new InvalidDataException(error);
        }
    }

    /**
     * Unpacks parameters from requestData and retrieves cCollection instance
     * 
     * @param item
     * @param requestData
     * @param locker
     * @throws InvalidDataException
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     */
    protected String[] unpackParamsAndGetCollection(ItemPath item, String requestData, Object locker) 
            throws InvalidDataException, PersistencyException, ObjectNotFoundException
    {
        String[] params = getDataList(requestData);

        if (params == null || params.length < 2) throw new InvalidDataException("CollectionBase - Invalid parameters:" + Arrays.toString(params));

        Logger.msg(2, "PredefinedStepCollectionBase.unpackParamsAndGetCollection() - params:%s", Arrays.toString(params));

        collectionName = params[0];

        // load collection
        collection = (Collection<?>) Gateway.getStorage().get(item, COLLECTION+"/"+collectionName+"/last", locker);

        try {
            if (StringUtils.isNumeric(params[1])) { //Params for Update and Remove operations
                slotID = Integer.parseInt(params[1]);

                if (params.length > 2 && StringUtils.isNotBlank(params[2])) {
                    setChildPath(params[2]);
                }

                if (params.length > 3 && StringUtils.isNotBlank(params[3])) {
                    memberNewProps = (CastorHashMap)Gateway.getMarshaller().unmarshall(params[3]);
                }
            }
            else { //Params for Add operations
                setChildPath(params[1]);

                if (params.length > 2 && StringUtils.isNotBlank(params[2])) {
                    memberNewProps = (CastorHashMap)Gateway.getMarshaller().unmarshall(params[2]);
                }
            }
        }
        catch (Exception e) {
            Logger.error("CollectionBase - Invalid parameters:" + Arrays.toString(params));
            Logger.error(e);
            throw new InvalidDataException("CollectionBase - Invalid parameters:" + Arrays.toString(params));
        }

        return params;
    }

    /**
     * 
     * @return
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    protected DependencyMember getDependencyMember() throws InvalidDataException, ObjectNotFoundException {
        List<CollectionMember> memberList = collection.resolveMembers(slotID, childPath);

        if (memberList.size() != 1)
            throw new InvalidDataException(collectionName + "contains more the one member for slotID:"+slotID+" memberId:"+childPath);

        CollectionMember member = memberList.get(0);

        if (!(member instanceof DependencyMember))
            throw new InvalidDataException(collectionName + " has to be Dependency (member class:" + member.getClass().getSimpleName()+")");
        
        return (DependencyMember)member;
    }

    protected void evaluateScript(ItemPath item, String propertyValue, CastorHashMap scriptProps , Object locker)
            throws ObjectNotFoundException, InvalidDataException, InvalidCollectionModification
    {
        if (StringUtils.isBlank(propertyValue) || !propertyValue.contains(":")) {
            throw new InvalidDataException(this.getClass().getSimpleName() + " cannot resolve Script from '" + propertyValue + "' value");
        }

        String[] tokens = propertyValue.split(":");

        try {
            Script script = Script.getScript(tokens[0], Integer.valueOf(tokens[1]));
            script.evaluate(item, scriptProps, getActContext(), locker);
        }
        catch (ScriptingEngineException e) {
            Logger.error(e);
            throw new InvalidCollectionModification(e.getMessage());
        }
    }
}
