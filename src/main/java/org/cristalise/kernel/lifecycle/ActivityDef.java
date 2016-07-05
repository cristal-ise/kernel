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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SchemaType;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SchemaVersion;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ScriptName;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ScriptVersion;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.StateMachineName;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.StateMachineVersion;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.Version;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Vector;

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
    public static final String SCHCOL = "schema";
    public static final String SCRCOL = "script";
    public static final String SMCOL  = "statemachine";

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

    @Override
    public void configureInstance(WfVertex act) throws InvalidDataException, ObjectNotFoundException {
        super.configureInstance(act);

        configureInstanceProp(act.getProperties(), getSchema(),       SchemaType,       SchemaVersion);
        configureInstanceProp(act.getProperties(), getScript(),       ScriptName,       ScriptVersion);
        configureInstanceProp(act.getProperties(), getStateMachine(), StateMachineName, StateMachineVersion);
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
                actSchema = (Schema) getCollectionResource(SCHCOL)[0];
            }
            catch (ObjectNotFoundException ex) {
                actSchema = (Schema) getPropertyResource(SchemaType, SchemaVersion);
            }
        }
        return actSchema;
    }

    public Script getScript() throws InvalidDataException, ObjectNotFoundException {
        if (actScript == null) {
            Logger.msg(1, "ActivityDef.getScript(actName:"+getName()+") - Loading ...");

            try {
                actScript = (Script) getCollectionResource(SCRCOL)[0];
            }
            catch (ObjectNotFoundException ex) {
                actScript = (Script) getPropertyResource(ScriptName, ScriptVersion);
            }
        }
        return actScript;
    }

    public StateMachine getStateMachine() throws InvalidDataException, ObjectNotFoundException {
        if (actStateMachine == null) {
            Logger.msg(1, "ActivityDef.getStateMachine(actName:"+getName()+") - Loading ...");

            try {
                actStateMachine = (StateMachine) getCollectionResource(SMCOL)[0];
            }
            catch (ObjectNotFoundException ex) {
                String smNameProp = (String) getProperties().get(StateMachineName);
                
                if (smNameProp == null || smNameProp.length() > 0) actStateMachine = LocalObjectLoader.getStateMachine(getDefaultSMName(), 0);
                else actStateMachine = (StateMachine) getPropertyResource(StateMachineName, StateMachineVersion);
            }
        }
        return actStateMachine;
    }

    protected DescriptionObject[] getCollectionResource(String collName) throws ObjectNotFoundException, InvalidDataException {
        // not stored yet
        if (itemPath == null) throw new ObjectNotFoundException(); 

        Logger.msg(5, "ActivityDef.getCollectionResource(actName:"+getName()+") - Loading from collection:"+collName);

        Dependency resColl;
        String verStr = (mVersion == null || mVersion == -1) ? "last" : String.valueOf(mVersion);
        
        try {
            resColl = (Dependency) Gateway.getStorage().get(itemPath, ClusterStorage.COLLECTION + "/" + collName + "/" + verStr, null);
        }
        catch (PersistencyException e) {
            throw new InvalidDataException("Error loading description collection " + collName);
        }

        ArrayList<DescriptionObject> retArr = new ArrayList<DescriptionObject>();

        for (DependencyMember resMem : resColl.getMembers().list) {
            String resUUID = resMem.getChildUUID();
            Integer resVer = deriveVersionNumber(resMem.getBuiltInProperty(Version));

            if (resVer == null) {
                throw new InvalidDataException("Version is null for Item:" + itemPath + ", Collection:" + collName + ", DependencyMember:" + resUUID);
            }

            switch (collName) {
                case SCHCOL:
                    retArr.add(LocalObjectLoader.getSchema(resUUID, resVer));
                    break;
                case SCRCOL:
                    retArr.add(LocalObjectLoader.getScript(resUUID, resVer));
                    break;
                case SMCOL:
                    retArr.add(LocalObjectLoader.getStateMachine(resUUID, resVer));
                    break;
                case CompositeActivityDef.ACTCOL:
                    retArr.add(LocalObjectLoader.getActDef(resUUID, resVer));
                    break;
                default:
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
                case SchemaType:
                    return LocalObjectLoader.getSchema(resName, resVer);
                case ScriptName:
                    return LocalObjectLoader.getScript(resName, resVer);
                case StateMachineName:
                    return LocalObjectLoader.getStateMachine(resName, resVer);
                default:
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

    public Dependency makeDescCollection(String colName, DescriptionObject... descs) throws InvalidDataException {
        //TODO: restrictmembership based on kernel propdef
        Dependency descDep = new Dependency(colName);
        if (mVersion != null && mVersion > -1) {
            descDep.setVersion(mVersion);
        }
        
        for (DescriptionObject thisDesc : descs) {
            if (thisDesc == null) continue;
            try {
                DependencyMember descMem = descDep.addMember(thisDesc.getItemPath());
                descMem.setBuiltInProperty(Version, thisDesc.getVersion());
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

        retArr.put(makeDescCollection("Schema",       getSchema()));
        retArr.put(makeDescCollection("Script",       getScript()));
        retArr.put(makeDescCollection("StateMachine", getStateMachine()));

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
