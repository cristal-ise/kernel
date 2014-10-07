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
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.traversal.GraphTraversal;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.WfVertex;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.LocalObjectLoader;

/**
 * @version $Revision: 1.46 $ $Date: 2005/10/05 07:39:36 $
 * @author $Author: abranson $
 */
public class ActivitySlotDef extends WfVertexDef
{
	private String activityDef;

	/**
	 * Method setActivityDef.
	 *
	 * @param oActivityDef
	 */
	public void setActivityDef(String oActivityDef)
	{
		activityDef = oActivityDef;
	}
	/**
	 * Method getActivityDef.
	 *
	 * @return String
	 */
	public String getActivityDef()
	{
		return activityDef;
	}
	/**
	 * @see java.lang.Object#Object()
	 */
	public ActivitySlotDef()
	{
		getProperties().put("Name", "");
		getProperties().put("Version", 0);
	}

	public ActivityDef getTheActivityDef() throws ObjectNotFoundException, InvalidDataException
	{
		ActivityDef actDef = LocalObjectLoader.getActDef(getActivityDef(), getVersionNumberProperty("Version"));
		if (actDef instanceof CompositeActivityDef)
			mIsComposite = true;
		return actDef;
	}
	/**
	 * @see org.cristalise.kernel.lifecycle.WfVertexDef#verify()
	 */
	/** launch the verification of the ActivityDef */
	@Override
	public boolean verify()
	{
		mErrors.removeAllElements();
		boolean err = true;
		int nbInEdgres = getInEdges().length;
		int nbOutEdges = getOutEdges().length;
		if (nbInEdgres == 0 && this.getID() != getParent().getChildrenGraphModel().getStartVertexId())
		{
			mErrors.add("Unreachable");
			err = false;
		}
		if (nbInEdgres > 1)
		{
			mErrors.add("Bad nb of previous");
			err = false;
		}
		if (nbOutEdges > 1)
		{
			mErrors.add("too many next");
			err = false;
		}
		if (nbOutEdges == 0)
		{
			if (!((CompositeActivityDef) getParent()).hasGoodNumberOfActivity())
			{
				mErrors.add("too many endpoints");
				err = false;
			}
		}
		
		Vertex[] allSiblings = getParent().getChildGraphModel().getVertices();
		String thisName = (String)getProperties().get("Name");
		if (thisName == null || thisName.length()==0) mErrors.add("Slot name is empty");
		else for (Vertex v : allSiblings) {
			if (v instanceof ActivitySlotDef && v.getID()!=getID()) {
				ActivitySlotDef otherSlot = (ActivitySlotDef)v;
				String otherName = (String)otherSlot.getProperties().get("Name");
				if (otherName != null && otherName.equals(thisName)) {
					mErrors.add("Duplicate slot name");
					err = false;
				}
			}
		}
		
		KeyValuePair[] props;
		try { 
			props = getTheActivityDef().getProperties().getKeyValuePairs();
			for (KeyValuePair prop : props) {
				if (prop.isAbstract() && !getProperties().containsKey(prop.getKey())) {
					mErrors.add("Abstract property '"+prop.getKey()+"' not defined in slot");
					err = false;
				}
			}
		} catch (Exception ex) { }


		// Loop check
		Vertex[] outV = getOutGraphables();
		Vertex[] anteVertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, false);
		boolean errInLoop = false;
		for (Vertex element : outV) {
			for (Vertex anteVertice : anteVertices)
				if (!loop() && element.getID() == anteVertice.getID())
					errInLoop = true;
		}
		if (errInLoop)
		{
			mErrors.add("Problem in Loop");
			err = false;
		}
		return err;
	}
	/**
	 * Method getNextWfVertices.
	 *
	 * @return WfVertexDef[]
	 */
	public WfVertexDef[] getNextWfVertices()
	{
		return (WfVertexDef[]) getOutGraphables();
	}
	/**
	 * @see org.cristalise.kernel.graph.model.GraphableVertex#isLayoutable()
	 */
	/**
	 * @see org.cristalise.kernel.graph.model.GraphableVertex#getIsLayoutable()
	 */
	public boolean isLayoutable()
	{
		return true;
	}
	/**
	 * Method getInfo.
	 *
	 * @return CastorHashMap
	 */
	private void configureInstance(Activity act)
	{
		KeyValuePair[] k = getProperties().getKeyValuePairs();
		for (KeyValuePair element : k)
			act.getProperties().put(element.getKey(), element.getValue(), element.isAbstract());
		act.setCentrePoint(getCentrePoint());
		act.setOutlinePoints(getOutlinePoints());
		act.setInEdgeIds(getInEdgeIds());
		act.setOutEdgeIds(getOutEdgeIds());
		act.setName(getActName());
		act.setID(getID());
	}

	public String getActName()
	{
		return (String) getProperties().get("Name");
	}

	@Override
	public WfVertex instantiate() throws ObjectNotFoundException, InvalidDataException {
		Activity newActivity = (Activity)getTheActivityDef().instantiate();
		configureInstance(newActivity);
		if (newActivity.getProperties().getAbstract().size() > 0) {
			throw new InvalidDataException("Abstract properties not overridden: "+newActivity.getProperties().getAbstract().toString());
		}
		return newActivity;
	}
}
