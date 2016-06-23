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
package org.cristalise.kernel.graph.model;

/**
* @version $Revision: 1.24 $ $Date: 2005/10/05 07:39:37 $
* @author  $Author: abranson $
*/
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;

public abstract class GraphableVertex extends Vertex
{
//	public static final String NAME = "Name";
	private CastorHashMap mProperties = null;
	private boolean mIsLayoutable;
	protected boolean mIsComposite;
	private GraphModel mChildrenGraphModel;
	
	public GraphableVertex()
	{
		mProperties = new CastorHashMap();
	}
	public void setProperties(CastorHashMap props)
	{
		mProperties = props;
	}
	public CastorHashMap getProperties()
	{
		return mProperties;
	}
	
	protected Integer deriveVersionNumber(Object val) throws InvalidDataException {
		if (val == null || val.equals("") || val.toString().equals("-1")) return null;
		try {
			return Integer.valueOf(val.toString());
		} catch (NumberFormatException ex) {
			throw new InvalidDataException("Invalid version number : "+val.toString());
		}
	}
	public KeyValuePair[] getKeyValuePairs()
	{
		return mProperties.getKeyValuePairs();
	}
	public void setKeyValuePairs(KeyValuePair[] pairs)
	{
		mProperties.setKeyValuePairs(pairs);
	}
	/** @associates Graphable that is directly containing it*/
	private GraphableVertex parent;
	/**
	 * Returns the parent.
	 * @return Graphable
	 */
	public GraphableVertex getParent()
	{
		return parent;
	}
	/**
	 * Sets the parent.
	 * @param parent The parent to set
	 */
	public void setParent(GraphableVertex parent)
	{
		if (this.equals(parent))
			throw new ExceptionInInitializerError();
		this.parent = parent;
	}
	@Override
	public GraphModel getChildrenGraphModel()
	{
		return mChildrenGraphModel;
	}
	@Override
	public Object getCreationContext()
	{
		return this;
	}
	public Vertex[] getOutGraphables()
	{
		if (parent == null)
			return new Vertex[0]; // none if no parent
		return parent.mChildrenGraphModel.getOutVertices(this);
	}
	public DirectedEdge[] getOutEdges()
	{
		if (parent == null)
			return new DirectedEdge[0]; // none if no parent
		return parent.mChildrenGraphModel.getOutEdges(this);
	}
	public DirectedEdge[] getInEdges()
	{
		if (parent == null)
			return new DirectedEdge[0]; // none if no parent
		DirectedEdge[] edges = getParent().mChildrenGraphModel.getInEdges(this);
		if (edges != null)
			return edges;
		else
			return new DirectedEdge[0];
	}
	public GraphableVertex[] getChildren()
	{
		return getLayoutableChildren();
	}

	public DirectedEdge[] getChildrenEdges()
	{
		if (getIsComposite())
		{
			return getChildrenGraphModel().getEdges();
		}
		return null;
	}

	public GraphableVertex[] getLayoutableChildren()
	{
		if (getIsComposite() && mChildrenGraphModel != null)
		{
			Vertex[] vs = mChildrenGraphModel.getVertices();
			GraphableVertex[] gvs = new GraphableVertex[vs.length];
			for (int i = 0; i < vs.length; i++)
			{
				gvs[i] = (GraphableVertex) vs[i];
			}
			return gvs;
		}
		return null;
	}

	/**@returns the Graphable searched or null if not this or children*/
	public GraphableVertex search(String ids)
	{
		if (getName().equals(ids))
			return this;
		if (String.valueOf(getID()).equals(ids))
			return this;
		if (getIsComposite())
		{
			GraphableVertex[] graphables = getChildren();
			if (ids.startsWith(String.valueOf(getID())))
				ids = ids.substring(ids.indexOf("/") + 1);
			else if (ids.startsWith(getName()))
				ids = ids.substring(getName().length() + 1);
			else if (ids.startsWith(getPath()))
				ids = ids.substring(getPath().length() + 1);
			else
                return null;

            for (GraphableVertex graphable : graphables) {
					GraphableVertex grap = graphable.search(ids);
					if (grap != null) return grap;
			}
        }
		return null;
	}
	/**
	 * Returns the isLayoutable.
	 * @return boolean
	 */
	public boolean getIsLayoutable()
	{
		return mIsLayoutable;
	}
	/**
	 * Sets the isLayoutable.
	 * @param isLayoutable The isLayoutable to set
	 */
	public void setIsLayoutable(boolean isLayoutable)
	{
		mIsLayoutable = isLayoutable;
	}
	/**
	 * Returns the isComposite.
	 * @return boolean
	 */
	public boolean getIsComposite()
	{
		return mIsComposite;
	}
	/**
	 * Sets the isComposite.
	 * @param isComposite The isComposite to set
	 */
	public void setIsComposite(boolean isComposite)
	{
		mIsComposite = isComposite;
	}

	public void addChild(GraphableVertex graphableVertex, GraphPoint g)
	{
		getChildrenGraphModel().addVertexAndCreateId(graphableVertex, g);
		graphableVertex.setParent(this);
	}

	/**
	 * Sets the childrenGraph.
	 * @param childrenGraph The childrenGraph to set
	 * @throws InvalidDataException - if the graph model wasn't valid in this context
	 */
	public void setChildrenGraphModel(GraphModel childrenGraph) throws InvalidDataException
	{
		mChildrenGraphModel = childrenGraph;
		DirectedEdge[] edges = mChildrenGraphModel.getEdges();
		GraphableVertex[] graphables = this.getLayoutableChildren();
		if (graphables != null)
			for (GraphableVertex graphable : graphables)
				graphable.setParent(this);
		if (edges != null)
			for (DirectedEdge edge : edges)
				((GraphableEdge) edge).setParent(this);
        childrenGraph.setContainingVertex(this);
	}

	/**
	 * @see org.cristalise.kernel.graph.model.Vertex#getCentrePoint()
	 */
	@Override
	public GraphPoint getCentrePoint()
	{
		if (!getIsLayoutable())
			return null;
		return super.getCentrePoint();
	}
	/**
	 * @see org.cristalise.kernel.graph.model.Vertex#getInEdgeIds()
	 */
	@Override
	public int[] getInEdgeIds()
	{
		if (!getIsLayoutable())
			return null;
		return super.getInEdgeIds();
	}
	/**
	 * @see org.cristalise.kernel.graph.model.Vertex#getOutEdgeIds()
	 */
	@Override
	public int[] getOutEdgeIds()
	{
		if (!getIsLayoutable())
			return null;
		return super.getOutEdgeIds();
	}
	/**
	 * @see org.cristalise.kernel.graph.model.Vertex#getOutlinePoints()
	 */
	@Override
	public GraphPoint[] getOutlinePoints()
	{
		if (!getIsLayoutable())
			return null;
		return super.getOutlinePoints();
	}
	public String getPath()
	{
		if (getName() != null && !getName().equals(""))
			return getParent().getPath() + "/" + getName();
		return getParent().getPath() + "/" + getID();
	}

	public Object getBuiltInProperty(BuiltInVertexProperties prop) {
	    return mProperties.get(prop.getAlternativeName());
	}

	public void setBuiltInProperty(BuiltInVertexProperties prop, Object val) {
	    mProperties.put(prop.getAlternativeName(), val);
	}

}