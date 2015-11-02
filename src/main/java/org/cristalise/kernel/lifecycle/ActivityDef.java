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
import java.util.Vector;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.WfVertex;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.ItemPath;
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
	private int mId = -1;
	private String mName = "";
	private int mVersion = -1;
	public boolean changed = false;
	Schema actSchema;
	Script actScript;
	StateMachine actStateMachine;
	ItemPath itemPath;

	/**
	 * @see java.lang.Object#Object()
	 */
	public ActivityDef()
	{
		mErrors = new Vector<String>(0, 1);
		setProperties(new WfCastorHashMap());
		getProperties().put("StateMachineName", getDefaultSMName());
		setIsLayoutable(false);
	}
	
	protected String getDefaultSMName() {
		return "Default";
	}
	
	/**
	 * @see org.cristalise.kernel.graph.model.Vertex#setID(int)
	 */
	@Override
	public void setID(int id)
	{
		mId = id;
		if (mName.equals(""))
			setName(String.valueOf(id));
	}
	/**
	 * @see org.cristalise.kernel.graph.model.Vertex#getID()
	 */
	@Override
	public int getID()
	{
		return mId;
	}
	@Override
	public void setProperties(CastorHashMap props) {
		super.setProperties(props);
		if (Gateway.getLookup() != null) { // resolve referenced desc items if we have a gateway
			// try schema
			String schemaName = (String)getProperties().get("SchemaType");
			if (schemaName != null && schemaName.length() > 0)
				try {
					Integer schemaVersion = getVersionNumberProperty("SchemaVersion");
					actSchema = LocalObjectLoader.getSchema(schemaName, schemaVersion);
				} catch (Exception ex) {
					Logger.error(ex);
					Logger.error("Schema definition reference property invalid");
				}
	
			// try script
			String scriptName = (String)getProperties().get("ScriptName");
			if (scriptName != null && scriptName.length() > 0)
				try {
					Integer scriptVersion = getVersionNumberProperty("SchemaVersion");
					actScript = new Script(scriptName, scriptVersion);
				} catch (Exception ex) {
					Logger.error(ex);
					Logger.error("Script definition reference property invalid");
				}
			
			// try script
			String smName = (String)getProperties().get("StateMachineName");
			if (smName != null && smName.length() > 0)
				try {
					Integer smVersion = getVersionNumberProperty("StateMachineVersion");
					actStateMachine = LocalObjectLoader.getStateMachine(smName, smVersion);
				} catch (Exception ex) {
					Logger.error(ex);
					Logger.error("State Machine definition reference property invalid");
				}
		}			
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
		actSl.setActivityDef(FileStringUtility.convert(name));
		actSl.getProperties().put("Name", name2.replace('/', '_'));
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
}
