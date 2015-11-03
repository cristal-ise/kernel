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
package org.cristalise.kernel.lifecycle;
import java.util.ArrayList;
import java.util.Vector;

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
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

/**
 * @version $Revision: 1.45 $ $Date: 2005/10/05 07:39:36 $
 * @author $Author: abranson $
 */
public class ActivityDef extends WfVertexDef implements C2KLocalObject, DescriptionObject
{
	public static final String SCHNAME = "SchemaType";
	public static final String SCHVER = "SchemaVersion";
	public static final String SCRNAME = "ScriptName";
	public static final String SCRVER = "ScriptVersion";
	public static final String SMNAME = "StateMachineName";
	public static final String SMVER = "StateMachineVersion";
	
	public static final String SCHCOL = "schema";
	public static final String SCRCOL = "script";
	public static final String SMCOL = "statemachine";
	
	private int mId = -1;
	private String mName = "";
	private int mVersion = -1; // unnumbered 'last'
	public boolean changed = false;
	Schema actSchema;
	Script actScript;
	StateMachine actStateMachine;
	ItemPath itemPath;

	public ActivityDef()
	{
		mErrors = new Vector<String>(0, 1);
		setProperties(new WfCastorHashMap());
		getProperties().put(SMNAME, getDefaultSMName());
		setIsLayoutable(false);
	}
	
	protected String getDefaultSMName() {
		return "Default";
	}
	
	@Override
	public void setID(int id)
	{
		mId = id;
		if (mName.equals(""))
			setName(String.valueOf(id));
	}
	
	@Override
	public int getID()
	{
		return mId;
	}


	/**
	 * @see org.cristalise.kernel.graph.model.Vertex#setName(java.lang.String)
	 */
	@Override
	public void setName(String n)
	{
		mName = n;
	}
	/**
	 * @see org.cristalise.kernel.graph.model.Vertex#getName()
	 */
	@Override
	public String getName()
	{
		return mName;
	}

	@Override
	public void setVersion(int v)
	{
		mVersion = v;
	}
	/**
	 * @see org.cristalise.kernel.graph.model.Vertex#getName()
	 */
	@Override
	public int getVersion()
	{
		return mVersion;
	}
	/**
	 * @see org.cristalise.kernel.lifecycle.WfVertexDef#getErrors()
	 */
	@Override
	public String getErrors()
	{
		return super.getErrors();
	}
	/**
	 * Method linkToSlot.
	 *
	 * @param actSl
	 * @param name
	 */
	public void linkToSlot(ActivitySlotDef actSl, String name, String name2)
	{
		actSl.setActivityDef(itemPath.getUUID().toString());
		actSl.getProperties().put(NAME, name2.replace('/', '_'));
		actSl.setName(name+" slot");
		setName(FileStringUtility.convert(name));
	}
	/**
	 * @see org.cristalise.kernel.lifecycle.WfVertexDef#verify()
	 */
	@Override
	public boolean verify()
	{
		return true;
	}
	/**
	 * @see org.cristalise.kernel.entity.C2KLocalObject#getClusterType()
	 */
	@Override
	public String getClusterType()
	{
		return null;
	}
	public String getActName()
	{
		return getName();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.cristalise.kernel.lifecycle.commonInterface.ActType#getDescName()
	 */
	public String getDescName()
	{
		return getName();
	}

	public void configureInstanceProp(CastorHashMap props, DescriptionObject desc, String nameProp, String verProp) {
		if (desc != null) {
			props.put(nameProp, desc.getItemPath().getUUID().toString());
			props.put(verProp, desc.getVersion());
		}
	}
	@Override
	public WfVertex instantiate() throws ObjectNotFoundException, InvalidDataException{
		return instantiate(getName());
	}
	
	public WfVertex instantiate(String name) throws ObjectNotFoundException, InvalidDataException
	{
        Activity act = new Activity();
        configureInstance(act);
		act.setName(name);
		act.setType(getName());
		
		configureInstanceProp(act.getProperties(), actSchema, SCHNAME, SCHVER);
		configureInstanceProp(act.getProperties(), actScript, SCRNAME, SCRVER);
		configureInstanceProp(act.getProperties(), actStateMachine, SMNAME, SMVER);
		return act;
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
			try {
				actSchema = (Schema)getCollectionResource(SCHCOL)[0];
			} catch (ObjectNotFoundException ex) {			
				actSchema = (Schema)getPropertyResource(SCHNAME, SCHVER);
			}
		}
		return actSchema;
	}
	
	public Script getScript() throws InvalidDataException, ObjectNotFoundException {
		if (actScript == null) {
			try {
				actScript = (Script)getCollectionResource(SCRCOL)[0];
			} catch (ObjectNotFoundException ex) {
			actScript = (Script)getPropertyResource(SCRNAME, SCRVER);
			}
		}
		return actScript;
	}
	
	public StateMachine getStateMachine() throws InvalidDataException, ObjectNotFoundException {
		if (actStateMachine == null) {
			try {
				actStateMachine = (StateMachine)getCollectionResource(SMCOL)[0];
			} catch (ObjectNotFoundException ex) {
				actStateMachine = (StateMachine)getPropertyResource(SMNAME, SMVER);
			}
		}
		return actStateMachine;
	}
	
	protected DescriptionObject[] getCollectionResource(String collName) throws ObjectNotFoundException, InvalidDataException {
		if (itemPath == null) throw new ObjectNotFoundException(); // not stored yet
		Dependency resColl;
		String verStr = mVersion==-1?"last":String.valueOf(mVersion);
		try {
			resColl = (Dependency) Gateway.getStorage().get(itemPath, ClusterStorage.COLLECTION+"/"+collName+"/"+verStr, null);
		} catch (PersistencyException e) {
			throw new InvalidDataException("Error loading description collection "+collName);
		}
		ArrayList<DescriptionObject> retArr = new ArrayList<DescriptionObject>();
		for (DependencyMember resMem : resColl.getMembers().list) {
			String resUUID = resMem.getChildUUID();
			Integer resVer = deriveVersionNumber(resMem.getProperties().get("Version"));
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
		if (retArr.size()==0) throw new ObjectNotFoundException();
		return retArr.toArray(new DescriptionObject[retArr.size()]);
	}
	
	protected DescriptionObject getPropertyResource(String nameProp, String verProp) throws InvalidDataException, ObjectNotFoundException {
		if (Gateway.getLookup() == null) return null;
		String resName = (String)getProperties().get(nameProp);
		
		if (!(getProperties().isAbstract(nameProp)) && resName != null && resName.length() > 0) {
			Integer resVer = deriveVersionNumber(getProperties().get(verProp));
			if (resVer == null && !(getProperties().isAbstract(verProp))) 
				throw new InvalidDataException("Invalid version property '"+resVer+"' in "+nameProp+" for "+getName());
			switch (nameProp) {
			case SCHNAME:
				return LocalObjectLoader.getSchema(resName, resVer);
			case SCRNAME:
				return LocalObjectLoader.getScript(resName, resVer);
			case SMNAME:
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
	
	protected Dependency makeDescCollection(String colName, DescriptionObject... descs) throws InvalidDataException {
		Dependency descDep = new Dependency(colName);
		for (DescriptionObject thisDesc : descs) {
			try {
				DependencyMember descMem = descDep.addMember(thisDesc.getItemPath());
				descMem.getProperties().put("Version", thisDesc.getVersion());
			} catch (Exception e) { 
				Logger.error(e);
				throw new InvalidDataException("Problem creating description collection for "+thisDesc+ " in "+getName());
			}
		}
		return descDep;

	}
	@Override
	public CollectionArrayList makeDescCollections() throws InvalidDataException, ObjectNotFoundException {
		CollectionArrayList retArr = new CollectionArrayList();
		
		if (getSchema() != null)
			retArr.put(makeDescCollection("Schema", actSchema));
		
		if (getScript() != null)
			retArr.put(makeDescCollection("Script", actScript));
		
		if (getStateMachine() != null)
			retArr.put(makeDescCollection("StateMachine", actStateMachine));
		
		return retArr;
	}
}
