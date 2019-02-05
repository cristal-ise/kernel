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
package org.cristalise.kernel.utils;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PROPERTY_DEF_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PROPERTY_DEF_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.QUERY_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.QUERY_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCRIPT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCRIPT_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_VERSION;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.Script;


public class LocalObjectLoader {
    private static ActDefCache              actCache      = new ActDefCache(null);
    private static ActDefCache              compActCache  = new ActDefCache(true);
    private static ActDefCache              elemActCache  = new ActDefCache(false);
    private static StateMachineCache        smCache       = new StateMachineCache();
    private static SchemaCache              schCache      = new SchemaCache();
    private static ScriptCache              scrCache      = new ScriptCache();
    private static QueryCache               queryCache    = new QueryCache();
    private static PropertyDescriptionCache propDescCache = new PropertyDescriptionCache();

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
        Logger.msg(5, "LocalObjectLoader.getScript("+scriptName+" v"+scriptVersion+")");
        return scrCache.get(scriptName, scriptVersion);
    }

    /**
     * Retrieves a script from the database finding data in the Vertex properties
     * 
     * @param properties vertex properties
     * @return Script
     */
    static public Script getScript(CastorHashMap properties) throws InvalidDataException, ObjectNotFoundException {
        return (Script)getDescObjectByProperty(properties, SCRIPT_NAME, SCRIPT_VERSION);
    }

    /**
     * Retrieves a named version of a query from the database
     *
     * @param queryName - query name
     * @param queryVersion - integer query version
     * @return Query
     * @throws ObjectNotFoundException - When query or version does not exist
     * @throws InvalidDataException - When the stored query data was invalid
     * 
     */ 
    static public Query getQuery(String queryName, int queryVersion) throws ObjectNotFoundException, InvalidDataException {
        Logger.msg(5, "LocalObjectLoader.getQuery("+queryName+" v"+queryVersion+")");
        return queryCache.get(queryName, queryVersion);
    }

    /**
     * Retrieves a query from the database finding data in the Vertex properties
     * 
     * @param properties vertex properties
     * @return Query
     */
    static public Query getQuery(CastorHashMap properties) throws InvalidDataException, ObjectNotFoundException {
        return (Query)getDescObjectByProperty(properties, QUERY_NAME, QUERY_VERSION);
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
        Logger.msg(5, "LocalObjectLoader.getSchema("+schemaName+" v"+schemaVersion+")");

        // don't bother if this is the Schema schema - for bootstrap especially
        if (schemaName.equals("Schema") && schemaVersion == 0)
            return new Schema(schemaName, schemaVersion, new ItemPath(new SystemKey(0, 5)), "");
        
        return schCache.get(schemaName, schemaVersion);
    }

    /**
     * Retrieves a schema from the database finding data in the Vertex properties
     * 
     * @param properties vertex properties
     * @return Schema
     */
    static public Schema getSchema(CastorHashMap properties) throws InvalidDataException, ObjectNotFoundException {
        return (Schema)getDescObjectByProperty(properties, SCHEMA_NAME, SCHEMA_VERSION);
    }

    /**
     * Retrieves a named version of ActivityDef from the database
     *
     * @param actName - activity name
     * @param actVersion - integer activity version
     * @return ActivityDef
     * @throws ObjectNotFoundException - When activity or version does not exist
     * @throws InvalidDataException - When the stored script data was invalid
     */
    static public ActivityDef getActDef(String actName, int actVersion) throws ObjectNotFoundException, InvalidDataException {
        Logger.msg(5, "LocalObjectLoader.getActDef("+actName+" v"+actVersion+")");
        return actCache.get(actName, actVersion);
    }

    /**
     * Retrieves a named version of CompositeActivityDef from the database
     *
     * @param actName - activity name
     * @param actVersion - integer activity version
     * @return ActivityDef
     * @throws ObjectNotFoundException - When activity or version does not exist
     * @throws InvalidDataException - When the stored script data was invalid
     */
    static public CompositeActivityDef getCompActDef(String actName, int actVersion) throws ObjectNotFoundException, InvalidDataException {
        Logger.msg(5, "LocalObjectLoader.getCompActDef("+actName+" v"+actVersion+")");
        return (CompositeActivityDef)compActCache.get(actName, actVersion);
    }

    /**
     * Retrieves a named version of ActivityDef from the database
     *
     * @param actName - activity name
     * @param actVersion - integer activity version
     * @return ActivityDef
     * @throws ObjectNotFoundException - When activity or version does not exist
     * @throws InvalidDataException - When the stored script data was invalid
     */
    static public ActivityDef getElemActDef(String actName, int actVersion) throws ObjectNotFoundException, InvalidDataException {
        Logger.msg(5, "LocalObjectLoader.getElemActDef("+actName+" v"+actVersion+")");
        return elemActCache.get(actName, actVersion);
    }

    /**
     * Retrieves a named version of a StateMachine from the database
     *
     * @param smName - state machine name
     * @param smVersion - integer state machine version
     * @return StateMachine
     * @throws ObjectNotFoundException - When state machine or version does not exist
     * @throws InvalidDataException - When the stored state machine data was invalid
     */	
    static public StateMachine getStateMachine(String smName, int smVersion) throws ObjectNotFoundException, InvalidDataException {
        Logger.msg(5, "LocalObjectLoader.getStateMachine("+smName+" v"+smVersion+")");
        return smCache.get(smName, smVersion);
    }

    /**
     * 
     * @param properties
     * @return
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    static public StateMachine getStateMachine(CastorHashMap properties) throws InvalidDataException, ObjectNotFoundException {
        return (StateMachine)getDescObjectByProperty(properties, STATE_MACHINE_NAME, STATE_MACHINE_VERSION);
    }

    /**
     * 
     * @param name
     * @param version
     * @return
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    static public PropertyDescriptionList getPropertyDescriptionList(String name, int version) throws ObjectNotFoundException, InvalidDataException {
        Logger.msg(5, "LocalObjectLoader.PropertyDescriptionList("+name+" v"+version+")");
        return propDescCache.get(name, version);
    }

    /**
     * 
     * @param properties
     * @return
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    static public PropertyDescriptionList getPropertyDescriptionList(CastorHashMap properties) throws InvalidDataException, ObjectNotFoundException {
        return (PropertyDescriptionList)getDescObjectByProperty(properties, PROPERTY_DEF_NAME, PROPERTY_DEF_VERSION);
    }

    /**
     * 
     * @param properties
     * @param nameProp
     * @param verProp
     * @return DescriptionObject
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    private static DescriptionObject getDescObjectByProperty(CastorHashMap properties, BuiltInVertexProperties nameProp, BuiltInVertexProperties verProp)
            throws InvalidDataException, ObjectNotFoundException
    {
        String resName = (String) properties.getBuiltInProperty(nameProp);

        if (!(properties.isAbstract(nameProp)) && StringUtils.isNotBlank(resName)) {
            Integer resVer = deriveVersionNumber(properties.getBuiltInProperty(verProp));

            Logger.msg(5, "LocalObjectLoader.getDescObjectByProperty() - "+nameProp+":"+resName+" v"+resVer+")");

            if (resVer == null && !(properties.isAbstract(verProp))) {
                throw new InvalidDataException("Invalid version property '" + resVer + "' in " + verProp);
            }

            switch (nameProp) {
                case SCHEMA_NAME:        return getSchema(resName, resVer);
                case SCRIPT_NAME:        return getScript(resName, resVer);
                case QUERY_NAME :        return getQuery(resName, resVer);
                case STATE_MACHINE_NAME: return getStateMachine(resName, resVer);
                case PROPERTY_DEF_NAME:  return getPropertyDescriptionList(resName, resVer);
                default:
                    throw new InvalidDataException("LocalObjectLoader CANNOT handle BuiltInVertexPropertie:"+nameProp);
            }
        }
        return null;
    }

    /**
     * Converts Object to an Integer representing a version number.
     * 
     * @param value the Object containing a version number
     * @return the converted Version number. Set to null if value was null or-1
     */
    public static Integer deriveVersionNumber(Object value) throws InvalidDataException {
        if (value == null || "".equals(value)) return null;

        try {
            Integer version = Integer.valueOf(value.toString());

            if(version == -1) return null;
            return            version;
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("Invalid version number : "+value.toString());
        }
    }
}
