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
import java.util.HashMap;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.scripting.ScriptErrorException;
import org.cristalise.kernel.utils.Logger;

/**
 * UserCodeProcess provides a very basic automatic execution of Scripts associated with the Jobs (Activities).
 * It is based on the Default StateMachine, and it implements the following sequence:
 * <pre>
 * 1. assessStartConditions()
 * 2. start()
 * 3. complete()
 * 4. in case of error/exception execute error transition which is suspend for default statemachine
 */
public class UserCodeProcess extends StandardClient implements ProxyObserver<Job>, Runnable {

    private final int START;
    private final int COMPLETE;
    private final int ERROR;

    /**
     * Defines the default role (value:{@value}). It also used as a prefix for every configuration property
     * eg: UserCode.StateMachine.startTransition
     */
    public static final String DEFAULT_ROLE = "UserCode";
    /**
     * Defines the name of the CRISTAL Property (value:{@value}) to override the default mapping for Start transition.
     * It is always prefixed like this: eg: UserCode.StateMachine.startTransition
     */
    public static final String STATE_MACHINE_START_TRANSITION = "StateMachine.startTransition";
    /**
     * Defines the name of the CRISTAL Property (value:{@value}) to override the default mapping for Complete transition.
     * It is always prefixed like this: eg: UserCode.StateMachine.completeTransition
     */
    public static final String STATE_MACHINE_COMPLETE_TRANSITION = "StateMachine.completeTransition";
    /**
     * Defines the name of the CRISTAL Property (value:{@value}) to override the default mapping for Error transition.
     * It is always prefixed like this: eg: UserCode.StateMachine.errorTransition
     */
    public static final String STATE_MACHINE_ERROR_TRANSITION = "StateMachine.errorTransition";

    /**
     * Defines the value (value:{@value}) to to be used in CRISTAL Property to ignore the Jobs of that Transition
     * eg: UserCode.StateMachine.resumeTransition = USERCODE_IGNORE
     */
    public static final String USERCODE_IGNORE = "USERCODE_IGNORE";

    protected static boolean                        active = true;
    protected final HashMap<String, C2KLocalObject> jobs   = new HashMap<String, C2KLocalObject>();

    /**
     * Default constructor set up the user code with default setting
     *
     * @throws InvalidDataException incorrect configration
     * @throws ObjectNotFoundException StateMachine to configure the UserCode was not found
     */
    public UserCodeProcess() throws InvalidDataException, ObjectNotFoundException {
        this(DEFAULT_ROLE);
    }

    /**
     * Constructor set up the user code
     *
     * @param propPrefix string to be used as prefix for each property
     *
     * @throws InvalidDataException StateMachine does not have the named Transition
     * @throws ObjectNotFoundException StateMachine was not found
     */
    public UserCodeProcess(String propPrefix) throws InvalidDataException, ObjectNotFoundException {
        if (propPrefix == null) propPrefix = DEFAULT_ROLE;

        StateMachine sm = getRequiredStateMachine(propPrefix, null, "boot/SM/Default.xml");

        //default values are valid for Transitions compatible with kernel provided Default StateMachine
        START    = getValidTransitionID(sm, propPrefix+"."+STATE_MACHINE_START_TRANSITION,    "Start");
        ERROR    = getValidTransitionID(sm, propPrefix+"."+STATE_MACHINE_ERROR_TRANSITION,    "Suspend");
        COMPLETE = getValidTransitionID(sm, propPrefix+"."+STATE_MACHINE_COMPLETE_TRANSITION, "Complete");
    }

    /**
     *
     * @param sm
     * @param propertyName
     * @param defaultValue
     * @return
     * @throws InvalidDataException
     */
    private int getValidTransitionID(StateMachine sm, String propertyName, String defaultValue) throws InvalidDataException {
        String propertyValue = Gateway.getProperties().getString(propertyName, defaultValue);

        if("USERCODE_IGNORE".equals(propertyValue)) return -1;
        else                                        return sm.getValidTransitionID(propertyValue);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Usercode Process");

        // subscribe to job list - this will initialise the jobs using the ProxyObserver interface as callback
        agent.subscribe(new MemberSubscription<Job>(this, ClusterType.JOB.getName(), true));

        while (active) {
            Job thisJob = getActualJob();

            if (thisJob != null) {
                String jobKey = thisJob.getItemPath()+":"+thisJob.getStepPath();
                int transitionId = thisJob.getTransition().getId();

                try {
                    if      (transitionId == START)    start(thisJob, jobKey);
                    else if (transitionId == COMPLETE) complete(thisJob, jobKey);
                }
                catch (InvalidTransitionException ex) {
                    // must have already been done by someone else - ignore
                }
                catch (Exception ex) {
                    Logger.error("Error executing job:"+thisJob);
                    Logger.error(ex);
                }
            }
            try {
                synchronized (jobs) {
                    if (jobs.size() == 0) {
                        Logger.msg("UserCodeProcess.run() - Sleeping");
                        while (active && jobs.size() == 0) jobs.wait(2000);
                    }
                }
            } catch (InterruptedException ex) { }
        }

        // shut down
        try {
            Gateway.close();
        }
        catch( Exception ex ) {
            Logger.error(ex);
        }
    }

    /**
     * Method called to handle the Start transition. Override this method to implement application specific action
     * for Jobs of Start Transition.
     *
     * @param thisJob the actual Job to be executed.
     * @param jobKey the key of the job (i.e. itemPath:stepPat)
     */
    public void start(Job thisJob, String jobKey)
            throws AccessRightsException, InvalidDataException, InvalidTransitionException, ObjectNotFoundException, PersistencyException,
            ObjectAlreadyExistsException, ScriptErrorException, InvalidCollectionModification
    {
        Logger.msg(5, "UserCodeProcess.start() - job:"+thisJob);

        if (assessStartConditions(thisJob)) {
            Logger.msg(5, "UserCodeProcess.start() - Attempting to start");
            agent.execute(thisJob);
        }
        else {
            Logger.msg(5, "UserCodeProcess.start() - Start conditions failed "+thisJob.getStepName()+" in "+thisJob.getItemPath());
        }
    }

    /**
     * Method called to handle the Complete transition. Override this method to implement application specific action
     * for Jobs of Complete Transition.
     *
     * @param thisJob the actual Job to be executed.
     * @param jobKey the key of the job (i.e. itemPath:stepPat)
     */
    public void complete(Job thisJob, String jobKey) throws Exception {
        Logger.msg(5, "UserCodeProcess.complete() - job:"+thisJob);

        runUserCodeLogic(thisJob, getErrorJob(thisJob, ERROR));
    }

    /**
     * Override this method to implement application specific evaluation of start condition.
     * Default implementation - returns always true, i.e. there were no start conditions.
     *
     * @param job the actual Job to be executed.
     * @return true, if the start condition were met
     */
    public boolean assessStartConditions(Job job) {
        return true;
    }

    /**
     * Override this mehod to implement application specific (business) logic
     * Default implementation - the agent execute any scripts, query or both defined
     *
     * @param job the actual Job to be executed.
     * @param errorJob Job to be executed in case of an error
     */
    public void runUserCodeLogic(Job job, Job errorJob)
            throws AccessRightsException, InvalidDataException, InvalidTransitionException, ObjectNotFoundException, PersistencyException,
            ObjectAlreadyExistsException, InvalidCollectionModification, ScriptErrorException
    {
        if (errorJob == null) agent.execute(job);
        else                  agent.execute(job, errorJob);
    }

    /**
     * Gets the next possible Job based on the Transitions of the Default StateMachine
     *
     * @return the actual Job
     */
    protected Job getActualJob() {
        Job thisJob = null;

        synchronized (jobs) {
            if (jobs.size() > 0) {

                thisJob = getJob(jobs, COMPLETE);
                if (thisJob == null) thisJob = getJob(jobs, START);

                if (thisJob == null) {
                    Logger.error("No supported jobs, but joblist is not empty! Discarding remaining jobs");
                    jobs.clear();
                }
                else {
                    jobs.remove(ClusterStorage.getPath(thisJob));
                }
            }
        }
        return thisJob;
    }

    /**
     *
     * @param completeJob
     * @param errorTrans
     * @return
     */
    private Job getErrorJob(Job completeJob, int errorTrans) {
        Job errorJob = null;

        synchronized (jobs) {
            for (C2KLocalObject c2kLocalObject : jobs.values()) {
                Job thisJob = (Job)c2kLocalObject;
                if (thisJob.getItemUUID().equals(completeJob.getItemUUID()) && thisJob.getTransition().getId() == errorTrans) {
                    Logger.msg(5, "UserCodeProcess.getErrorJob() - job:"+thisJob);
                    errorJob = thisJob;
                }
            }
        }

        return errorJob;
    }

    /**
     *
     * @param jobs
     * @param transition
     * @return
     */
    private static Job getJob(HashMap<String, C2KLocalObject> jobs, int transition) {
        for (C2KLocalObject c2kLocalObject : jobs.values()) {
            Job thisJob = (Job)c2kLocalObject;
            if (thisJob.getTransition().getId() == transition) {
                Logger.msg(1,"=================================================================");
                Logger.msg(5, "UserCodeProcess.getJob() - job:"+thisJob);
                return thisJob;
            }
        }
        return null;
    }


    /**
     * Receives job from the AgentProxy. Reactivates thread if sleeping.
     */
    @Override
    public void add(Job job) {
        synchronized(jobs) {
            jobs.put(job.getClusterPath(), job);
            jobs.notify();
            Logger.msg(7, "UserCodeProcess.add() - Added job:"+job);
        }
    }

    /**
     *
     */
    @Override
    public void control(String control, String msg) {
        if (MemberSubscription.ERROR.equals(control)) Logger.error("Error in job subscription: "+msg);
    }

    /**
     * Job removal notification from the AgentProxy.
     */
    @Override
    public void remove(String id) {
        synchronized(jobs) {
            Job job = (Job) jobs.remove(id);
            Logger.msg(7, "UserCodeProcess.remove() - Removed job:"+job);
        }
    }

    public String getDesc() {
        String role = Gateway.getProperties().getString("UserCode.roleOverride", UserCodeProcess.DEFAULT_ROLE);
        return("Usercode Process for role "+role);
    }

    public static void shutdown() {
        active = false;
    }

    static public void main(String[] args) {
        int status = 0;

        try {
            Gateway.init(readC2KArgs(args));

            String prefix = Gateway.getProperties().getString("UserCode.roleOverride", UserCodeProcess.DEFAULT_ROLE);

            UserCodeProcess proc =  new UserCodeProcess(prefix);

            proc.login(
                    Gateway.getProperties().getString(prefix + ".agent",    InetAddress.getLocalHost().getHostName()),
                    Gateway.getProperties().getString(prefix + ".password", "uc"),
                    Gateway.getProperties().getString("AuthResource", "Cristal"));

            new Thread(proc).start();

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    shutdown();
                }
            }));
        }
        catch( Exception ex ) {
            Logger.error(ex);

            try {
                Gateway.close();
            }
            catch(Exception ex1) {
                Logger.error(ex1);
            }
            status = 1;
            System.exit(status);
        }
    }
}
