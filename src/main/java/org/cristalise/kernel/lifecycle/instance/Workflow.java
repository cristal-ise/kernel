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
package org.cristalise.kernel.lifecycle.instance;

import java.util.ArrayList;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.graph.model.TypeNameAndConstructionInfo;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStepContainer;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.utils.Logger;

/**
 * @version $Revision: 1.64 $ $Date: 2005/09/30 07:09:48 $
 * @author $Author: abranson $
 */
public class Workflow extends CompositeActivity implements C2KLocalObject {
    public History   history;
    private ItemPath itemPath = null;

    /** TypeNameAndConstructionInfo[] variables added by Steve */
    private final TypeNameAndConstructionInfo[] mVertexTypeNameAndConstructionInfo = {
            new TypeNameAndConstructionInfo("AND Split", "AndSplit"),
            new TypeNameAndConstructionInfo("OR Split",  "OrSplit"),
            new TypeNameAndConstructionInfo("XOR Split", "XOrSplit"),
            new TypeNameAndConstructionInfo("Join",      "Join"),
            new TypeNameAndConstructionInfo("Loop",      "LoopSplit"),
            new TypeNameAndConstructionInfo("Activity",  "Atomic"),
            new TypeNameAndConstructionInfo("Composite", "Composite")
    };
    private final TypeNameAndConstructionInfo[] mEdgeTypeNameAndConstructionInfo   = {
            new TypeNameAndConstructionInfo("Next", "Next")
    };

    /**
     * @see java.lang.Object#Object()
     */
    public Workflow() {
    }

    public Workflow(CompositeActivity domain, PredefinedStepContainer predef) {
        this();
        domain.setName("domain");
        initChild(domain, true, new GraphPoint(150, 100));
        addChild(predef, new GraphPoint(300, 100));
    }

    /**
     * Caches a History object for this Item, using the workflow as a locker. This object will be used for all Event storage during
     * execution, to reduce the cost of creating a new one for each one.
     * 
     * For other storage, such as during initialization, a non-cached History is created
     * 
     * @param locker the transaction locker
     * @return History object
     * @throws InvalidDataException inconsistent data
     */
    public History getHistory(Object locker) throws InvalidDataException {
        if (locker != this) return new History(itemPath, locker);
        
        if (history == null) {
            if (itemPath == null) throw new InvalidDataException("Workflow not initialized.");

            history = new History(itemPath, this);
        }
        return history;
    }

    public History getHistory() throws InvalidDataException {
        return getHistory(this);
    }

    /**
     * Method getVertexTypeNameAndConstructionInfo.
     *
     * @return TypeNameAndConstructionInfo[]
     */
    /** getVertexTypeNameAndConstructionInfo() added by Steve */
    public TypeNameAndConstructionInfo[] getVertexTypeNameAndConstructionInfo() {
        return mVertexTypeNameAndConstructionInfo;
    }

    /**
     * Method getEdgeTypeNameAndConstructionInfo.
     *
     * @return TypeNameAndConstructionInfo[]
     */
    /** getVertexTypeNameAndConstructionInfo() added by Steve */
    public TypeNameAndConstructionInfo[] getEdgeTypeNameAndConstructionInfo() {
        return mEdgeTypeNameAndConstructionInfo;
    }

    public String requestAction(AgentPath agent, AgentPath delegator, String stepPath, ItemPath itemPath, int transitionID, String requestData, String attachmentType, byte[] attachment)
            throws ObjectNotFoundException, AccessRightsException, InvalidTransitionException, InvalidDataException,
                   ObjectAlreadyExistsException, PersistencyException, ObjectCannotBeUpdated, CannotManageException,
                   InvalidCollectionModification
    {
        Logger.msg(3, "Workflow::requestAction() - transition:" + transitionID + " step:" + stepPath + " agent:" + agent);
        GraphableVertex vert = search(stepPath);
        if (vert != null && vert instanceof Activity)
            return ((Activity) vert).request(agent, delegator, itemPath, transitionID, requestData, attachmentType, attachment, this);
        else
            throw new ObjectNotFoundException(stepPath + " not found");
    }

    /**
     * @see org.cristalise.kernel.graph.model.GraphableVertex#getPath()
     */
    @Override
    public String getPath() {
        return "workflow";
    }

    /**
     * @see org.cristalise.kernel.graph.model.Vertex#getName()
     */
    @Override
    public String getName() {
        return "workflow";
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.Activity#getType()
     */
    @Override
    public String getType() {
        return "workflow";
    }

    /**
     * @see org.cristalise.kernel.graph.model.Vertex#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#verify()
     */
    @Override
    public boolean verify() {
        for (int i = 0; i < getChildren().length; i++) {
            if (!((WfVertex) getChildren()[i]).verify()) {
                mErrors.add("error in children");
                return false;
            }
        }
        return true;
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.Activity#getWf()
     */
    @Override
    public Workflow getWf() {
        return this;
    }

    public void initialise(ItemPath itemPath, AgentPath agent, Object locker) throws InvalidDataException {
        setItemPath(itemPath);
        runFirst(agent, itemPath, locker);
    }

    public ItemPath getItemPath() {
        return itemPath;
    }

    public void setItemPath(ItemPath itemPath) {
        this.itemPath = itemPath;
    }

    public void setItemUUID(String uuid) throws InvalidItemPathException {
        setItemPath(new ItemPath(uuid));
    }

    public String getItemUUID() {
        return getItemPath().getUUID().toString();
    }

    /**
     * if type = 0 only domain steps will be queried if type = 1 only predefined steps will be queried else both will be queried
     */
    public ArrayList<Job> calculateJobs(AgentPath agent, ItemPath itemPath, int type)
            throws InvalidAgentPathException, ObjectNotFoundException, InvalidDataException {
        ArrayList<Job> jobs = new ArrayList<Job>();
        if (type != 1)
            jobs.addAll(((CompositeActivity) search("workflow/domain")).calculateJobs(agent, itemPath, true));
        if (type != 0)
            jobs.addAll(((CompositeActivity) search("workflow/predefined")).calculateJobs(agent, itemPath, true));
        return jobs;
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.CompositeActivity#hasGoodNumberOfActivity()
     */
    @Override
    public boolean hasGoodNumberOfActivity() {
        return true;
    }

    /**
     * @see org.cristalise.kernel.entity.C2KLocalObject#getClusterType()
     */
    @Override
    public ClusterType getClusterType() {
        return ClusterType.LIFECYCLE;
    }
    @Override
    public String getClusterPath() {
        return getClusterType()+"/workflow";
    }
}
