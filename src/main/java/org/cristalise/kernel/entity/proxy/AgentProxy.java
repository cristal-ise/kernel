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
package org.cristalise.kernel.entity.proxy;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.Agent;
import org.cristalise.kernel.entity.AgentHelper;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptErrorException;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.Logger;

/******************************************************************************
 * It is a wrapper for the connection and communication with Agent It caches
 * data loaded from the Agent to reduce communication
 ******************************************************************************/
public class AgentProxy extends ItemProxy {

    AgentPath     mAgentPath;
    String        mAgentName;
    Authenticator auth;

    /**************************************************************************
     * Creates an AgentProxy without cache and change notification
     **************************************************************************/
    protected AgentProxy(org.omg.CORBA.Object ior, AgentPath agentPath) throws ObjectNotFoundException {
        super(ior, agentPath);
        mAgentPath = agentPath;
    }

    @Override
    public void finalize() throws Throwable {
        if (auth != null) {
            auth.disconnect();
        }
        super.finalize();
    }

    public Authenticator getAuthObj() {
        return auth;
    }

    public void setAuthObj(Authenticator auth) {
        this.auth = auth;
    }

    @Override
    public Agent narrow() throws ObjectNotFoundException {
        try {
            return AgentHelper.narrow(mIOR);
        }
        catch (org.omg.CORBA.BAD_PARAM ex) {
        }
        throw new ObjectNotFoundException("CORBA Object was not an Agent, or the server is down.");
    }

    /**
     * Standard execution of jobs. Note that this method should always be the
     * one used from clients - all execution parameters are taken from the job
     * where they're probably going to be correct.
     *
     * @param job
     * @throws AccessRightsException
     * @throws InvalidDataException
     * @throws InvalidTransitionException
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     * @throws ObjectAlreadyExistsException
     * @throws ScriptErrorException
     * @throws InvalidCollectionModification
     */
    public String execute(Job job)
            throws AccessRightsException, InvalidDataException, InvalidTransitionException, ObjectNotFoundException,
            PersistencyException, ObjectAlreadyExistsException, ScriptErrorException, InvalidCollectionModification
    {
        ItemProxy item = Gateway.getProxyManager().getProxy(job.getItemPath());
        Date startTime = new Date();

        Logger.msg(3, "AgentProxy.execute() - executing " + job.getStepPath() + " for " + mAgentPath.getAgentName());

        // get the outcome validator if present
        if (job.hasScript()) {
            Logger.msg(3, "AgentProxy - executing script");
            try {
                // pre-validate outcome from script if there is one
                if (job.hasOutcome() || job.isOutcomeSet()) {
                    Logger.msg(5, "AgentProxy.execute() - validating outcome before script execution");
                    String error = job.getOutcome().validate();
                    if (error.length() > 0) {
                        Logger.error("Outcome not valid: \n " + error);
                        Logger.error(job.getOutcome().getData());
                        throw new InvalidDataException(error);
                    }
                }

                // load script
                ErrorInfo scriptErrors = (ErrorInfo) callScript(item, job);
                String errorString = scriptErrors.toString();
                if (scriptErrors.getFatal()) {
                    Logger.error("AgentProxy.execute() - fatal script error");
                    throw new ScriptErrorException(scriptErrors);
                }
                
                if (errorString.length() > 0) Logger.warning("Script errors: " + errorString);
            }
            catch (ScriptingEngineException ex) {
                Logger.error(ex);
                throw new InvalidDataException(ex.getMessage());
            }
        }

        if (job.isOutcomeSet()) {
            Logger.msg(3, "AgentProxy - validating outcome");
            String error = job.getOutcome().validate();
            if (error.length() > 0) {
                Logger.error(job.getOutcome().getData());
                throw new InvalidDataException(error);
            }
        }

        job.setAgentPath(mAgentPath);
        Logger.msg(3, "AgentProxy.execute() - submitting job to item proxy");
        String result = item.requestAction(job);
        if (Logger.doLog(3)) {
            Date timeNow = new Date();
            long secsNow = (timeNow.getTime() - startTime.getTime()) / 1000;
            Logger.msg(3, "AgentProxy.execute() - execution DONE in " + secsNow + " seconds");
        }

        return result;
    }

    private Object callScript(ItemProxy item, Job job) throws ScriptingEngineException, InvalidDataException, ObjectNotFoundException {
        Script script = job.getScript();
        script.setActExecEnvironment(item, this, job);
        return script.execute();
    }

    public String execute(ItemProxy item, String predefStep, C2KLocalObject obj) throws AccessRightsException, InvalidDataException,
            InvalidTransitionException, ObjectNotFoundException, PersistencyException, ObjectAlreadyExistsException,
            InvalidCollectionModification {
        String param;
        try {
            param = marshall(obj);
        }
        catch (Exception ex) {
            Logger.error(ex);
            throw new InvalidDataException("Error on marshall");
        }
        return execute(item, predefStep, param);
    }

    /**
     * Multi-parameter execution. Wraps parameters up in a PredefinedStepOutcome
     * if the schema of the requested step is such.
     * 
     * @param item
     *            The item on which to execute the step
     * @param predefStep
     *            The step name to run
     * @param params
     *            An array of parameters to pass to the step. See each step's
     *            documentation for its required parameters
     * 
     * @return The outcome after processing. May have been altered by the step.
     * 
     * @throws AccessRightsException
     *             The agent was not allowed to execute this step
     * @throws InvalidDataException
     *             The parameters supplied were incorrect
     * @throws InvalidTransitionException
     *             The step wasn't available
     * @throws ObjectNotFoundException
     *             Thrown by some steps that try to locate additional objects
     * @throws PersistencyException
     *             Problem writing or reading the database
     * @throws ObjectAlreadyExistsException
     *             Thrown by steps that create additional object
     * @throws InvalidCollectionModification
     */
    public String execute(ItemProxy item, String predefStep, String[] params) throws AccessRightsException, InvalidDataException,
            InvalidTransitionException, ObjectNotFoundException, PersistencyException, ObjectAlreadyExistsException,
            InvalidCollectionModification {
        String schemaName = PredefinedStep.getPredefStepSchemaName(predefStep);
        String param;

        if (schemaName.equals("PredefinedStepOutcome")) param = PredefinedStep.bundleData(params);
        else                                            param = params[0];

        try {
            return item.getItem().requestAction(mAgentPath.getSystemKey(), "workflow/predefined/" + predefStep, PredefinedStep.DONE, param);
        }
        catch (Exception ex) {
            Logger.error(ex);
            throw ex;
        }
    }

    /**
     * Single parameter execution
     * 
     * @see #execute(ItemProxy, String, String[])
     * 
     * @param item
     * @param predefStep
     * @param param
     * @return The outcome after processing. May have been altered by the step.
     * @throws AccessRightsException
     * @throws InvalidDataException
     * @throws InvalidTransitionException
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     * @throws ObjectAlreadyExistsException
     * @throws InvalidCollectionModification
     */
    public String execute(ItemProxy item, String predefStep, String param) 
            throws AccessRightsException, InvalidDataException, InvalidTransitionException, ObjectNotFoundException, PersistencyException, ObjectAlreadyExistsException,InvalidCollectionModification
    {
        return execute(item, predefStep, new String[] { param });
    }

    /** Wrappers for scripts */
    public String marshall(Object obj) throws Exception {
        return Gateway.getMarshaller().marshall(obj);
    }

    public Object unmarshall(String obj) throws Exception {
        return Gateway.getMarshaller().unmarshall(obj);
    }

    public ItemProxy searchItem(String name) throws ObjectNotFoundException {
        return searchItem(new DomainPath(""), name);
    }

    /** Let scripts resolve items */
    public ItemProxy searchItem(Path root, String name) throws ObjectNotFoundException {
        Iterator<Path> results = Gateway.getLookup().search(root, name);

        Path returnPath = null;
        if (!results.hasNext()) throw new ObjectNotFoundException(name);

        while (results.hasNext()) {
            Path nextMatch = results.next();
            if (returnPath != null && nextMatch.getUUID() != null && !returnPath.getUUID().equals(nextMatch.getUUID()))
                throw new ObjectNotFoundException("Too many items with that name");
            returnPath = nextMatch;
        }

        return Gateway.getProxyManager().getProxy(returnPath);
    }

    public List<ItemProxy> searchItems(Path start, PropertyDescriptionList props) {
        Iterator<Path> results = Gateway.getLookup().search(start, props);
        return createItemProxyList(results);
    }

    public List<ItemProxy> searchItems(Path start, Property[] props) {
        Iterator<Path> results = Gateway.getLookup().search(start, props);
        return createItemProxyList(results);
    }

    private List<ItemProxy> createItemProxyList(Iterator<Path> results) {
        ArrayList<ItemProxy> returnList = new ArrayList<ItemProxy>();
        while (results.hasNext()) {
            Path nextMatch = results.next();
            try {
                returnList.add(Gateway.getProxyManager().getProxy(nextMatch));
            }
            catch (ObjectNotFoundException e) {
                Logger.error("Path '" + nextMatch + "' did not resolve to an Item");
            }
        }
        return returnList;
    }

    public ItemProxy getItem(String itemPath) throws ObjectNotFoundException {
        return (getItem(new DomainPath(itemPath)));
    }

    @Override
    public AgentPath getPath() {
        return mAgentPath;
    }

    public ItemProxy getItem(Path itemPath) throws ObjectNotFoundException {
        return Gateway.getProxyManager().getProxy(itemPath);
    }

    public ItemProxy getItemByUUID(String uuid) throws ObjectNotFoundException, InvalidItemPathException {
        return Gateway.getProxyManager().getProxy(new ItemPath(uuid));
    }
}
