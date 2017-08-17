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

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
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
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.scripting.ScriptErrorException;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

/**
 * 
 */
public class UserCodeProcess extends StandardClient implements ProxyObserver<Job>, Runnable {

    private final int START;
    private final int COMPLETE;
    private final int SUSPEND;
    private final int RESUME;

    static boolean                        active       = true;
    ArrayList<String>                     ignoredPaths = new ArrayList<String>();
    HashMap<String, ErrorInfo>            errors       = new HashMap<String, ErrorInfo>();
    final HashMap<String, C2KLocalObject> jobs         = new HashMap<String, C2KLocalObject>();

    public UserCodeProcess() throws InvalidDataException, ObjectNotFoundException {
        StateMachine sm = getRequiredStateMachine("UserCode", null, "boot/SM/Default.xml");

        //default values are valid for Transitions compatible with kernel provided Default StateMachine
        START    = getValidTransitionID(sm, "UserCode.StateMachine.startTransition",    "Start");
        COMPLETE = getValidTransitionID(sm, "UserCode.StateMachine.completeTransition", "Complete");
        SUSPEND  = getValidTransitionID(sm, "UserCode.StateMachine.suspendTransition",  "Suspend");
        RESUME   = getValidTransitionID(sm, "UserCode.StateMachine.resumeTransition",   "Resume");
    }

    private int getValidTransitionID(StateMachine sm, String propertyName, String defaultValue) throws InvalidDataException {
        String propertyValue = Gateway.getProperties().getString(propertyName, defaultValue);

        if("USERCODE_IGNORE".equals(propertyValue)) return -1;
        else                                        return sm.getValidTransitionID(propertyValue);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Usercode Process");
        // subscribe to job list
        agent.subscribe(new MemberSubscription<Job>(this, ClusterType.JOB.getName(), true));
        while (active) {
            Job thisJob = getActualJob();

            if (thisJob != null) {
                String jobKey = thisJob.getItemPath()+":"+thisJob.getStepPath();
                int transitionId = thisJob.getTransition().getId();

                try {
                    if      (transitionId==START)    start(thisJob, jobKey);
                    else if (transitionId==COMPLETE) complete(thisJob, jobKey);
                    else if (transitionId==SUSPEND)  suspend(thisJob, jobKey);
                    else if (transitionId==RESUME)   resume(thisJob, jobKey);
                }
                catch (ScriptErrorException ex) {
                    errors.put(jobKey, ex.getErrors());
                    ignoredPaths.add(jobKey);
                }
                catch (InvalidTransitionException ex) {
                    // must have already been done by someone else - ignore
                }
                catch (Throwable ex) {
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
     * Method called to handle the Start transition
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
     * Method called to handle the Complete transition
     * 
     * @param thisJob the actual Job to be executed.
     * @param jobKey the key of the job (i.e. itemPath:stepPat)
     */
    public void complete(Job thisJob, String jobKey) throws Exception {
        Logger.msg(5, "UserCodeProcess.complete() - job:"+thisJob);
        runUserCodeLogic(thisJob);

        if (ignoredPaths.contains(jobKey)) ignoredPaths.remove(jobKey);
    }

    /**
     * Method called to handle the Resume transition
     * 
     * @param thisJob the actual Job to be executed.
     * @param jobKey the key of the job (i.e. itemPath:stepPat)
     */
    public void resume(Job thisJob, String jobKey)
            throws AccessRightsException, InvalidDataException, InvalidTransitionException, ObjectNotFoundException, PersistencyException,
                   ObjectAlreadyExistsException, ScriptErrorException, InvalidCollectionModification
    {
        Logger.msg(5, "UserCodeProcess.resume() - job:"+thisJob);

        if (!ignoredPaths.contains(jobKey)) agent.execute(thisJob);
    }

    /**
     * Method called to handle the Suspend transition
     * 
     * @param thisJob the actual Job to be executed.
     * @param jobKey the key of the job (i.e. itemPath:stepPat)
     */
    public void suspend(Job thisJob, String jobKey) 
            throws MarshalException, ValidationException, InvalidDataException, ObjectNotFoundException, IOException, MappingException, 
                   AccessRightsException, InvalidTransitionException, PersistencyException, ObjectAlreadyExistsException, 
                   InvalidCollectionModification, ScriptErrorException 
    {
        Logger.msg(5, "UserCodeProcess.suspend() - job:"+thisJob);

        if (ignoredPaths.contains(jobKey)) {
            if (errors.containsKey(jobKey)) {
                thisJob.setOutcome(Gateway.getMarshaller().marshall(errors.get(jobKey)));
                errors.remove(jobKey);
            }
            agent.execute(thisJob);
        }
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
     */
    public void runUserCodeLogic(Job job)
            throws AccessRightsException, InvalidDataException, InvalidTransitionException, ObjectNotFoundException, PersistencyException,
                   ObjectAlreadyExistsException, InvalidCollectionModification, ScriptErrorException 
    {
        agent.execute(job);
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
                if (thisJob == null) thisJob = getJob(jobs, SUSPEND);
                if (thisJob == null) thisJob = getJob(jobs, RESUME);

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
    public void add(Job contents) {
        synchronized(jobs) {
            Logger.msg(7, "UserCodeProcess.add() - path:"+ClusterStorage.getPath(contents));
            jobs.put(ClusterStorage.getPath(contents), contents);
            jobs.notify();
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
            Logger.msg(7, "UserCodeProcess.remove() - id:"+id);
            jobs.remove(id);
        }
    }

    public String getDesc() {
        return("Usercode Process");
    }

    public static void shutdown() {
        active = false;
    }

    static public void main(String[] args) {
        int status = 0;

        try {
            Gateway.init(readC2KArgs(args));

            UserCodeProcess proc =  new UserCodeProcess();

            proc.login( Gateway.getProperties().getString("UserCode.agent", InetAddress.getLocalHost().getHostName()),
                        Gateway.getProperties().getString("UserCode.password", "uc"),
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
