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
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lifecycle.instance.stateMachine.State;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
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
        setBuiltInProperty(BuiltInVertexProperties.Abortable, false);
        setBuiltInProperty(BuiltInVertexProperties.RepeatWhen, false);
        try {
			setChildrenGraphModel(new GraphModel(new WfVertexOutlineCreator()));
		} catch (InvalidDataException e) { } // shouldn't happen with an empty one
        setIsComposite(true);
    }

	@Override
	protected String getDefaultSMName() {
		return "CompositeActivity";
	}

	@Override
	public void setChildrenGraphModel(GraphModel childrenGraph) throws InvalidDataException {
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
     * Initialize Vertex and attach to the current activity
     *
     * @param vertex
     * @param first if true, the Waiting state will be one of the first launched by the parent activity
     * @param point
     */
    public void initChild(WfVertex vertex, boolean first, GraphPoint point)
    {
        safeAddChild(vertex, point);
        if (first) setFirstVertex(vertex.getID());
    }

    /**
     * 
     * @param vertexID
     */
    public void setFirstVertex(int vertexID) {
        Logger.msg(5, "org.cristalise.kernel.lifecycle.CompositeActivity::setFirstVertex() vertexID:"+vertexID);

        getChildrenGraphModel().setStartVertexId(vertexID);
    }


    /**
     * Adds vertex to graph cloning GraphPoint first (NPE safe)
     * 
     * @param v
     * @param g
     */
    private void safeAddChild(GraphableVertex v, GraphPoint g) {
        GraphPoint p = null;
        if(g != null) p = new GraphPoint(g.x, g.y);
        addChild(v, p);
    }

    /**
     * 
     * @param child
     * @param Name
     * @param point
     * @return  child vertex
     */
    public WfVertex newExistingChild(Activity child, String Name, GraphPoint point)
    {
        child.setName(Name);
        safeAddChild(child, point);
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
        return newChild(Types.valueOf(vertexTypeId), "False id", false, point);
    }

    /**
     * 
     * @param type
     * @param name
     * @param first
     * @param point
     * @return new child vertex
     */
    public WfVertex newChild(Types type, String name, boolean first, GraphPoint point) {
        switch (type) {
            case Atomic:    return newAtomChild(name, first, point);
            case Composite: return newCompChild(name, first, point);
            case OrSplit:   return newSplitChild(name, "Or",   first, point);
            case XOrSplit:  return newSplitChild(name, "XOr",  first, point);
            case AndSplit:  return newSplitChild(name, "And",  first, point);
            case LoopSplit: return newSplitChild(name, "Loop", first, point);
            case Join:      return newJoinChild(name, "Join",  first, point);
            case Route:     return newJoinChild(name, "Route", first, point);
    
            default:
                throw new IllegalArgumentException("Unhandled enum value of WfVertex.Type:" + type.name());
        }
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
    public Split newSplitChild(String name, String Type, boolean first, GraphPoint point)
    {
        Split split = null;

        if      (Type.equals("Or"))   { split = new OrSplit(); } 
        else if (Type.equals("XOr"))  { split = new XOrSplit(); }
        else if (Type.equals("Loop")) { split = new Loop(); }
        else                          { split = new AndSplit(); }

        initChild(split, first, point);
        split.setName(name);

        return split;
    }

    /**
     * Method newJoinChild.
     *
     * @param point
     * @return Join
     */
    public Join newJoinChild(String name, String type, boolean first, GraphPoint point)
    {
        Join join = new Join();
        join.getProperties().put("Type", type);
        initChild(join, first, point);
        join.setName(name);
        return join;
    }

    /*
    public Join newRouteChild(GraphPoint point)
    {
        Join join = new Join();
        join.getProperties().put("Type", "Route");
        safeAddChild(join, point);
        return join;
    }
    */

    /**
     * None recursive search by id
     * 
     * @param id
     * @return WfVertex
     */
    WfVertex search(int id)
    {
        return (WfVertex)getChildrenGraphModel().resolveVertex(id);
    }

    @Override
	public void run(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException
    {
        Logger.debug(8, getPath() + "CompositeActivity::run() state: " + getState());

        super.run(agent, itemPath, locker);
        State currentState = getStateMachine().getState(state);
        if (getChildrenGraphModel().getStartVertex() != null && !currentState.isFinished())
        {
        	
        	if (getStateMachine().getInitialStateCode() == state) {
        		Transition autoStart = null;
        		//see if there's only one that isn't terminating
    			try {
					for (Transition transition : getStateMachine().getPossibleTransitions(this, agent).keySet()) {
						if (!transition.isFinishing()) {
							if (autoStart == null)
								autoStart = transition;
							else {
								autoStart = null;
								break;
							}
						}
					}
				} catch (ObjectNotFoundException e) {
					Logger.error(e);
					throw new InvalidDataException("Problem calculating possible transitions for agent "+agent.toString());
				}
        		if (autoStart != null) {
					try {
						request(agent, null, itemPath, autoStart.getId(), null, locker);
					} catch (RuntimeException e) {
						throw e;
					} catch (AccessRightsException e) { // Agent didn't have permission to start the activity, so leave it waiting
						Logger.error(e);
						return;
					} catch (Exception e) {
						throw new InvalidDataException("Problem initializing composite activity: "+e.getMessage());
					}
					WfVertex first = (WfVertex) getChildrenGraphModel().getStartVertex();
					first.run(agent, itemPath, locker);
        		}
        	}
        }
    }

    @Override
	public void runNext(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException 
    {
        if (!getStateMachine().getState(state).isFinished()) {
        	Transition trans = null;
        	try {
				for (Transition possTran : getStateMachine().getPossibleTransitions(this, agent).keySet()) {
					// Find the next transition for automatic procedure. A non-finishing transition will override a finishing one, 
					// but otherwise having more than one possible means we cannot proceed. Transition enablement should filter before this point. 

					if (trans == null || (trans.isFinishing() && !possTran.isFinishing()))
						trans = possTran;
					else if (trans.isFinishing() == possTran.isFinishing()) {
						Logger.warning("Unclear choice of transition possible from current state for Composite Activity '"+getName()+"'. Cannot automatically proceed.");
						setActive(true);
						return;
					}		
				}
			} catch (ObjectNotFoundException e) {
				Logger.error(e);
				throw new InvalidDataException("Problem calculating possible transitions for agent "+agent.toString());
			}
        	
        	if (trans == null) { // current agent can't proceed
        		Logger.msg(3, "Not possible for the current agent to proceed with the Composite Activity '"+getName()+"'.");
        		setActive(true);
        		return;
        	}

        	// automatically execute the next outcome if it doesn't require an outcome.
        	if (trans != null) {
        		if (trans.hasOutcome(getProperties()) || trans.hasScript(getProperties())) {
	        		Logger.msg(3, "Composite activity '"+getName()+"' has script or schema defined. Cannot proceed automatically.");
	        		setActive(true);
	        		return;
	        	}
        		
				try {
					request(agent, null, itemPath, trans.getId(), null, locker);
					if (!trans.isFinishing()) // don't run next if we didn't finish
						return;
				} catch (Exception e) {
					Logger.error(e);
					setActive(true);
					throw new InvalidDataException("Problem completing composite activity: "+e.getMessage());
				}
        	}
        }
        super.runNext(agent, itemPath, locker);
    }


    /**
     * 
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
        return addNext(search(originID), search(terminusID));
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
	public void abort() {
        GraphableVertex[] vChildren = getChildren();
        for (int i = 0; i < vChildren.length; i++)
        {
            ((WfVertex) vChildren[i]).abort();
        }
	}
    
    public boolean hasActive() {
    	GraphableVertex[] vChildren = getChildren();
    	for (int i = 0; i < vChildren.length; i++) {
    		if (!(vChildren[i] instanceof Activity))
    			continue;
    		Activity childAct = (Activity)vChildren[i];
    		if (childAct.getActive())
    				return true; // if a child activity is active
    		if (childAct instanceof CompositeActivity && 
    				((CompositeActivity)vChildren[i]).hasActive())
    				return true; // if a child composite has active children
    	}
    	return false; // don't include own active status
    }

    @Override
	public String request(AgentPath agent, AgentPath delegator, ItemPath itemPath, int transitionID, String requestData, Object locker) throws AccessRightsException, InvalidTransitionException, InvalidDataException, ObjectNotFoundException, PersistencyException, ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, InvalidCollectionModification
    {
        Transition trans = getStateMachine().getTransition(transitionID);
        if (trans.isFinishing() && hasActive()) {
        	if ((Boolean)getBuiltInProperty(BuiltInVertexProperties.Abortable))
        		abort();
        	else
        		throw new InvalidTransitionException("Attempted to finish a composite activity that had active children but was not Abortable");
        }
        
        if (getStateMachine().getTransition(transitionID).reinitializes()) {
        	int preserveState = state;
        	reinit(getID());
        	setState(preserveState);
        }
        
        if (getChildrenGraphModel().getStartVertex() != null && (getStateMachine().getState(state).equals(getStateMachine().getInitialState())
        		|| getStateMachine().getTransition(transitionID).reinitializes()))
        	((WfVertex) getChildrenGraphModel().getStartVertex()).run(agent, itemPath, locker);
        
        return super.request(agent, delegator, itemPath, transitionID, requestData, locker);
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
