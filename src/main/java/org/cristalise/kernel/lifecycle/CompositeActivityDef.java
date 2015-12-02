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
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.graph.model.TypeNameAndConstructionInfo;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Next;
import org.cristalise.kernel.lifecycle.instance.WfVertex;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

/**
 * @version $Revision: 1.93 $ $Date: 2005/10/05 07:39:36 $
 * @author $Author: abranson $
 */
public class CompositeActivityDef extends ActivityDef
{
	public static final String ACTCOL = "Activity";
	private final TypeNameAndConstructionInfo[] mVertexTypeNameAndConstructionInfo =
		{
			new TypeNameAndConstructionInfo("Activity", "Atomic"),
			new TypeNameAndConstructionInfo("Composite", "Composite"),
			new TypeNameAndConstructionInfo("AND Split", "And"),
			new TypeNameAndConstructionInfo("OR Split", "Or"),
			new TypeNameAndConstructionInfo("XOR Split", "XOr"),
			new TypeNameAndConstructionInfo("Join", "Join"),
			new TypeNameAndConstructionInfo("Loop", "Loop"),
			};
	private final TypeNameAndConstructionInfo[] mEdgeTypeNameAndConstructionInfo =
		{
			new TypeNameAndConstructionInfo("Next Edge", "Next")
		};
	public TypeNameAndConstructionInfo[] getVertexTypeNameAndConstructionInfo()
	{
		return mVertexTypeNameAndConstructionInfo;
	}
	public TypeNameAndConstructionInfo[] getEdgeTypeNameAndConstructionInfo()
	{
		return mEdgeTypeNameAndConstructionInfo;
	}

	public CompositeActivityDef()
	{
		super();
		getProperties().put("Abortable", false);
		setChildrenGraphModel(new GraphModel(new WfVertexDefOutlineCreator()));
		setIsComposite(true);
	}
	
	@Override
	protected String getDefaultSMName() {
		return "CompositeActivity";
	}

	/**
	 * Method addNextDef.
	 *
	 * @param origin
	 * @param terminus
	 * @return NextDef
	 */
	public NextDef addNextDef(WfVertexDef origin, WfVertexDef terminus)
	{
		NextDef returnNxt = new NextDef(origin, terminus);
		getChildrenGraphModel().addEdgeAndCreateId(returnNxt, origin, terminus);
		return returnNxt;
	}
	/**
	 * Method addExistingActivityDef.
	 *
	 * @param actDef
	 * @param point
	 */
	public ActivitySlotDef addExistingActivityDef(String name, ActivityDef actDef, GraphPoint point)
	{
		changed = true;
		ActivitySlotDef child = new ActivitySlotDef(name, actDef);
		addChild(child, point);
        return child;
	}
	/**
	 * Method newChild.
	 *
	 * @param Name
	 * @param Type
	 * @param location
	 * @return WfVertexDef
	 * @throws InvalidDataException 
	 * @throws ObjectNotFoundException 
	 */
	public WfVertexDef newChild(String Name, String Type, Integer version, GraphPoint location) throws ObjectNotFoundException, InvalidDataException
	{
		changed = true;
		WfVertexDef child;
		if (Type.equals("Or"))
		{
			child = new OrSplitDef();
			addChild(child, location);
			Logger.msg(5, Type + " " + child.getID() + " added to " + this.getID());
		}
		else if (Type.equals("XOr"))
		{
			child = new XOrSplitDef();
			addChild(child, location);
			Logger.msg(5, Type + " " + child.getID() + " added to " + this.getID());
		}
		else if (Type.equals("And"))
		{
			child = new AndSplitDef();
			addChild(child, location);
			Logger.msg(5, Type + " " + child.getID() + " added to " + this.getID());
		}
		else if (Type.equals("Loop"))
		{
			child = new LoopDef();
			addChild(child, location);
			Logger.msg(5, Type + " " + child.getID() + " added to " + this.getID());
		}
		else if (Type.equals("Atomic"))
		{
			ActivityDef act = LocalObjectLoader.getElemActDef(Name, version);
			child = new ActivitySlotDef(Name, act);
			addChild(child, location);
			Logger.msg(5, Type + " " + child.getID() + " added to " + this.getID());
		}
		else if (Type.equals("Join"))
		{
			child = new JoinDef();
			child.getProperties().put("Type", "Join");
			addChild(child, location);
			Logger.msg(5, Type + " " + child.getID() + " added to " + this.getID());
		}
		else if (Type.equals("Route"))
		{
			child = new JoinDef();
			child.getProperties().put("Type", "Route");
			addChild(child, location);
			Logger.msg(5, Type + " " + child.getID() + " added to " + this.getID());
		}
		else
		{
			CompositeActivityDef act = LocalObjectLoader.getCompActDef(Name, version);
			child = new ActivitySlotDef(Name, act);
			addChild(child, location);
			Logger.msg(5, Type + " " + child.getID() + " added to " + this.getID());
		}
		return child;
	}
	/**
	 * Method instantiateAct.
	 *
	 * @return CompositeActivity
	 */
	@Override
	public WfVertex instantiate() throws ObjectNotFoundException, InvalidDataException {
		return instantiate(getName());
	}

	@Override
	public WfVertex instantiate(String name) throws ObjectNotFoundException, InvalidDataException
	{
		CompositeActivity cAct = new CompositeActivity();
        configureInstance(cAct);
        cAct.setType(getName());
        cAct.setName(name);
        GraphableVertex[] vertexDefs = getLayoutableChildren();
		WfVertex[] wfVertices = new WfVertex[vertexDefs.length];
		for (int i = 0; i < vertexDefs.length; i++)
		{
			WfVertexDef vertDef = (WfVertexDef)vertexDefs[i];
			wfVertices[i] = vertDef.instantiate();
			wfVertices[i].setParent(cAct);
		}
		Next[] nexts = new Next[getChildrenGraphModel().getEdges().length];
		for (int i = 0; i < getChildrenGraphModel().getEdges().length; i++)
		{
			NextDef nextDef = (NextDef) getChildrenGraphModel().getEdges()[i];
			nexts[i] = nextDef.instantiate();
			nexts[i].setParent(cAct);
		}
		cAct.getChildrenGraphModel().setStartVertexId(getChildrenGraphModel().getStartVertexId());
		cAct.getChildrenGraphModel().setEdges(nexts);
		cAct.getChildrenGraphModel().setVertices(wfVertices);
		cAct.getChildrenGraphModel().setNextId(getChildrenGraphModel().getNextId());
		cAct.getChildrenGraphModel().resetVertexOutlines();
		return cAct;
	}

	@Override
	public CollectionArrayList makeDescCollections() throws InvalidDataException, ObjectNotFoundException {
		CollectionArrayList retArr = super.makeDescCollections();
		retArr.put(makeActDefCollection());
		return retArr;
	}
	
	public Dependency makeActDefCollection() throws InvalidDataException {
		ArrayList<ActivityDef> descs = new ArrayList<ActivityDef>();
		for (GraphableVertex elem : getChildren()) {
			try {
				if (elem instanceof ActivitySlotDef) {
					ActivityDef actDef = ((ActivitySlotDef)elem).getTheActivityDef();
					if (!descs.contains(actDef)) descs.add(actDef);
				}
			} catch (Exception ex) {
				Logger.error(ex);
			}
		}
		return makeDescCollection(ACTCOL, descs.toArray(new ActivityDef[descs.size()]));
	}
	/**
	 * Method hasGoodNumberOfActivity.
	 *
	 * @return boolean
	 */

	public boolean hasGoodNumberOfActivity()
	{
		int endingAct = 0;
		GraphableVertex[] graphableVertices = this.getLayoutableChildren();
		if (graphableVertices != null)
			for (GraphableVertex graphableVertice : graphableVertices) {
				WfVertexDef vertex = (WfVertexDef) graphableVertice;
				if (getChildrenGraphModel().getOutEdges(vertex).length == 0)
					endingAct++;
			}
		if (endingAct > 1)
			return false;
		return true;
	}

	/**
	 * @see org.cristalise.kernel.graph.model.GraphableVertex#getPath()
	 */
	@Override
	public String getPath()
	{
		if (getParent() == null)
			return getName();
		return super.getPath();
	}
	@Override
	public void setChildrenGraphModel(GraphModel childrenGraph) {
		super.setChildrenGraphModel(childrenGraph);
		childrenGraph.setVertexOutlineCreator(new WfVertexDefOutlineCreator());
	}
	//deprecated
	public String[] getCastorNonLayoutableChildren() {
		return new String[0];
	}

	public void setCastorNonLayoutableChildren(String[] dummy) { }
	
	@Override
	public boolean verify() {
		boolean err = super.verify();
    	GraphableVertex[] vChildren = getChildren();
        for (int i = 0; i < vChildren.length; i++)
        {
        	WfVertexDef wfvChild = (WfVertexDef)vChildren[i];
            if (!(wfvChild.verify()))
            {
                mErrors.add(wfvChild.getName()+": "+wfvChild.getErrors());
                err = false;
            }
        }
        return err;
	}
	
	@Override
	public void export(Writer imports, File dir) throws InvalidDataException, ObjectNotFoundException, IOException {
		if (getSchema() != null) 
			actSchema.export(imports, dir);
		if (getScript() != null)
			actScript.export(imports, dir);
		if (getStateMachine() != null)
			actStateMachine.export(imports, dir);
		
		for (int i=0; i<getChildren().length; i++) { // export routing scripts and schemas
			GraphableVertex vert = getChildren()[i];
			if (vert instanceof AndSplitDef)
				try {
					((AndSplitDef)vert).getRoutingScript().export(imports, dir);
				} catch (ObjectNotFoundException ex) {}
			if (vert instanceof ActivitySlotDef) 
				((ActivitySlotDef)vert).getTheActivityDef().export(imports, dir);
			
		}
		
		// export marshalled compAct
		String compactXML;
		try {
			compactXML = Gateway.getMarshaller().marshall(this);
		} catch (Exception e) {
			Logger.error(e);
			throw new InvalidDataException("Couldn't marshall composite activity def "+getActName());
		}
		FileStringUtility.string2File(new File(new File(dir, "CA"), getActName()+(getVersion()==null?"":"_"+getVersion())+".xml"), compactXML);
		if (imports!=null) {
			imports.write("<Resource name=\""+getActName()+"\" "
					+(getItemPath()==null?"":"id=\""+getItemID()+"\"")
					+(getVersion()==null?"":"version=\""+getVersion()+"\" ")
					+"type=\"CA\">boot/CA/"+getActName()
					+(getVersion()==null?"":"_"+getVersion())+".xml</Resource>\n");
		}
	}
}
