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



import java.util.HashMap;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lifecycle.routingHelpers.ActivityDataHelper;
import org.cristalise.kernel.lifecycle.routingHelpers.DataHelper;
import org.cristalise.kernel.lifecycle.routingHelpers.PropertyDataHelper;
import org.cristalise.kernel.lifecycle.routingHelpers.ViewpointDataHelper;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;


/**
 * @version $Revision: 1.38 $ $Date: 2005/09/07 13:46:31 $
 * @author  $Author: abranson $
 */
public abstract class WfVertex extends GraphableVertex
{
    public enum Types {
        Atomic,
        Composite,
        OrSplit,
        XOrSplit,
        AndSplit,
        LoopSplit,
        Join,
        Route
    }
    
    /**sets the activity available to be executed on start of Workflow or composite activity (when it is the first one of the
     * (sub)process
     * @throws InvalidDataException 
     * @throws ObjectAlreadyExistsException 
     * @throws ObjectNotFoundException 
     * @throws AccessRightsException 
     * @throws InvalidTransitionException 
     * @throws PersistencyException 
     * @throws ObjectCannotBeUpdated */
    public abstract void runFirst(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException;

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
    public abstract void runNext(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException;
 
    /**
     * Method reinit.
     * @param idLoop
     * @throws InvalidDataException 
     * @throws ObjectNotFoundException 
	 */
    public abstract void reinit( int idLoop ) throws InvalidDataException;
    
    public void abort() { }

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
	public abstract void run(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException;

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
	
	public Object evaluateProperty(ItemPath itemPath, String propName, Object locker) throws InvalidDataException, PersistencyException, ObjectNotFoundException {
		return evaluatePropertyValue(itemPath, getProperties().get(propName), locker);
	}

	public Object evaluatePropertyValue(ItemPath itemPath, Object propValue, Object locker) throws InvalidDataException, PersistencyException, ObjectNotFoundException {

		if (!(propValue instanceof String) || !((String)propValue).contains("//")) //
			return propValue;
		String propValString = (String)propValue;
		DataHelper dataHelper;
        String[] valueSplit = propValString.split("//");

        if (valueSplit.length != 2) throw new InvalidDataException("Invalid param: "+propValue);

        String pathType = valueSplit[0];
        String dataPath = valueSplit[1];
        
        //derive the parent composite in which the script is running
        String actContext = getPath();
        actContext = actContext.substring(0, actContext.lastIndexOf('/'));

        try {
            dataHelper = getDataHelper(pathType);
        } catch (ObjectNotFoundException ex) { // No recognized helper, just return the raw value
            return propValue;
        }
        if (itemPath == null) itemPath = getWf().getItemPath();

        return dataHelper.get(itemPath, actContext, dataPath, locker);
	}
	
	public static DataHelper getDataHelper(String id) throws InvalidDataException, ObjectNotFoundException {
		Object configHelper = Gateway.getProperties().getObject("DataHelper."+id);
		if (configHelper != null) {
			if (configHelper instanceof DataHelper) 
				return (DataHelper)configHelper;
			else 
				throw new InvalidDataException("Property DataHelper."+id+" was not a DataHelper: " +configHelper.toString());
		}
		else
			switch (id) {
	        case "viewpoint":
	            return new ViewpointDataHelper();
	        case "property":
	        	return new PropertyDataHelper();
	        case "activity":
	        	return new ActivityDataHelper();
			}
		throw new ObjectNotFoundException("Data helper '"+id+"' unknown");
	}
	
	
    protected Object evaluateScript(String scriptName, Integer scriptVersion, ItemPath itemPath, Object locker) throws ScriptingEngineException
    {
        try {
            Script script = getScript(scriptName, scriptVersion);
            HashMap<?, ?> scriptInputParams = script.getAllInputParams();
            
            for (KeyValuePair vertexProp : getProperties().getKeyValuePairs()) {
                if (scriptInputParams.containsKey(vertexProp.getKey())) {
                    Object value = vertexProp.getValue();
                    Logger.msg(5,"WfVertex.evaluateScript() - Match found for '"+vertexProp.getKey()+"' => setting value from " + value);
                    Object inputParam = evaluatePropertyValue(itemPath, value, locker);
                    Logger.msg(5, "Split.evaluateScript() - Setting param " + vertexProp.getKey() + " to " + inputParam);
                    script.setInputParamValue(vertexProp.getKey(), inputParam);
                }
            }

            if (scriptInputParams.containsKey("item")) {
                script.setInputParamValue("item", Gateway.getProxyManager().getProxy(itemPath));
            }
            if (scriptInputParams.containsKey("agent")) {
                AgentPath systemAgent = Gateway.getLookup().getAgentPath("system");
                script.setInputParamValue("agent", Gateway.getProxyManager().getProxy(systemAgent));
            }
            Object retVal = script.execute();
            Logger.msg(2, "Split.evaluateScript() - Script returned "+retVal);
            if (retVal == null) retVal = "";
            return retVal;
        }
        catch (Exception e) {
            Logger.msg(1, "Split.evaluateScript() - Error: Script " + scriptName);
            Logger.error(e);
            throw new ScriptingEngineException(e.getMessage());
        }
    }

    private static Script getScript(String name, Integer version) throws ScriptingEngineException, ObjectNotFoundException, InvalidDataException
    {
        if (name == null || name.length() == 0) throw new ScriptingEngineException("Script name is empty");
        
        Script script;

        if (version!=null) {
            script = LocalObjectLoader.getScript(name, version);
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

