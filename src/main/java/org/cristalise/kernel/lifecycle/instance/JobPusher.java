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

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.Agent;
import org.cristalise.kernel.entity.AgentHelper;
import org.cristalise.kernel.entity.agent.JobArrayList;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

final class JobPusher extends Thread {
    private final Activity activity;
    private final RolePath myRole;
    private final ItemPath itemPath;

    JobPusher(Activity activity, ItemPath itemPath, RolePath role) {
        this.activity = activity;
        this.itemPath = itemPath;
        this.myRole = role;
    }

    @Override
    public void run() {
        String tName = "Agent job pusher for "+itemPath+":"+activity.getPath()+" to role "+myRole;
        Thread.currentThread().setName(tName);

        Logger.msg(7, "JobPusher.run() - Started:"+tName);

        try {
            for (AgentPath nextAgent: Gateway.getLookup().getAgents(myRole)) {
                Logger.msg(7, "JobPusher.run() - Calculating jobs for agent:" + nextAgent);

                try {
                    // get joblist for agent
                    JobArrayList jobList = new JobArrayList(this.activity.calculateJobs(nextAgent, itemPath, false));
                    if (jobList.list.size() !=  0) {
                        // push it to the agent
                        String stringJobs = Gateway.getMarshaller().marshall(jobList);
                        Agent thisAgent = AgentHelper.narrow( nextAgent.getIOR() );

                        Logger.msg(7, "JobPusher.run() - Calling refreshJobList() with "+jobList.list.size()+" jobs for agent "+nextAgent+" from "+activity.getPath());
                        thisAgent.refreshJobList(itemPath.getSystemKey(), activity.getPath(), stringJobs);
                    }
                    else {
                        Logger.msg(7, "JobPusher.run() - NO jobs to send to agent "+nextAgent+" from "+activity.getPath());
                    }
                }
                catch (Exception ex) {
                    Logger.error("JobPusher.run() - Agent " + nextAgent + " of role " + myRole
                        + " could not be found to be informed of a change in " + itemPath);
                    Logger.error(ex);
                }
            }
        }
        catch (ObjectNotFoundException e) {
            Logger.warning("JobPusher cannot push jobs, it did not find any agents for role:"+myRole);
        }
        Logger.msg(7, "JobPusher.run() - FINISHED:"+tName);
    }
}
