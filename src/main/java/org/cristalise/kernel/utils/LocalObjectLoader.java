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

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.scripting.Script;


public class LocalObjectLoader {
	private static ActDefCache actCache = new ActDefCache(null);
	private static ActDefCache compActCache = new ActDefCache(true);
	private static ActDefCache elemActCache = new ActDefCache(false);
	private static StateMachineCache smCache = new StateMachineCache();
	private static SchemaCache schCache = new SchemaCache();
	private static ScriptCache scrCache = new ScriptCache();
	
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
	
	static public CompositeActivityDef getCompActDef(String actName, int actVersion) throws ObjectNotFoundException, InvalidDataException {
		Logger.msg(5, "Loading activity def "+actName+" v"+actVersion);
		return (CompositeActivityDef)compActCache.get(actName, actVersion);
	}
	
	static public ActivityDef getElemActDef(String actName, int actVersion) throws ObjectNotFoundException, InvalidDataException {
		Logger.msg(5, "Loading activity def "+actName+" v"+actVersion);
		return elemActCache.get(actName, actVersion);
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
