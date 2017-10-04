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
package org.cristalise.kernel.entity.agent;

import java.util.List;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.entity.AgentOperations;
import org.cristalise.kernel.entity.ItemImplementation;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.AgentPredefinedStepContainer;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


/**
 * Implementation of Agent, though called from the CORBA implementation ActiveEntity.
 *
 * <p>The Agent is an extension of the Item that can execute Jobs, and in doing so
 * change the state of Item workflows, submit data to them in the form of Outcomes
 * and run any scripts associated with those activities. In this server object,
 * none of this specific Agent work is performed - it all must be done using the
 * client API. The server implementation only manages the Agent's data: its roles
 * and persistent Jobs.
 */
public class AgentImplementation extends ItemImplementation implements AgentOperations {

    private JobList currentJobs;

    public AgentImplementation(AgentPath path) {
        super(path);

        try {
            currentJobs = (JobList) mStorage.get(path, ClusterType.JOB.getName(), path);
        }
        catch (PersistencyException | ObjectNotFoundException ex) {
            Logger.error(ex);
        }
    }

    /**
     * Updates an Agent's list of Jobs relating to a particular activity. Only
     * Activities that are assigned to a Role that is flagged to push Jobs do this.
     *
     */
    @Override
    public synchronized void refreshJobList(SystemKey sysKey, String stepPath, String newJobs) {
        try {
            ItemPath itemPath = new ItemPath(sysKey);
            JobArrayList newJobList = (JobArrayList)Gateway.getMarshaller().unmarshall(newJobs);

            List<String> keysToRemove = currentJobs.getKeysForStep(itemPath, stepPath);

            // merge new jobs in first, so the RemoteMap.getLastId() used during addJob() returns the next unique id
            for (Job newJob : newJobList.list) {
                Logger.msg(6, "AgentImplementation.refreshJobList() - Adding job:"+newJob.getItemPath()+"/"+newJob.getStepPath()+":"+newJob.getTransition().getName());
                currentJobs.addJob(newJob);
            }

            // remove old jobs for this item0
            for(String key: keysToRemove) currentJobs.remove(key);

            Gateway.getStorage().commit(mItemPath);
        }
        catch (Throwable ex) {
            Logger.error("Could not refresh job list.");
            Logger.error(ex);
            Gateway.getStorage().abort(mItemPath);
        }
    }

    /** Adds the given Role to this Agent. Called from the SetAgentRoles predefined step.
     *
     * @param roleName - the new Role to add
     * @throws CannotManageException When the process has no lookup manager
     * @throws ObjectNotFoundException Role does not exists
     *
     */
    @Override
    public void addRole(String roleName) throws CannotManageException, ObjectNotFoundException {
        RolePath newRole = Gateway.getLookup().getRolePath(roleName);
        try {
            Gateway.getLookupManager().addRole((AgentPath)mItemPath, newRole);
        }
        catch (ObjectCannotBeUpdated ex) {
            throw new CannotManageException("Could not update role");
        }
    }

    /**
     * Removes the given Role from this Agent. Called by the SetAgentRoles
     * predefined step.
     *
     * @param roleName the Name of the Role
     */
    @Override
    public void removeRole(String roleName) throws CannotManageException, ObjectNotFoundException {
        RolePath rolePath = Gateway.getLookup().getRolePath(roleName);
        try {
            Gateway.getLookupManager().removeRole((AgentPath)mItemPath, rolePath);
        }
        catch (ObjectCannotBeUpdated ex) {
            throw new CannotManageException("Could not update role");
        }
    }

    /**
     * Agents have their own predefined step containers. They contain the standard
     * predefined steps, plus special Agent ones related to Agent management and
     * instantiation.
     *
     * @see org.cristalise.kernel.lifecycle.instance.predefined.agent.AgentPredefinedStepContainer
     */
    @Override
    protected PredefinedStepContainer getNewPredefStepContainer() {
        return new AgentPredefinedStepContainer();
    }

    @Override
    protected void finalize() throws Throwable {
        Logger.msg(7, "AgentImplementation.finalize() - Reaping " + mItemPath);
        if (currentJobs != null) currentJobs.deactivate();
        Gateway.getStorage().clearCache(mItemPath, null);
        super.finalize();
    }
}
