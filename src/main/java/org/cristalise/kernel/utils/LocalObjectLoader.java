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
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;


public class LocalObjectLoader {
	private static ActDefCache actCache = new ActDefCache();
	private static StateMachineCache smCache = new StateMachineCache();
	private static SchemaCache schCache = new SchemaCache();
	private static ScriptCache scrCache = new ScriptCache();

	static public ItemProxy loadLocalObjectDef(String root, String name)
		throws ObjectNotFoundException
	{
		// first check for a UUID name
		try {
			ItemPath resItem = new ItemPath(name);
			if (resItem.exists())
				return Gateway.getProxyManager().getProxy(resItem);
		} catch (InvalidItemPathException ex) { }
			
		// then check for a direct path
		DomainPath directPath = new DomainPath(root+"/"+name);
		if (directPath.exists() && directPath.getItemPath() != null) {
			return Gateway.getProxyManager().getProxy(directPath);
		}
		
		// else search for it below
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


	/**
	 * Retrieves a named version of a script from the database
	 *
	 * @param scriptName - script name
	 * @param scriptVersion - integer script version
	 * @return Script
	 * @throws ObjectNotFoundException - When script or version does not exist
	 * @throws InvalidDataException - When the stored script data was invalid
	 * 
	 */	
	static public Script getScript(String scriptName, int scriptVersion) throws ObjectNotFoundException, InvalidDataException {
		Logger.msg(5, "Loading script "+scriptName+" v"+scriptVersion);
		return scrCache.get(scriptName, scriptVersion);
	}

	/**
	 * Retrieves a named version of a schema from the database
	 *
	 * @param schemaName - schema name
	 * @param schemaVersion - integer schema version
	 * @return Schema
	 * @throws ObjectNotFoundException - When schema or version does not exist
	 * @throws InvalidDataException - When the stored schema data was invalid
	 */
	static public Schema getSchema(String schemaName, int schemaVersion) throws ObjectNotFoundException, InvalidDataException {
		Logger.msg(5, "Loading schema "+schemaName+" v"+schemaVersion);
	    // don't bother if this is the Schema schema - for bootstrap esp.
	    if (schemaName.equals("Schema") && schemaVersion == 0)
	        return new Schema(schemaName, schemaVersion, null, "");
		return schCache.get(schemaName, schemaVersion);
	}

	/**
	 * Retrieves a named version of activity def from the database
	 *
	 * @param actName - activity name
	 * @param version - integer activity version
	 * @return ActivityDef
	 * @throws ObjectNotFoundException - When activity or version does not exist
	 * @throws InvalidDataException - When the stored script data was invalid
	 */
	static public ActivityDef getActDef(String actName, int actVersion) throws ObjectNotFoundException, InvalidDataException {
		Logger.msg(5, "Loading activity def "+actName+" v"+actVersion);
		return actCache.get(actName, actVersion);
	}
	
	/**
	 * Retrieves a named version of a state machine from the database
	 *
	 * @param smName - state machine name
	 * @param smVersion - integer state machine version
	 * @return StateMachine
	 * @throws ObjectNotFoundException - When state machine or version does not exist
	 * @throws InvalidDataException - When the stored state machine data was invalid
	 */	
	static public StateMachine getStateMachine(String smName, int smVersion) throws ObjectNotFoundException, InvalidDataException {
		Logger.msg(5, "Loading activity state machine "+smName+" v"+smVersion);
		return smCache.get(smName, smVersion);
	}
}
