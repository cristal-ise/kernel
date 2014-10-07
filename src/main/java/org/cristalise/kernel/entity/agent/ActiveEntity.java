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
package org.cristalise.kernel.entity.agent;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.entity.AgentPOA;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 * ActiveEntity - the CORBA object representing the Agent. All functionality
 * is delegated to the AgentImplementation, which extends ItemImplementation,
 * as this cannot extend its equivalent TraceableEntity
 * 
 **************************************************************************/
public class ActiveEntity extends AgentPOA
{

    private final org.omg.PortableServer.POA  mPoa;
	private final AgentImplementation mAgentImpl; 
	
    public ActiveEntity( AgentPath                   key,
                         org.omg.PortableServer.POA  poa ) 
    {
        Logger.msg(5, "ActiveEntity::constructor() - SystemKey:" + key );
        mPoa	= poa;	
        mAgentImpl = new AgentImplementation(key);
    }


   /**************************************************************************
    *
    *
    **************************************************************************/
    @Override
	public org.omg.PortableServer.POA _default_POA()
    {
        if(mPoa != null)
            return mPoa;
        else
            return super._default_POA();
    }


   /**************************************************************************
    *
    *
    **************************************************************************/
    @Override
	public SystemKey getSystemKey()
    {
    	return mAgentImpl.getSystemKey();
    }


   /**************************************************************************
    *
    *
    **************************************************************************/
    @Override
	public String queryData(String path)
        throws AccessRightsException,
               ObjectNotFoundException,
               PersistencyException
    {
        synchronized (this) {
			return mAgentImpl.queryData(path);
		}
    }



    /**
     * Called by an activity when it reckons we need to update our joblist for it
     */

    @Override
	public void refreshJobList(SystemKey sysKey, String stepPath, String newJobs) {
        synchronized (this) {
			mAgentImpl.refreshJobList(sysKey, stepPath, newJobs);
		}
    }

    @Override
	public void addRole(String roleName) throws CannotManageException, ObjectNotFoundException {
    	synchronized (this) {
    		mAgentImpl.addRole(roleName);
    	}
    }

    @Override
	public void removeRole(String roleName) throws CannotManageException, ObjectNotFoundException {
    	synchronized (this) {
    		mAgentImpl.removeRole(roleName);
    	}
    }
    
	@Override
	public void initialise(SystemKey agentId, String propString, String initWfString,
			String initCollsString) throws AccessRightsException,
			InvalidDataException, PersistencyException, ObjectNotFoundException {
		synchronized (this) {
			mAgentImpl.initialise(agentId, propString, initWfString, initCollsString);
		}
		
	}

	@Override
	public String requestAction(SystemKey agentID, String stepPath, int transitionID,
			String requestData) throws AccessRightsException,
			InvalidTransitionException, ObjectNotFoundException,
			InvalidDataException, PersistencyException,
			ObjectAlreadyExistsException, InvalidCollectionModification {
		
		synchronized (this) {
            return mAgentImpl.requestAction(agentID, stepPath, transitionID, requestData);
        }
		
	}

	@Override
	public String queryLifeCycle(SystemKey agentId, boolean filter)
			throws AccessRightsException, ObjectNotFoundException,
			PersistencyException {
		synchronized (this) {
			return mAgentImpl.queryLifeCycle(agentId, filter);
		}
	}
}
