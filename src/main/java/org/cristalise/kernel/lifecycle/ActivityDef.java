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

import static org.cristalise.kernel.collection.BuiltInCollections.QUERY;
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA;
import static org.cristalise.kernel.collection.BuiltInCollections.SCRIPT;
import static org.cristalise.kernel.collection.BuiltInCollections.STATE_MACHINE;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;
import static org.cristalise.kernel.process.resource.BuiltInResources.ELEM_ACT_DESC_RESOURCE;

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
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.WfVertex;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

/**
 * 
 */
public class ActivityDef extends WfVertexDef implements C2KLocalObject, DescriptionObject {

    //FIXME: ActivityDef should not extend WfVertexDef because is not part of the graph (check ActivitySlotDef instead)
    private Integer mVersion = null;  // null is 'last',previously was -1
    public boolean  changed  = false;

    ItemPath        itemPath;

    Schema          actSchema;
    Script          actScript;
    Query           actQuery;
    StateMachine    actStateMachine;

    public ActivityDef() {
        mErrors = new Vector<String>(0, 1);
        setProperties(new WfCastorHashMap());
        setIsLayoutable(false);
    }

    @Override
    public void setID(int id) {
        super.setID(id);
        if (getName() == null || "".equals(getName())) setName(String.valueOf(id));
    }

    @Override
    public String getItemID() {
        return (itemPath != null) ? itemPath.getUUID().toString() : null;
    }

    @Override
    public void setVersion(Integer v) {
        mVersion = v;
    }

    @Override
    public Integer getVersion() {
        return mVersion;
    }

    @Override
    public String getErrors() {
        return super.getErrors();
    }

    @Override
    public boolean verify() {
        return true;
    }

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

    /**
     * 
     */
    public void configureInstance(WfVertex act) throws InvalidDataException, ObjectNotFoundException {
        super.configureInstance(act);

        try {
            for (String collName : Gateway.getStorage().getClusterContents(itemPath, ClusterStorage.COLLECTION)) {
                Logger.msg(5, "ActivityDef.configureInstance("+getName()+") - Processing collection:"+collName);

                String verStr = (mVersion == null || mVersion == -1) ? "last" : String.valueOf(mVersion);
                Dependency dep = null;

                try {
                    dep = (Dependency) Gateway.getStorage().get(itemPath, ClusterStorage.COLLECTION+"/"+collName+"/"+verStr, null);
                }
                catch (ObjectNotFoundException e) {
                    if(Logger.doLog(8)) Logger.warning("Unavailable Collection path:"+itemPath+"/"+ClusterStorage.COLLECTION+"/"+collName+"/"+verStr);
                }
                catch (PersistencyException e) {
                    Logger.error(e);
                    throw new InvalidDataException("Collection:"+collName+" error:"+e.getMessage());
                }

                if (dep != null) dep.addToVertexProperties(act.getProperties());
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
            actSchema = LocalObjectLoader.getSchema(getProperties());
        }
        return actSchema;
    }

    public Script getScript() throws InvalidDataException, ObjectNotFoundException {
        if (actScript == null) {
            Logger.msg(1, "ActivityDef.getScript(actName:"+getName()+") - Loading ...");
            actScript = LocalObjectLoader.getScript(getProperties());
        }
        return actScript;
    }

    public Query getQuery() throws InvalidDataException, ObjectNotFoundException {
        if (actQuery == null) {
            Logger.msg(1, "ActivityDef.getQuery(actName:"+getName()+") - Loading ...");
            actQuery = LocalObjectLoader.getQuery(getProperties());
        }
        return actQuery;
    }

    public StateMachine getStateMachine() throws InvalidDataException, ObjectNotFoundException {
        if (actStateMachine == null) {
            Logger.msg(1, "ActivityDef.getStateMachine(actName:"+getName()+") - Loading ...");
            actStateMachine = LocalObjectLoader.getStateMachine(getProperties());
        }
        return actStateMachine;
    }

    @Deprecated
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
                case QUERY:
                    retArr.add(LocalObjectLoader.getQuery(resUUID, resVer));
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

    public void setSchema(Schema actSchema) {
        this.actSchema = actSchema;
    }

    public void setScript(Script actScript) {
        this.actScript = actScript;
    }

    public void setQuery(Query actQuery) {
        this.actQuery = actQuery;
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
        retArr.put(makeDescCollection(QUERY,         getQuery()));
        retArr.put(makeDescCollection(STATE_MACHINE, getStateMachine()));

        return retArr;
    }

    @Override
    public void export(Writer imports, File dir) throws InvalidDataException, ObjectNotFoundException, IOException {
        String actXML;
        String tc = ELEM_ACT_DESC_RESOURCE.getTypeCode();

        try {
            actXML = Gateway.getMarshaller().marshall(this);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new InvalidDataException("Couldn't marshall activity def " + getActName());
        }
        
        FileStringUtility.string2File(new File(new File(dir, tc), getActName() + (getVersion() == null ? "" : "_" + getVersion()) + ".xml"), actXML);

        if (imports != null) {
            imports.write("<Activity " + getExportAttributes(tc) + ">" + getExportCollections() + "</Activity>\n");
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
                + (getScript() == null ? "" : "<Script       name=\"" + getScript().getName() + "\"       id=\"" + getScript().getItemID()       + "\" version=\"" + getScript().getVersion()       + "\"/>")
                + (getQuery()  == null ? "" : "<Query        name=\"" + getQuery().getName()  + "\"       id=\"" + getQuery().getItemID()        + "\" version=\"" + getQuery().getVersion()        + "\"/>");
    }
}
