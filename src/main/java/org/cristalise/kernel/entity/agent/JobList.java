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
import java.util.Vector;

import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.RemoteMap;
import org.cristalise.kernel.utils.Logger;

public class JobList extends RemoteMap<Job> {

    /**
     * 
     */
    private static final long serialVersionUID = -1110616958817712975L;

    public JobList(AgentPath agentPath, Object locker) {
        super(agentPath, ClusterStorage.JOB, locker);
    }

    public void addJob(Job job) {
        synchronized (this) {
            int jobId = getLastId() + 1;
            job.setId(jobId);
            put(String.valueOf(jobId), job);
        }
    }

    @Override
    public String getClusterType() {
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

    public void removeJobsForStep(ItemPath itemPath, String stepPath) {
        ArrayList<String> staleJobs = new ArrayList<String>();
        Iterator<String> jobIter = keySet().iterator();

        while (jobIter.hasNext()) {
            String jid = jobIter.next();
            Job j = get(jid);
            if (j.getItemPath().equals(itemPath) && j.getStepPath().equals(stepPath)) staleJobs.add(jid);
        }

        Logger.msg(3, "JobList.removeJobsForStep() - removing " + staleJobs.size());

        for (String jid : staleJobs) {
            remove(jid);
        }

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
}