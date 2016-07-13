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
package org.cristalise.kernel.lifecycle;

import static org.cristalise.kernel.collection.BuiltInCollections.ACTIVITY;
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA;
import static org.cristalise.kernel.collection.BuiltInCollections.SCRIPT;
import static org.cristalise.kernel.collection.BuiltInCollections.STATE_MACHINE;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCRIPT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCRIPT_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Vector;

import org.cristalise.kernel.collection.BuiltInCollections;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.WfVertex;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

/**
 * 
 */
public class ActivityDef extends WfVertexDef implements C2KLocalObject, DescriptionObject {
    public static BuiltInCollections[] builtInDependencies = {STATE_MACHINE, SCHEMA, SCRIPT};

    private int     mId      = -1;
    private String  mName    = "";
    private Integer mVersion = null;  // null is 'last',previously was -1
    public boolean  changed  = false;
    
    ItemPath        itemPath;

    Schema          actSchema;
    Script          actScript;
    StateMachine    actStateMachine;

    public ActivityDef() {
        mErrors = new Vector<String>(0, 1);
        setProperties(new WfCastorHashMap());
        setIsLayoutable(false);
    }

    protected String getDefaultSMName() {
        return "Default";
    }

    @Override
    public void setID(int id) {
        mId = id;
        if (mName.equals("")) setName(String.valueOf(id));
    }

    @Override
    public int getID() {
        return mId;
    }

    @Override
    public String getItemID() {
        return itemPath.getUUID().toString();
    }

    /**
     * @see org.cristalise.kernel.graph.model.Vertex#setName(java.lang.String)
     */
    @Override
    public void setName(String n) {
        mName = n;
    }

    /**
     * @see org.cristalise.kernel.graph.model.Vertex#getName()
     */
    @Override
    public String getName() {
        return mName;
    }

    @Override
    public void setVersion(Integer v) {
        mVersion = v;
    }

    /**
     * @see org.cristalise.kernel.graph.model.Vertex#getName()
     */
    @Override
    public Integer getVersion() {
        return mVersion;
    }

    /**
     * @see org.cristalise.kernel.lifecycle.WfVertexDef#getErrors()
     */
    @Override
    public String getErrors() {
        return super.getErrors();
    }

    /**
     * @see org.cristalise.kernel.lifecycle.WfVertexDef#verify()
     */
    @Override
    public boolean verify() {
        return true;
    }

    /**
     * @see org.cristalise.kernel.entity.C2KLocalObject#getClusterType()
     */
    @Override
    public String getClusterType() {
        return null;
    }

    public String getActName() {
        return getName();
    }

    /**
     */
    public String getDescName() {
        return getName();
    }

    public void configureInstanceProp(CastorHashMap instanceProps, DescriptionObject desc, BuiltInVertexProperties nameProp, BuiltInVertexProperties verProp) {
        if (desc != null) {
            Logger.msg(5, "ActivityDef.configureInstanceProp(actName:"+getName()+") - Setting property:"+nameProp);

            instanceProps.setBuiltInProperty(nameProp, desc.getItemID());
            instanceProps.setBuiltInProperty(verProp, desc.getVersion());
        }
        else {
            Logger.msg(8, "ActivityDef.configureInstanceProp(actName:"+getName()+") - No values were set for property:"+nameProp);
        }
    }

    @Override
    public WfVertex instantiate() throws ObjectNotFoundException, InvalidDataException {
        return instantiate(getName());
    }

    public WfVertex instantiate(String name) throws ObjectNotFoundException, InvalidDataException {
        Activity act = new Activity();
        act.setName(name);

        configureInstance(act);

        if (getItemPath() != null) act.setType(getItemID());

        return act;
    }

    public static BuiltInVertexProperties getNamePropertyOfBuiltInCollection(BuiltInCollections collection) throws InvalidDataException {
        switch (collection) {
            case SCHEMA:
                return SCHEMA_NAME;
            case SCRIPT:
                return SCRIPT_NAME;
            case STATE_MACHINE:
                return STATE_MACHINE_NAME;
            default:
                throw new InvalidDataException("ActivityDef does not handle BuiltInCollection:"+collection);
        }
    }

    public static BuiltInVertexProperties getVersionPropertyOfBuiltInCollection(BuiltInCollections collection) throws InvalidDataException {
        switch (collection) {
            case SCHEMA:
                return SCHEMA_VERSION;
            case SCRIPT:
                return SCRIPT_VERSION;
            case STATE_MACHINE:
                return STATE_MACHINE_VERSION;
            default:
                throw new InvalidDataException("ActivityDef does not handle BuiltInCollection:"+collection);
        }
    }

    public DescriptionObject getDescriptionObjectOfBuiltInCollection(BuiltInCollections collection) throws InvalidDataException, ObjectNotFoundException {
        switch (collection) {
            case SCHEMA:
                return getSchema();
            case SCRIPT:
                return getScript();
            case STATE_MACHINE:
                return getStateMachine();
            default:
                throw new InvalidDataException("ActivityDef does not handle BuiltInCollection:"+collection);
        }
    }

    public void configureInstance(WfVertex act) throws InvalidDataException, ObjectNotFoundException {
        super.configureInstance(act);

//      configureInstanceProp(act.getProperties(), getSchema(),       SCHEMA_NAME,        SCHEMA_VERSION);
//      configureInstanceProp(act.getProperties(), getScript(),       SCRIPT_NAME,        SCRIPT_VERSION);
//      configureInstanceProp(act.getProperties(), getStateMachine(), STATE_MACHINE_NAME, STATE_MACHINE_VERSION);

        try {
            for (String collName : Gateway.getStorage().getClusterContents(itemPath, ClusterStorage.COLLECTION)) {
                Logger.msg(5, "ActivityDef.configureInstance("+getName()+") - Processing collection:"+collName);

                BuiltInCollections coll = BuiltInCollections.getValue(collName);

                if(coll != null && coll != ACTIVITY) {
                    configureInstanceProp( act.getProperties(),  
                                           getDescriptionObjectOfBuiltInCollection(coll),
                                           getNamePropertyOfBuiltInCollection(coll), 
                                           getVersionPropertyOfBuiltInCollection(coll) );
                }
                else {
                    Logger.warning("ActivityDef.configureInstance("+getName()+") - UNIMPLEMENTED collection:"+collName);
                    /*
                    Dependency resColl;

                    try {
                        String verStr = (mVersion == null || mVersion == -1) ? "last" : String.valueOf(mVersion);
                        resColl = (Dependency) Gateway.getStorage().get(itemPath, ClusterStorage.COLLECTION + "/" + collName + "/" + verStr, null);
                    }
                    catch (PersistencyException e) {
                        Logger.error(e);
                        throw new InvalidDataException("Error loading collection " + collName);
                    }

                    for (DependencyMember resMem : resColl.getMembers().list) {
                        String resUUID = resMem.getChildUUID();
                        Integer resVer = deriveVersionNumber(resMem.getBuiltInProperty(VERSION));
                    }
                    */
                }
                
                //TODO: calling original solution for backward compatibility in case Activity is still using Properties instead of Collections
                if (actSchema == null)
                    configureInstanceProp(act.getProperties(), getSchema(),       SCHEMA_NAME,        SCHEMA_VERSION);
                if(actScript == null)
                    configureInstanceProp(act.getProperties(), getScript(),       SCRIPT_NAME,        SCRIPT_VERSION);
                if(actStateMachine == null)
                    configureInstanceProp(act.getProperties(), getStateMachine(), STATE_MACHINE_NAME, STATE_MACHINE_VERSION);
            }
        }
        catch (PersistencyException e) {
            Logger.error(e);
            throw new InvalidDataException(e.getMessage());
        }
    }

    @Override
    public ItemPath getItemPath() {
        return itemPath;
    }

    @Override
    public void setItemPath(ItemPath path) {
        itemPath = path;
    }

    public Schema getSchema() throws InvalidDataException, ObjectNotFoundException {
        if (actSchema == null) {
            Logger.msg(1, "ActivityDef.getSchema(actName:"+getName()+") - Loading ...");

            try {
                actSchema = (Schema) getBuiltInCollectionResource(SCHEMA)[0];
            }
            catch (ObjectNotFoundException ex) {
                actSchema = (Schema) getPropertyResource(SCHEMA_NAME, SCHEMA_VERSION);
            }
        }
        return actSchema;
    }

    public Script getScript() throws InvalidDataException, ObjectNotFoundException {
        if (actScript == null) {
            Logger.msg(1, "ActivityDef.getScript(actName:"+getName()+") - Loading ...");

            try {
                actScript = (Script) getBuiltInCollectionResource(SCRIPT)[0];
            }
            catch (ObjectNotFoundException ex) {
                actScript = (Script) getPropertyResource(SCRIPT_NAME, SCRIPT_VERSION);
            }
        }
        return actScript;
    }

    public StateMachine getStateMachine() throws InvalidDataException, ObjectNotFoundException {
        if (actStateMachine == null) {
            Logger.msg(1, "ActivityDef.getStateMachine(actName:"+getName()+") - Loading ...");

            try {
                actStateMachine = (StateMachine) getBuiltInCollectionResource(STATE_MACHINE)[0];
            }
            catch (ObjectNotFoundException ex) {
                String smNameProp = (String) getProperties().get(STATE_MACHINE_NAME);
                
                if (smNameProp == null || smNameProp.length() > 0) actStateMachine = LocalObjectLoader.getStateMachine(getDefaultSMName(), 0);
                else actStateMachine = (StateMachine) getPropertyResource(STATE_MACHINE_NAME, STATE_MACHINE_VERSION);
            }
        }
        return actStateMachine;
    }

    protected DescriptionObject[] getBuiltInCollectionResource(BuiltInCollections collection) throws ObjectNotFoundException, InvalidDataException {
        // not stored yet
        if (itemPath == null) throw new ObjectNotFoundException(); 

        Logger.msg(5, "ActivityDef.getCollectionResource(actName:"+getName()+") - Loading from collection:"+collection);

        Dependency resColl;

        try {
            String verStr = (mVersion == null || mVersion == -1) ? "last" : String.valueOf(mVersion);
            resColl = (Dependency) Gateway.getStorage().get(itemPath, ClusterStorage.COLLECTION + "/" + collection + "/" + verStr, null);
        }
        catch (PersistencyException e) {
            Logger.error(e);
            throw new InvalidDataException("Error loading description collection " + collection);
        }

        ArrayList<DescriptionObject> retArr = new ArrayList<DescriptionObject>();

        for (DependencyMember resMem : resColl.getMembers().list) {
            String resUUID = resMem.getChildUUID();
            Integer resVer = deriveVersionNumber(resMem.getBuiltInProperty(VERSION));

            if (resVer == null) {
                throw new InvalidDataException("Version is null for Item:" + itemPath + ", Collection:" + collection + ", DependencyMember:" + resUUID);
            }

            switch (collection) {
                case SCHEMA:
                    retArr.add(LocalObjectLoader.getSchema(resUUID, resVer));
                    break;
                case SCRIPT:
                    retArr.add(LocalObjectLoader.getScript(resUUID, resVer));
                    break;
                case STATE_MACHINE:
                    retArr.add(LocalObjectLoader.getStateMachine(resUUID, resVer));
                    break;
                case ACTIVITY:
                    retArr.add(LocalObjectLoader.getActDef(resUUID, resVer));
                    break;
                default:
                    throw new InvalidDataException("");
            }
        }
        
        if (retArr.size() == 0) throw new ObjectNotFoundException();
        
        return retArr.toArray(new DescriptionObject[retArr.size()]);
    }

    protected DescriptionObject getPropertyResource(BuiltInVertexProperties nameProp, BuiltInVertexProperties verProp)
            throws InvalidDataException, ObjectNotFoundException
    {
        if (Gateway.getLookup() == null) return null;
        Logger.msg(5, "ActivityDef.getPropertyResource(actName:"+getName()+") - Loading from property:"+nameProp);

        String resName = (String) getBuiltInProperty(nameProp);

        if (!(getProperties().isAbstract(nameProp)) && resName != null && resName.length() > 0) {
            Integer resVer = deriveVersionNumber(getBuiltInProperty(verProp));

            if (resVer == null && !(getProperties().isAbstract(verProp))) {
                throw new InvalidDataException("Invalid version property '" + resVer + "' in " + verProp + " for " + getName());
            }

            switch (nameProp) {
                case SCHEMA_NAME:
                    return LocalObjectLoader.getSchema(resName, resVer);
                case SCRIPT_NAME:
                    return LocalObjectLoader.getScript(resName, resVer);
                case STATE_MACHINE_NAME:
                    return LocalObjectLoader.getStateMachine(resName, resVer);
                default:
                    throw new InvalidDataException("");
            }
        }
        return null;
    }

    public void setSchema(Schema actSchema) {
        this.actSchema = actSchema;
    }

    public void setScript(Script actScript) {
        this.actScript = actScript;
    }

    public void setStateMachine(StateMachine actStateMachine) {
        this.actStateMachine = actStateMachine;
    }

    public Dependency makeDescCollection(BuiltInCollections collection, DescriptionObject... descs) throws InvalidDataException {
        //TODO: restrict membership based on kernel property desc
        Dependency descDep = new Dependency(collection.getName());
        if (mVersion != null && mVersion > -1) {
            descDep.setVersion(mVersion);
        }

        for (DescriptionObject thisDesc : descs) {
            if (thisDesc == null) continue;
            try {
                DependencyMember descMem = descDep.addMember(thisDesc.getItemPath());
                descMem.setBuiltInProperty(VERSION, thisDesc.getVersion());
            }
            catch (Exception e) {
                Logger.error(e);
                throw new InvalidDataException("Problem creating description collection for " + thisDesc + " in " + getName());
            }
        }
        return descDep;
    }

    @Override
    public CollectionArrayList makeDescCollections() throws InvalidDataException, ObjectNotFoundException {
        CollectionArrayList retArr = new CollectionArrayList();

        retArr.put(makeDescCollection(SCHEMA,        getSchema()));
        retArr.put(makeDescCollection(SCRIPT,        getScript()));
        retArr.put(makeDescCollection(STATE_MACHINE, getStateMachine()));

        return retArr;
    }

    @Override
    public void export(Writer imports, File dir) throws InvalidDataException, ObjectNotFoundException, IOException {
        String actXML;
        try {
            actXML = Gateway.getMarshaller().marshall(this);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new InvalidDataException("Couldn't marshall activity def " + getActName());
        }
        
        FileStringUtility.string2File(new File(new File(dir, "EA"), getActName() + (getVersion() == null ? "" : "_" + getVersion()) + ".xml"), actXML);

        if (imports != null) {
            imports.write("<Activity " + getExportAttributes("EA") + ">" + getExportCollections() + "</Activity>\n");
        }
    }

    protected String getExportAttributes(String type) throws InvalidDataException, ObjectNotFoundException, IOException {
        return "name=\"" + getActName() + "\" " 
                + (getItemPath() == null ? "" : "id=\""      + getItemID()  + "\" ")
                + (getVersion() == null  ? "" : "version=\"" + getVersion() + "\" ")
                + "resource=\"boot/" + type + "/" + getActName() + (getVersion() == null ? "" : "_" + getVersion()) + ".xml\"";
    }

    protected String getExportCollections() throws InvalidDataException, ObjectNotFoundException, IOException {
        return 
            (getStateMachine() == null ? "" : "<StateMachine name=\"" + getStateMachine().getName() + "\" id=\"" + getStateMachine().getItemID() + "\" version=\"" + getStateMachine().getVersion() + "\"/>")
                + (getSchema() == null ? "" : "<Schema       name=\"" + getSchema().getName() + "\"       id=\"" + getSchema().getItemID()       + "\" version=\"" + getSchema().getVersion()       + "\"/>")
                + (getScript() == null ? "" : "<Script       name=\"" + getScript().getName() + "\"       id=\"" + getScript().getItemID()       + "\" version=\"" + getScript().getVersion()       + "\"/>");
    }
}
