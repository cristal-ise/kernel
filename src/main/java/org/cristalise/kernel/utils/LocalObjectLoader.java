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
package org.cristalise.kernel.utils;

import java.util.Iterator;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;


public class LocalObjectLoader {
	private static ActDefCache actCache = new ActDefCache();
	private static StateMachineCache smCache = new StateMachineCache();

	static public ItemProxy loadLocalObjectDef(String root, String name)
		throws ObjectNotFoundException
	{
		DomainPath defRoot = new DomainPath(root);
	    Iterator<Path> e = Gateway.getLookup().search(defRoot, name);
	    if (e.hasNext()) {
	    	DomainPath defPath = (DomainPath)e.next();
		    if (e.hasNext()) throw new ObjectNotFoundException("Too many matches for "+name+" in "+root);
		    return Gateway.getProxyManager().getProxy(defPath);
	    }
	    else {
	    	throw new ObjectNotFoundException("No match for "+name+" in "+root);
	    }

	}

	static public String getScript(String scriptName, int scriptVersion) throws ObjectNotFoundException {
	    Logger.msg(5, "Loading script "+scriptName+" v"+scriptVersion);
	    try {
	    	ItemProxy script = loadLocalObjectDef("/desc/Script/", scriptName);
   	        Viewpoint scriptView = (Viewpoint)script.getObject(ClusterStorage.VIEWPOINT + "/Script/" + scriptVersion);
	    	return scriptView.getOutcome().getData();
	    } catch (PersistencyException ex) {
	    	Logger.error(ex);
	        throw new ObjectNotFoundException("Error loading script " + scriptName + " version " + scriptVersion);
	    }

	}

	static public Schema getSchema(String schemaName, int schemaVersion) throws ObjectNotFoundException {
		Logger.msg(5, "Loading schema "+schemaName+" v"+schemaVersion);
	    
	    String docType = schemaName;
	    int docVersion = schemaVersion;
	    String schemaData;

	    // don't bother if this is the Schema schema - for bootstrap esp.
	    if (schemaName.equals("Schema") && schemaVersion == 0)
	        return new Schema(docType, docVersion, "");

        ItemProxy schema = loadLocalObjectDef("/desc/OutcomeDesc/", schemaName);
        Viewpoint schemaView = (Viewpoint)schema.getObject(ClusterStorage.VIEWPOINT + "/Schema/" + schemaVersion);
        try {
        	schemaData = schemaView.getOutcome().getData();
        } catch (PersistencyException ex) {
        	Logger.error(ex);
        	throw new ObjectNotFoundException("Problem loading schema "+schemaName+" v"+schemaVersion+": "+ex.getMessage());
        }
	    return new Schema(docType, docVersion, schemaData);
	}

	/**
	 * Retrieves a named version of activity def from the database
	 *
	 * @param actName - activity name
	 * @param version - named version (String)
	 * @return ActivityDef
	 * @throws ObjectNotFoundException - When activity or version does not exist
	 */
	static public ActivityDef getActDef(String actName, int actVersion) throws ObjectNotFoundException, InvalidDataException {
		Logger.msg(5, "Loading activity def "+actName+" v"+actVersion);
		return actCache.get(actName, actVersion);
	}
	
	static public StateMachine getStateMachine(String smName, int smVersion) throws ObjectNotFoundException, InvalidDataException {
		Logger.msg(5, "Loading activity state machine "+smName+" v"+smVersion);
		return smCache.get(smName, smVersion);
	}
}
