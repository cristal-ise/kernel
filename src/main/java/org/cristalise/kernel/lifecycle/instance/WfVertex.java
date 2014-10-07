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
package org.cristalise.kernel.lifecycle.instance;



import java.util.HashMap;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lifecycle.routingHelpers.ViewpointDataHelper;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.Logger;


/**
 * @version $Revision: 1.38 $ $Date: 2005/09/07 13:46:31 $
 * @author  $Author: abranson $
 */
public abstract class WfVertex extends GraphableVertex
{
    /**sets the activity available to be executed on start of Workflow or composite activity (when it is the first one of the
     * (sub)process
     * @throws InvalidDataException 
     * @throws ObjectAlreadyExistsException 
     * @throws ObjectNotFoundException 
     * @throws AccessRightsException 
     * @throws InvalidTransitionException 
     * @throws PersistencyException 
     * @throws ObjectCannotBeUpdated */
    public abstract void runFirst(AgentPath agent, ItemPath itemPath) throws InvalidDataException;

    /**
	 * @see java.lang.Object#Object()
	 */
	public WfVertex()
    {
    	super();
		setIsLayoutable(true);
		setIsComposite(false);
    }

    /**
	 * Method runNext.
     * @throws InvalidDataException 
     * @throws ObjectNotFoundException 
     * @throws AccessRightsException 
     * @throws InvalidTransitionException 
     * @throws PersistencyException 
     * @throws ObjectAlreadyExistsException 
     * @throws ObjectCannotBeUpdated 
	 */
	public void runNext(AgentPath agent, ItemPath itemPath) throws InvalidDataException
    {
		try
		{
			((CompositeActivity)getParent()).request(agent, itemPath, CompositeActivity.COMPLETE, null);
		}
		catch (Exception e)
		{
			//Logger.error(e);
		}

    }

    /**
	 * Method reinit.
	 * @param idLoop
     * @throws InvalidDataException 
     * @throws ObjectNotFoundException 
	 */
	public abstract void reinit( int idLoop ) throws InvalidDataException;

    /**
	 * Method verify.
	 * @return boolean
	 */
	public abstract boolean verify();

    /**
	 * Method getErrors.
	 * @return String
	 */
	public abstract String getErrors();

    /**
	 * Method run.
     * @throws InvalidDataException 
     * @throws ObjectAlreadyExistsException 
     * @throws ObjectNotFoundException 
     * @throws AccessRightsException 
     * @throws InvalidTransitionException 
     * @throws PersistencyException 
     * @throws ObjectCannotBeUpdated 
	 */
	public abstract void run(AgentPath agent, ItemPath itemPath) throws InvalidDataException;

    /**
	 * Method loop.
	 * @return boolean
	 */
	public abstract boolean loop();

    /**
	 * Method addNext.
	 * @param vertex
	 */
	public abstract Next addNext(WfVertex vertex);

    protected Object evaluateScript(String scriptName, Integer scriptVersion, ItemPath itemPath) throws ScriptingEngineException
    {

        try
        {
            Script script = getScript(scriptName, scriptVersion);

            KeyValuePair[] k = getProperties().getKeyValuePairs();
            HashMap<?, ?> requiredInput = script.getAllInputParams();
            for (KeyValuePair element : k) {
                if (requiredInput.containsKey(element.getKey()))
                {
                    String value = element.getStringValue();
                    Object inputParam = value;

                    if (value.startsWith("viewpoint//"))
                    {
                        value = value.substring(11);
                        if (value.startsWith("."))
                            value = itemPath.getUUID() + value.substring(1);
                        try {
                            inputParam = ViewpointDataHelper.get(value)[0];
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            throw new InvalidDataException("Could not retrieve data from viewpoint: "+value);
                        }
                    }
                    if (value.startsWith("property//"))
                    {
                    	value = value.substring(10);
                    	try {
                    		inputParam = Gateway.getStorage().get(itemPath, ClusterStorage.PROPERTY+"/"+value, null);
                    	} catch (ObjectNotFoundException ex) {
                    		inputParam = null;
                    	}
                    }
                    Logger.msg(5, "Split.evaluateScript() - Setting param " + element.getKey() + " to " + inputParam);
                    script.setInputParamValue(element.getKey(), inputParam);
                }
            }

            //TODO: is this right?
            if (requiredInput.containsKey("item")) {
                script.setInputParamValue("item", Gateway.getProxyManager().getProxy(itemPath));
            }
            if (requiredInput.containsKey("agent")) {
                AgentPath systemAgent = Gateway.getLookup().getAgentPath("system");
                script.setInputParamValue("agent", Gateway.getProxyManager().getProxy(systemAgent));
            }
            Object retVal = script.execute();
            Logger.msg(2, "Split.evaluateScript() - Script returned "+retVal);
            if (retVal == null) retVal = "";
            return retVal;
        }
        catch (Exception e)
        {
            Logger.msg(1, "Split.evaluateScript() - Error: Script " + scriptName);
            Logger.error(e);
            throw new ScriptingEngineException();
        }
    }

    private static Script getScript(String name, Integer version) throws ScriptingEngineException
    {
    	if (name == null || name.length() == 0)  
    		throw new ScriptingEngineException("Script name is empty");
        Script script;
        if (version!=null) {
            script = new Script(name, version);
        }
        else { // empty version: try expression
            int split = name.indexOf(":");
            script = new Script(name.substring(0, split), name.substring(split + 1));
        }

        return script;
    }


	public Workflow getWf()
	{
		return ((CompositeActivity)getParent()).getWf();
	}
}

