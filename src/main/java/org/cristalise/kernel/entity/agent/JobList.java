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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.RemoteMap;
import org.cristalise.kernel.utils.Logger;

import static org.cristalise.kernel.persistency.ClusterType.JOB;

public class JobList extends RemoteMap<Job> {

    /**
     * 
     */
    private static final long serialVersionUID = -1110616958817712975L;

    public JobList(AgentPath agentPath, Object locker) {
        super(agentPath, JOB.getName(), locker);
    }

    public void addJob(Job job) {
        synchronized (this) {
            int jobId = getLastId() + 1;
            job.setId(jobId);
            put(String.valueOf(jobId), job);
        }
    }

    @Override
    public ClusterType getClusterType() {
        return null;
    }

    public Job getJob(int id) {
        return get(String.valueOf(id));
    }

    public void removeJobsForItem(ItemPath itemPath) {
        Iterator<Job> currentMembers = values().iterator();
        Job j = null;

        while (currentMembers.hasNext()) {
            j = currentMembers.next();

            if (j.getItemPath().equals(itemPath)) remove(String.valueOf(j.getId()));
        }

        Logger.msg(5, "JobList::removeJobsWithSysKey() - " + itemPath + " DONE.");
    }

    /**
     * Find the list of JobKeys for the given Item and its Step
     * 
     * @param itemPath the ItemPath (uuid)
     * @param stepPath the Step path 
     * @return the list of JobKeys mathcing the inputs
     */
    public List<String> getKeysForStep(ItemPath itemPath, String stepPath) {
        List<String> jobKeys = new ArrayList<String>();
        Iterator<String> jobIter = keySet().iterator();

        while (jobIter.hasNext()) {
            String jid = jobIter.next();
            Job j = get(jid);
            if (j.getItemPath().equals(itemPath) && j.getStepPath().equals(stepPath)) jobKeys.add(jid);
        }
        return jobKeys;
    }

    public void removeJobsForStep(ItemPath itemPath, String stepPath) {
        List<String> staleJobs = getKeysForStep(itemPath, stepPath);

        Logger.msg(3, "JobList.removeJobsForStep() - removing " + staleJobs.size());

        for (String jid : staleJobs) remove(jid);

        Logger.msg(5, "JobList::removeJobsForStep() - " + itemPath + " DONE.");
    }

    public Vector<Job> getJobsOfItem(ItemPath itemPath) {
        Iterator<Job> currentMembers = values().iterator();
        Job j = null;
        Vector<Job> jobs = new Vector<Job>();

        while (currentMembers.hasNext()) {
            j = currentMembers.next();

            if (j.getItemPath().equals(itemPath)) jobs.add(j);
        }

        Logger.msg(5, "JobList::getJobsOfSysKey() - returning " + jobs.size() + " Jobs.");

        return jobs;
    }

    public void dump(int logLevel) {
        if (!Logger.doLog(logLevel)) return;

        StringBuffer sb = new StringBuffer("{ ");

        Iterator<String> jobIter = keySet().iterator();

        while (jobIter.hasNext()) {
            String jid = jobIter.next();
            Job j = get(jid);
            sb.append("[id:"+jid+" ");
            sb.append("step:"+j.getStepName()+" ");
            sb.append("role:"+j.getAgentRole()+" ");
            sb.append("trans:"+j.getTransition().getName()+"] ");
        }
        sb.append("}");
        Logger.msg("Joblist "+sb);
    }
}