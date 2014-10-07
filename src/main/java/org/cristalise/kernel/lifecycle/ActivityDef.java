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
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;

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
	/**
	 * @see java.lang.Object#Object()
	 */
	public ActivityDef()
	{
		mErrors = new Vector<String>(0, 1);
		setProperties(new WfCastorHashMap());
		setIsLayoutable(false);
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
}
