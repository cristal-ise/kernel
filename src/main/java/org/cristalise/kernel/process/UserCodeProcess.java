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
package org.cristalise.kernel.process;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.scripting.ScriptErrorException;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * $Revision: 1.31 $
 * $Date: 2004/10/21 08:02:19 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/
public class UserCodeProcess extends StandardClient implements ProxyObserver<Job>, Runnable {
	
	// Default state machine transitions
    private static final int START = 1;
	private static final int COMPLETE = 2;
	private static final int SUSPEND = 3;
	private static final int RESUME = 4;
	
	protected AgentProxy agent;
    static boolean active = true;
    ArrayList<String> ignoredPaths = new ArrayList<String>();
    HashMap<String, ErrorInfo> errors = new HashMap<String, ErrorInfo>();
    final HashMap<String, C2KLocalObject> jobs = new HashMap<String, C2KLocalObject>();

    public UserCodeProcess(String agentName, String agentPass, String resource) {
        // login - try for a while in case server hasn't imported our user yet
        for (int i=1;i<6;i++) {
            try {
                Logger.msg("Login attempt "+i+" of 5");
                agent = Gateway.connect(agentName, agentPass, resource);
                break;
            } catch (Exception ex) {
                Logger.error("Could not log in.");
                Logger.error(ex);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex2) { }
            }
        }
        System.out.println(getDesc()+" initialised for " + agentName);
    }

    @Override
	public void run() {
        Thread.currentThread().setName("Usercode Process");
        // subscribe to job list
        agent.subscribe(new MemberSubscription<Job>(this, ClusterStorage.JOB, true));
        while (active) {
            Job thisJob = null;
            synchronized (jobs) {
                if (jobs.size() > 0) {
                    thisJob = getJob(jobs, COMPLETE);
                    if (thisJob == null)
                        thisJob = getJob(jobs, START);
                    if (thisJob == null)
                        thisJob = getJob(jobs, SUSPEND);
                    if (thisJob == null)
                        thisJob = getJob(jobs, RESUME);

                    if (thisJob == null) {
                        Logger.error("No supported jobs, but joblist is not empty! Discarding remaining jobs");
                        jobs.clear();
                    }
                    else
                        jobs.remove(ClusterStorage.getPath(thisJob));
                }
            }

            if (thisJob != null) {
                String jobKey = thisJob.getItemPath()+":"+thisJob.getStepPath();
                int transitionId = thisJob.getTransition().getId();
                try {
                    if (transitionId==START) {
                        Logger.msg(5, "Testing start conditions");
                        boolean start = assessStartConditions(thisJob);
                        if (start) {
                            Logger.msg(5, "Attempting to start");
                            agent.execute(thisJob);
                        }
                        else {
                            Logger.msg(5, "Start conditions failed "+thisJob.getStepName()+" in "+thisJob.getItemPath());
                        }
                    }
                    else if (transitionId==COMPLETE) {
                        Logger.msg(5, "Executing logic");
                        runUCLogic(thisJob);
                        if (ignoredPaths.contains(jobKey))
                            ignoredPaths.remove(jobKey);
                    }
                    else if (transitionId==SUSPEND) {
                        if (ignoredPaths.contains(jobKey)) {
                        	if (errors.containsKey(jobKey)) {
                        		thisJob.setOutcome(Gateway.getMarshaller().marshall(errors.get(jobKey)));
                        		errors.remove(jobKey);
                        	}
                            agent.execute(thisJob);
                        }
                    }
                    else if (transitionId==RESUME) {
                        if (!ignoredPaths.contains(jobKey))
                            agent.execute(thisJob);
                    }
                } catch (ScriptErrorException ex) {
                	errors.put(jobKey, ex.getErrors());
                	ignoredPaths.add(jobKey);
                } catch (InvalidTransitionException ex) {
                    // must have already been done by someone else - ignore
                } catch (Throwable ex) {
                    Logger.error("Error executing "+thisJob.getTransition().getName()+" job:");
                    Logger.error(ex);
                    ErrorInfo ei = new ErrorInfo();
                    ei.setFatal();
                    ei.addError(ex.getClass().getSimpleName());
                    ei.addError(ex.getMessage());
                    errors.put(jobKey, ei);
                    ignoredPaths.add(jobKey);
                }
            }
            try {
                synchronized (jobs) {
                    if (jobs.size() == 0) {
                        Logger.msg("Sleeping");
                            while (active && jobs.size() == 0)
                                jobs.wait(2000);
                        }
                    }
            } catch (InterruptedException ex) { }
        }

        // shut down
        try
        {
        	Gateway.close();
        }
        catch( Exception ex )
        {
            Logger.error(ex);
        }
    }

	private static Job getJob(HashMap<String, C2KLocalObject> jobs, int transition) {
        for (C2KLocalObject c2kLocalObject : jobs.values()) {
            Job thisJob = (Job)c2kLocalObject;
            if (thisJob.getTransition().getId() == transition) {
                Logger.msg(1,"=================================================================");
                Logger.msg(1, "Got "+thisJob.getTransition().getName()+" job for "+thisJob.getStepName()+" in "+thisJob.getItemPath());
                return thisJob;
            }
        }
        return null;
    }

    public boolean assessStartConditions(Job job) {
        // default implementation - has no start conditions.
        return true;
    }

    public void runUCLogic(Job job) throws Exception {
        // default implementation - the agent will execute any scripts defined when we execute
        agent.execute(job);
    }


    /**
     * Receives job from the AgentProxy. Reactivates thread if sleeping.
    */
    @Override
	public void add(Job contents) {
            synchronized(jobs) {
                Logger.msg(7, "Adding "+ClusterStorage.getPath(contents));
                jobs.put(ClusterStorage.getPath(contents), contents);
                jobs.notify();
            }

    }
    
	@Override
	public void control(String control, String msg) {
		if (MemberSubscription.ERROR.equals(control))
			Logger.error("Error in job subscription: "+msg);
	}

    /**
    * Removes job removal notification from the AgentProxy.
    */
    @Override
	public void remove(String id) {
        synchronized(jobs) {
            Logger.msg(7, "Deleting "+id);
            jobs.remove(id);
        }
    }

    public static UserCodeProcess getInstance() throws UnknownHostException {
        return new UserCodeProcess(InetAddress.getLocalHost().getHostName(), "uc", Gateway.getProperties().getString("AuthResource", "Cristal"));
    }

    static public void main(String[] args)
    {
        int status = 0;

        try
        {
        	Gateway.init(readC2KArgs(args));
            UserCodeProcess proc = getInstance();
            new Thread(proc).start();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
				public void run() {
                    shutdown();
                }
            }));
        }
        catch( Exception ex )
        {
            Logger.error(ex);

            try
            {
            	Gateway.close();
            }
            catch(Exception ex1)
            {
                Logger.error(ex1);
            }
            status = 1;
            System.exit(status);
        }
    }

    public String getDesc() {
        return("Usercode Process");
    }

    public static void shutdown() {
        active = false;
    }

}
