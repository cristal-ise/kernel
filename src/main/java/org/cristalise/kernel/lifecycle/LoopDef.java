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
import org.cristalise.kernel.lifecycle.instance.Loop;
import org.cristalise.kernel.lifecycle.instance.WfVertex;

/**
 * @version $Revision: 1.19 $ $Date: 2005/09/29 10:18:31 $
 * @author  $Author: abranson $
 */

public class LoopDef extends XOrSplitDef
{
	public boolean hasLoop = false;
	public int isNext = 0;

	/**
	 * @see java.lang.Object#Object()
	 */
	public LoopDef()
	{
		super();
	}

	/**
	 * @see org.cristalise.kernel.lifecycle.WfVertexDef#loop()
	 */
	@Override
	public boolean loop()
	{
		return true;
	}

	/**
	 * @see org.cristalise.kernel.lifecycle.WfVertexDef#verify()
	 */
	@Override
	public boolean verify()
	{
		if (!super.verify()) return false;
		Vertex[] nexts = getOutGraphables();
		Vertex[] anteVertices =
			GraphTraversal.getTraversal(this.getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, false);
        int k = 0;
        int l = 0;
        Vertex[] brothers = getParent().getChildren();
        for (Vertex brother : brothers)
			if (brother instanceof LoopDef) l++;
        for (Vertex next : nexts)
			for (Vertex anteVertice : anteVertices)
				if (next.equals(anteVertice))
					k++;
		if (k != 1 && !(l>1))
		{
            mErrors.add("bad number of pointing back nexts");
			return false;
		}
//        if (nexts.length>2) {
//            mErrors.add("you must only have 2 nexts");
//            return false;
//        }
		return true;
	}

    @Override
	public boolean isLoop() {
        return true;
    }

	@Override
	public WfVertex instantiate() throws InvalidDataException, ObjectNotFoundException {
		Loop newLoop = new Loop();
		configureInstance(newLoop);
		return newLoop;
	}

}
