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
package org.cristalise.kernel.lifecycle.instance;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.traversal.GraphTraversal;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.Logger;

/**
 * @version $Revision: 1.35 $ $Date: 2005/05/10 15:14:54 $
 * @author $Author: abranson $
 */
public class Loop extends XOrSplit
{
	/**
	 * @see java.lang.Object#Object()
	 */
	public Loop()
	{
		super();
	}
	/**
	 * @see org.cristalise.kernel.lifecycle.instance.WfVertex#loop()
	 */
	@Override
	public boolean loop()
	{
		return true;
	}
	@Override
	public void followNext(Next activeNext, AgentPath agent, ItemPath itemPath) throws InvalidDataException
	{
		WfVertex v = activeNext.getTerminusVertex();
		if (!isInPrev(v))
			v.run(agent, itemPath);
		else
		{
			v.reinit(getID());
			v.run(agent, itemPath);
		}
	}
	/**
	 * @throws InvalidDataException 
	 * @see org.cristalise.kernel.lifecycle.instance.WfVertex#reinit(int)
	 */
	@Override
	public void reinit(int idLoop) throws InvalidDataException
	{
		Logger.msg(8, "Loop.reinit");
		if (idLoop == getID())
			return;
		else
		{
			Vertex[] outVertices = getOutGraphables();
			for (int j = 0; j < outVertices.length; j++)
			{
				if (!isInPrev(outVertices[j]))
					 ((WfVertex) outVertices[j]).reinit(idLoop);
			}
		}
	}
	/**
	 * @see org.cristalise.kernel.lifecycle.instance.WfVertex#verify()
	 */
	@Override
	public boolean verify()
	{
		boolean err = super.verify();
		Vertex[] nexts = getOutGraphables();
		Vertex[] anteVertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, false);
		int k = 0;
		int l = 0;
		Vertex[] brothers = getParent().getChildren();
		for (Vertex brother : brothers)
			if (brother instanceof Loop)
				l++;
		for (Vertex next : nexts) {
			for (Vertex anteVertice : anteVertices)
				if (next.getID() == anteVertice.getID())
					k++;
		}
		if (k != 1 && !(l > 1))
		{
			mErrors.add("bad number of pointing back nexts");
			return false;
		}
		//        if (nexts.length>2) {
		//            mErrors.add("you must only have 2 nexts");
		//            return false;
		//        }
		return err;
	}
	private boolean isInPrev(Vertex vertex)
	{
		int id = vertex.getID();
		Vertex[] anteVertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, false);
		for (Vertex anteVertice : anteVertices) {
			if (anteVertice.getID() == id)
			{
				return true;
			}
		}
		return false;
	}
	@Override
	public boolean isLoop()
	{
		return true;
	}
}
