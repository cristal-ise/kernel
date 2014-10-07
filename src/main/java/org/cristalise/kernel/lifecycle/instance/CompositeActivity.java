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
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.Logger;


/**
 * @version $Revision: 1.86 $ $Date: 2005/10/05 07:39:37 $
 * @author $Author: abranson $
 */
public class CompositeActivity extends Activity
{
	

    /*
     * --------------------------------------------
     * ----------------CONSTRUCTOR-----------------
     * --------------------------------------------
     */
    public CompositeActivity()
    {
        super();
        setChildrenGraphModel(new GraphModel(new WfVertexOutlineCreator()));
        setIsComposite(true);
    }
    
    // State machine
	public static final int START = 0;
	public static final int COMPLETE = 1;
	@Override
	protected String getDefaultSMName() {
		return "CompositeActivity";
	}

	@Override
	public void setChildrenGraphModel(GraphModel childrenGraph) {
		super.setChildrenGraphModel(childrenGraph);
		childrenGraph.setVertexOutlineCreator(new WfVertexOutlineCreator());
	}
    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#verify()
     */
    /*
     * -------------------------------------------- --------------Other
     * Functions--------------- --------------------------------------------
     */
    /** launch the verification of the subprocess() */
    @Override
	public boolean verify()
    {
        boolean err = super.verify();
        GraphableVertex[] vChildren = getChildren();
        for (int i = 0; i < vChildren.length; i++)
        {
            if (!((WfVertex) vChildren[i]).verify())
            {
                mErrors.add("error in children");
                return false;
            }
        }
        return err;
    }

    /**
     * Method initChild.
     *
     * @param act
     * @param first
     * @param point
     */
    /**
     * Create an initialize a Activity attached to the current activity
     *
     * @param first :
     *            if true, the activity Waiting will be one of the first
     *            launched by the parent activity
     */
    public void initChild(Activity act, boolean first, GraphPoint point)
    {
        this.addChild(act, new GraphPoint(point.x, point.y));
        if (first)
        {
            getChildrenGraphModel().setStartVertexId(act.getID());
            Logger.msg(5, "org.cristalise.kernel.lifecycle.CompositeActivity :: " + getID() + " is first");
        }
    }

    /**
     * Method newChild.
     *
     * @param Name
     * @param Type
     * @param point
     * @return WfVertex
     */
    public WfVertex newExistingChild(Activity child, String Name, GraphPoint point)
    {
        child.setName(Name);
        addChild(child, new GraphPoint(point.x, point.y));
        return child;
    }

    /**
     * Method newChild.
     *
     * @param Name
     * @param Type
     * @param point
     * @return WfVertex
     */
    public WfVertex newChild(String Name, String Type, GraphPoint point)
    {
        WfVertex v = newChild(Type, point);
        v.setName(Name);
        return v;
    }

    /**
     * Method newChild.
     *
     * @param vertexTypeId
     * @param point
     * @return WfVertex
     */
    public WfVertex newChild(String vertexTypeId, GraphPoint point)
    {
        WfVertex wfVertex = null;
        if (vertexTypeId.equals("Atomic"))
        {
            wfVertex = newAtomChild("False id", false, point);
        } else if (vertexTypeId.equals("Composite"))
        {
            wfVertex = newCompChild("False id", false, point);
        } else if (vertexTypeId.endsWith("Split"))
        {
            if (vertexTypeId.startsWith("Or"))
            {
                wfVertex = newSplitChild("Or", point);
            } else if (vertexTypeId.startsWith("XOr"))
            {
                wfVertex = newSplitChild("XOr", point);
            } else if (vertexTypeId.startsWith("Loop"))
            {
                wfVertex = newSplitChild("Loop", point);
            } else
            {
                wfVertex = newSplitChild("And", point);
            }
        } else if (vertexTypeId.equals("Join"))
        {
            wfVertex = newJoinChild(point);
        } else if (vertexTypeId.equals("Route"))
        {
            wfVertex = newRouteChild(point);
        }
        return wfVertex;
    }

    /**
     * Method newCompChild.
     *
     * @param id
     * @param first
     * @param point
     * @return CompositeActivity Create an initialize a composite Activity
     *         attached to the current activity
     */
    public CompositeActivity newCompChild(String id, boolean first, GraphPoint point)
    {
        CompositeActivity act = new CompositeActivity();
        initChild(act, first, point);
        act.setName(id);
        return act;
    }

    /**
     * Method newAtomChild.
     *
     * @param id
     * @param first
     * @param point
     * @return Activity Create an initialize an Atomic Activity attached to the
     *         current activity
     *
     */
    public Activity newAtomChild(String id, boolean first, GraphPoint point)
    {
        Activity act = new Activity();
        initChild(act, first, point);
        act.setName(id);
        return act;
    }

    /**
     * Method newSplitChild.
     *
     * @param Type
     * @param point
     * @return Split
     */
    public Split newSplitChild(String Type, GraphPoint point)
    {
        Split split;
        if (Type.equals("Or"))
        {
            split = new OrSplit();
        } else if (Type.equals("XOr"))
        {
            split = new XOrSplit();
        } else if (Type.equals("Loop"))
        {
            split = new Loop();
        } else
        {
            split = new AndSplit();
        }
        addChild(split, new GraphPoint(point.x, point.y));
        return split;
    }

    /**
     * Method newJoinChild.
     *
     * @param point
     * @return Join
     */
    public Join newJoinChild(GraphPoint point)
    {
        Join join = new Join();
        join.getProperties().put("Type", "Join");
        addChild(join, new GraphPoint(point.x, point.y));
        return join;
    }

    public Join newRouteChild(GraphPoint point)
    {
        Join join = new Join();
        join.getProperties().put("Type", "Route");
        addChild(join, new GraphPoint(point.x, point.y));
        return join;
    }

    /**
     * Method search.
     *
     * @param ids
     * @return WfVertex
     */
    WfVertex search(int ids)
    {
        for (int i = 0; i < getChildren().length; i++)
        {
            WfVertex ver = (WfVertex) getChildren()[i];
            if (ver instanceof Split)
            {
                if (ver.getID() == ids)
                {
                    return ver;
                }
            }
            if (ver instanceof Join)
            {
                if (ver.getID() == ids)
                {
                    return ver;
                }
            }
        }
        return null;
    }

    /**
     * @throws InvalidDataException 
     * @throws ObjectNotFoundException 
     * @throws AccessRightsException 
     * @throws InvalidTransitionException 
     * @throws PersistencyException 
     * @throws ObjectAlreadyExistsException 
     * @throws ObjectCannotBeUpdated 
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#run()
     */
    @Override
	public void run(AgentPath agent, ItemPath itemPath) throws InvalidDataException
    {
        super.run(agent, itemPath);
        if (getChildrenGraphModel().getStartVertex() != null && !getStateMachine().getState(state).isFinished())
        {
            WfVertex first = (WfVertex) getChildrenGraphModel().getStartVertex();
            first.run(agent, itemPath);
        }
    }

    @Override
	public void runNext(AgentPath agent, ItemPath itemPath) throws InvalidDataException 
    {
        if (!getStateMachine().getState(state).isFinished())
			try {
				request(agent, itemPath, CompositeActivity.COMPLETE, null);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) { 
				Logger.error(e); // current agent couldn't complete the composite, so leave it
			} 
        super.runNext(agent, itemPath);
    }


    /**
     * @throws InvalidDataException 
     * @throws ObjectNotFoundException 
     * @throws InvalidAgentPathException 
     * @see org.cristalise.kernel.lifecycle.instance.Activity#calculateJobs()
     */
    @Override
	public ArrayList<Job> calculateJobs(AgentPath agent, ItemPath itemPath, boolean recurse) throws InvalidAgentPathException, ObjectNotFoundException, InvalidDataException 
    {
        ArrayList<Job> jobs = new ArrayList<Job>();
        boolean childActive = false;
        if (recurse)
            for (int i = 0; i < getChildren().length; i++)
                if (getChildren()[i] instanceof Activity)
                {
                    Activity child = (Activity) getChildren()[i];
                    jobs.addAll(child.calculateJobs(agent, itemPath, recurse));
                    childActive |= child.active;
                }
        if (!childActive)
            jobs.addAll(super.calculateJobs(agent, itemPath, recurse));
        return jobs;
    }

    @Override
	public ArrayList<Job> calculateAllJobs(AgentPath agent, ItemPath itemPath, boolean recurse) throws InvalidAgentPathException, ObjectNotFoundException, InvalidDataException 
    {
        ArrayList<Job> jobs = new ArrayList<Job>();
        if (recurse)
            for (int i = 0; i < getChildren().length; i++)
                if (getChildren()[i] instanceof Activity)
                {
                    Activity child = (Activity) getChildren()[i];
                    jobs.addAll(child.calculateAllJobs(agent, itemPath, recurse));
                }
        jobs.addAll(super.calculateAllJobs(agent, itemPath, recurse));
        return jobs;
    }

    /**
     * Method addNext.
     *
     * @param origin
     * @param terminus
     * @return Next
     */
    public Next addNext(WfVertex origin, WfVertex terminus)
    {
        return new Next(origin, terminus);
    }

    /**
     * Method addNext.
     *
     * @param originID
     * @param terminusID
     * @return Next
     */
    public Next addNext(int originID, int terminusID)
    {
        Next n = new Next();
        n.setParent(this);
        getChildrenGraphModel().addEdgeAndCreateId(n, originID, terminusID);
        return n;
    }

    /**
     * Method hasGoodNumberOfActivity.
     *
     * @return boolean
     */
    public boolean hasGoodNumberOfActivity()
    {
        int endingAct = 0;
        for (int i = 0; i < getChildren().length; i++)
        {
            WfVertex vertex = (WfVertex) getChildren()[i];
            if (getChildrenGraphModel().getOutEdges(vertex).length == 0)
                endingAct++;
        }
        if (endingAct > 1)
            return false;
        return true;
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.Activity#getType()
     */
    @Override
	public String getType()
    {
        return super.getType();
    }

    /**
     * @throws InvalidDataException 
     *
     */
    @Override
	public void reinit(int idLoop) throws InvalidDataException
    {
        super.reinit(idLoop);
        if (getChildrenGraphModel().getStartVertex() != null && !getStateMachine().getState(state).isFinished())
            ((WfVertex) getChildrenGraphModel().getStartVertex()).reinit(idLoop);
    }

    @Override
	public String request(AgentPath agent, ItemPath itemPath, int transitionID, String requestData) throws AccessRightsException, InvalidTransitionException, InvalidDataException, ObjectNotFoundException, PersistencyException, ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, InvalidCollectionModification
    {
        if (getChildrenGraphModel().getStartVertex() != null && !getStateMachine().getState(state).isFinished() && transitionID == CompositeActivity.START)
        	((WfVertex) getChildrenGraphModel().getStartVertex()).run(agent, itemPath);

        return super.request(agent, itemPath, transitionID, requestData);
    }
    
	public void refreshJobs(ItemPath itemPath)
    {
        GraphableVertex[] children = getChildren();
        for (GraphableVertex element : children)
			if (element instanceof CompositeActivity)
                ((CompositeActivity) element).refreshJobs(itemPath);
            else if (element instanceof Activity)
                ((Activity) element).pushJobsToAgents(itemPath);
    }
}