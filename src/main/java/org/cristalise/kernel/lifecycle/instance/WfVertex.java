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

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lifecycle.routingHelpers.DataHelperUtility;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.Logger;

/**
 * 
 */
public abstract class WfVertex extends GraphableVertex {

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

    /**
     * Sets the activity available to be executed on start of Workflow or composite activity 
     * (when it is the first one of the (sub)process)
     */
    public abstract void runFirst(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException;

    /**
     * 
     */
    public WfVertex() {
        super();
        setIsLayoutable(true);
        setIsComposite(false);
    }

    public abstract void runNext(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException;

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

    public abstract void run(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException;

    /**
     * @return boolean
     */
    public abstract boolean loop();
    
    /**
     * Derive the path of the parent CompositeAct in which the script is running
     * 
     * @return the path of the parent CompositeAct
     */
    public String getActContext() {
        return getPath().substring(0, getPath().lastIndexOf('/'));
    }

    public abstract Next addNext(WfVertex vertex);

    public Object evaluateProperty(ItemPath itemPath, String propName, Object locker)
            throws InvalidDataException, PersistencyException, ObjectNotFoundException
    {
        return evaluatePropertyValue(itemPath, getProperties().get(propName), locker);
    }
    public Object evaluatePropertyValue(ItemPath itemPath, Object propValue, Object locker)
            throws InvalidDataException, PersistencyException, ObjectNotFoundException
    {
        if (itemPath == null) itemPath = getWf().getItemPath();

        return DataHelperUtility.evaluateValue(itemPath, propValue, getActContext(), locker);
    }

    /**
     * 
     * 
     * @param scriptName
     * @param scriptVersion
     * @param itemPath
     * @param locker
     * @return the value returned by the Script
     * @throws ScriptingEngineException
     */
    protected Object evaluateScript(String scriptName, Integer scriptVersion, ItemPath itemPath, Object locker) throws ScriptingEngineException {
        try {
            if (itemPath == null) itemPath = getWf().getItemPath();

            Script script = Script.getScript(scriptName, scriptVersion);
            return script.evaluate(itemPath, getProperties(), getActContext(), locker);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new ScriptingEngineException(e.getMessage());
        }
    }

    /**
     * 
     * @return the top level CompositeActivity, aka Workflow
     */
    public Workflow getWf() {
        return ((CompositeActivity)getParent()).getWf();
    }
}

