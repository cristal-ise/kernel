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
package org.cristalise.kernel.entity;

import java.util.UUID;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.entity.agent.JobArrayList;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.predefined.item.ItemPredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.TransactionManager;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.utils.Logger;


public class ItemImplementation implements ItemOperations {
	
	protected final TransactionManager          mStorage;
	protected final ItemPath                    mItemPath;
	
	protected ItemImplementation(ItemPath key) {
		this.mStorage = Gateway.getStorage();
		this.mItemPath = key;
	}
	
	@Override
	public SystemKey getSystemKey() {
		return mItemPath.getSystemKey();
	}
	
	public UUID getUUID() {
		return mItemPath.getUUID();
	}

	@Override
	public void initialise(SystemKey agentId, String propString, String initWfString,
			String initCollsString) throws AccessRightsException,
			InvalidDataException, PersistencyException 
	{
        Logger.msg(5, "Item::initialise("+mItemPath+") - agent:"+agentId);
		Object locker = new Object();
		
		AgentPath agentPath;
		try {
			agentPath = new AgentPath(agentId);
		} catch (InvalidItemPathException e) {
			throw new AccessRightsException("Invalid Agent Id:" + agentId);
		}

		// must supply properties
		if (propString == null || propString.length() == 0) {
			throw new InvalidDataException("No properties supplied");
		}

		// store properties
		try {
			PropertyArrayList props = (PropertyArrayList) Gateway
					.getMarshaller().unmarshall(propString);
			for (Property thisProp : props.list)
				mStorage.put(mItemPath, thisProp, locker);
		} catch (Throwable ex) {
			Logger.msg(8, "ItemImplementation::initialise(" + mItemPath
					+ ") - Properties were invalid: " + propString);
			Logger.error(ex);
			mStorage.abort(locker);
			throw new InvalidDataException("Properties were invalid");
		}

		// Store an event and the initial properties
		try {
			Outcome initOutcome = new Outcome(0, propString, "ItemInitialization", 0);
	        History hist = new History(mItemPath, locker);
	        Transition predefDone = new Transition(0, "Done", 0, 0);
	        Event newEvent = hist.addEvent(new AgentPath(agentId), "", "Initialize", "", "", initOutcome.getSchemaType(), 0, "PredefinedStep", 0, predefDone, "last");
	        initOutcome.setID(newEvent.getID());
	        Viewpoint newLastView = new Viewpoint(mItemPath, initOutcome.getSchemaType(), "last", 0, newEvent.getID());
	        Gateway.getStorage().put(mItemPath, initOutcome, locker);
	        Gateway.getStorage().put(mItemPath, newLastView, locker);
		} catch (Throwable ex) {
			Logger.msg(8, "ItemImplementation::initialise(" + mItemPath
					+ ") - Could not store event and outcome.");
			Logger.error(ex);
			mStorage.abort(locker);
			throw new PersistencyException("Error storing event and outcome");
		}

		// init collections
		if (initCollsString != null && initCollsString.length() > 0) {
			try {
				CollectionArrayList colls = (CollectionArrayList) Gateway
						.getMarshaller().unmarshall(initCollsString);
				for (Collection<?> thisColl : colls.list) {
					mStorage.put(mItemPath, thisColl, locker);
				}
			} catch (Throwable ex) {
				Logger.msg(8, "ItemImplementation::initialise(" + mItemPath
						+ ") - Collections were invalid: "
						+ initCollsString);
				Logger.error(ex);
				mStorage.abort(locker);
				throw new InvalidDataException("Collections were invalid");
			}
		}
		
		// create wf
		Workflow lc = null;
		try {
			if (initWfString == null || initWfString.length() == 0)
				lc = new Workflow(new CompositeActivity(), getNewPredefStepContainer());
			else
				lc = new Workflow((CompositeActivity) Gateway
						.getMarshaller().unmarshall(initWfString), getNewPredefStepContainer());
			mStorage.put(mItemPath, lc, locker);
		} catch (Throwable ex) {
			Logger.msg(8, "ItemImplementation::initialise(" + mItemPath
					+ ") - Workflow was invalid: " + initWfString);
			Logger.error(ex);
			mStorage.abort(locker);
			throw new InvalidDataException("Workflow was invalid");
		}
		
		mStorage.commit(locker);
		
		// All objects are in place, initialize the workflow to get the Item running
		lc.initialise(mItemPath, agentPath);
		mStorage.put(mItemPath, lc, null);
		
		Logger.msg(3, "Initialisation of item " + mItemPath
				+ " was successful");
	}


	protected PredefinedStepContainer getNewPredefStepContainer() {
		
		return new ItemPredefinedStepContainer();
	}

	@Override
	public String requestAction(SystemKey agentId, String stepPath, int transitionID,
			String requestData) throws AccessRightsException,
			InvalidTransitionException, ObjectNotFoundException,
			InvalidDataException, PersistencyException,
			ObjectAlreadyExistsException, InvalidCollectionModification {

		try {
			
			AgentPath agent = new AgentPath(agentId);
			Logger.msg(1, "ItemImplementation::request(" + mItemPath + ") - Transition "
					+ transitionID + " on " + stepPath + " by " + agent);

			
			Workflow lifeCycle = (Workflow) mStorage.get(mItemPath,
					ClusterStorage.LIFECYCLE + "/workflow", null);

			String finalOutcome = lifeCycle.requestAction(agent, stepPath, mItemPath,
					transitionID, requestData);

			// store the workflow if we've changed the state of the domain
			// wf
			if (!(stepPath.startsWith("workflow/predefined")))
				mStorage.put(mItemPath, lifeCycle, null);
			//remove entity path if transaction was successful
			if (stepPath.equals("workflow/predefined/Erase")) { 
				Logger.msg("Erasing item path "+mItemPath.toString());
		        Gateway.getLookupManager().delete(mItemPath);
			}

			return finalOutcome;
			// Normal operation exceptions
		} catch (AccessRightsException ex) {
			Logger.msg("Propagating AccessRightsException back to the calling agent: "+ex.getMessage());
			throw ex;
		} catch (InvalidTransitionException ex) {
			Logger.msg("Propagating InvalidTransitionException back to the calling agent: "+ex.getMessage());
			throw ex;
		} catch (ObjectNotFoundException ex) {
			Logger.msg("Propagating ObjectNotFoundException back to the calling agent: "+ex.getMessage());
			throw ex;
			// errors
		} catch (InvalidItemPathException ex) {
			throw new AccessRightsException("Invalid Agent Id: " + agentId);
		} catch (InvalidDataException ex) {
			Logger.msg("Propagating InvalidDataException back to the calling agent: "+ex.getMessage());
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			Logger.msg("Propagating ObjectAlreadyExistsException back to the calling agent: "+ex.getMessage());
			throw ex;
		} catch (InvalidCollectionModification ex) {
			Logger.msg("Propagating InvalidCollectionModification back to the calling agent: "+ex.getMessage());
			throw ex;
		} catch (Throwable ex) { // non-CORBA exception hasn't been caught!
			Logger.error("Unknown Error: requestAction on " + mItemPath
					+ " by " + agentId + " executing " + stepPath);
			Logger.error(ex);
			throw new InvalidDataException(
					"Extraordinary Exception during execution:"
							+ ex.getClass().getName() + " - "
							+ ex.getMessage());
		}
	}

	@Override
	public String queryLifeCycle(SystemKey agentId, boolean filter)
			throws AccessRightsException, ObjectNotFoundException,
			PersistencyException {
		Logger.msg(1, "ItemImplementation::queryLifeCycle(" + mItemPath
				+ ") - agent: " + agentId);
		try {
			AgentPath agent;
			try {
				agent = new AgentPath(agentId);
			} catch (InvalidItemPathException e) {
				throw new AccessRightsException("Agent " + agentId
						+ " doesn't exist");
			}
			Workflow wf;
			wf = (Workflow) mStorage.get(mItemPath,
					ClusterStorage.LIFECYCLE + "/workflow", null);
			JobArrayList jobBag = new JobArrayList();
			CompositeActivity domainWf = (CompositeActivity) wf
					.search("workflow/domain");
			jobBag.list = filter ? 
					domainWf.calculateJobs(agent, mItemPath, true) : 
					domainWf.calculateAllJobs(agent, mItemPath, true);
			Logger.msg(1, "ItemImplementation::queryLifeCycle(" + mItemPath
					+ ") - Returning " + jobBag.list.size() + " jobs.");
			try {
				return Gateway.getMarshaller().marshall(jobBag);
			} catch (Exception e) {
				Logger.error(e);
				throw new PersistencyException("Error marshalling job bag");
			}
		} catch (Throwable ex) {
			Logger.error("ItemImplementation::queryLifeCycle(" + mItemPath
					+ ") - Unknown error");
			Logger.error(ex);
			throw new PersistencyException(
					"Unknown error querying jobs. Please see server log.");
		}
	}

	@Override
	public String queryData(String path) throws AccessRightsException,
			ObjectNotFoundException, PersistencyException {

		String result = "";

		Logger.msg(1, "ItemImplementation::queryData(" + mItemPath + ") - "
				+ path);

		try { // check for cluster contents query

			if (path.endsWith("/all")) {
				int allPos = path.lastIndexOf("all");
				String query = path.substring(0, allPos);
				String[] ids = mStorage.getClusterContents(mItemPath, query);

				for (int i = 0; i < ids.length; i++) {
					result += ids[i];

					if (i != ids.length - 1)
						result += ",";
				}
			}
			// ****************************************************************
			else { // retrieve the object instead
				C2KLocalObject obj = mStorage.get(mItemPath, path, null);

				// marshall it, or in the case of an outcome get the data.
				result = Gateway.getMarshaller().marshall(obj);
			}
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (Throwable ex) {
			Logger.warning("ItemImplementation::queryData(" + mItemPath
					+ ") - " + path + " Failed: " + ex.getClass().getName());
			throw new PersistencyException("Server exception: "
					+ ex.getClass().getName());
		}

		if (Logger.doLog(9))
			Logger.msg(9, "ItemImplementation::queryData(" + mItemPath
					+ ") - result:" + result);

		return result;
	}
	
    /**
    *
    */
   @Override
	protected void finalize() throws Throwable {
       Logger.msg(7, "Item "+mItemPath+" reaped");
       Gateway.getStorage().clearCache(mItemPath, null);
       super.finalize();
   }
}
