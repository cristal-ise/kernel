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

import static org.cristalise.kernel.graph.model.BuiltInEdgeProperties.ALIAS;
import static org.cristalise.kernel.graph.model.BuiltInEdgeProperties.TYPE;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.LAST_NUM;

import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.GraphableEdge;
import org.cristalise.kernel.lifecycle.instance.Next;

/**
 */
public class NextDef extends GraphableEdge
{
	/**
	 * @see java.lang.Object#Object()
	 */
	public NextDef()
	{
	}
	/**
	 * Method verify.
	 *
	 * @return boolean
	 */
	public boolean verify()
	{
		return true;
	}
	/**
	 * Method NextDef.
	 *
	 * @param pre
	 * @param nex
	 */
	/** create and initialize a link between an Activities */
	public NextDef(WfVertexDef pre, WfVertexDef nex)
	{
		setParent(pre.getParent());
		if (pre instanceof OrSplitDef || pre instanceof XOrSplitDef)
		{
			int num = pre.getOutGraphables().length;
			try {
				num = Integer.parseInt((String) pre.getBuiltInProperty(LAST_NUM));
			}
			catch (Exception e) {
			}

			setBuiltInProperty(ALIAS, String.valueOf(num));
			pre.setBuiltInProperty(LAST_NUM, String.valueOf(num + 1));
		}
	}
	@Override
	public boolean containsPoint(GraphPoint p)
	{
		GraphPoint originPoint = getOriginPoint();
		GraphPoint terminusPoint = getTerminusPoint();
		GraphPoint midPoint = new GraphPoint();
		if (("Broken +".equals(getBuiltInProperty(TYPE))))
		{
			midPoint.x = (originPoint.x + terminusPoint.x) / 2;
			midPoint.y = (originPoint.y + terminusPoint.y) / 2;
		}
		else if (("Broken -".equals(getBuiltInProperty(TYPE))))
		{
			boolean arrowOnY = !(originPoint.y - terminusPoint.y < 60 && originPoint.y - terminusPoint.y > -60);
			midPoint.x = arrowOnY ? terminusPoint.x : (originPoint.x + terminusPoint.x) / 2;
			midPoint.y = arrowOnY ? (originPoint.y + terminusPoint.y) / 2 : originPoint.y;
		}
		else if (("Broken |".equals(getBuiltInProperty(TYPE))))
		{
			boolean arrowOnY = !(originPoint.y - terminusPoint.y < 60 && originPoint.y - terminusPoint.y > -60);
			midPoint.x = arrowOnY ? originPoint.x : (originPoint.x + terminusPoint.x) / 2;
			midPoint.y = arrowOnY ? (originPoint.y + terminusPoint.y) / 2 : terminusPoint.y;
		}
		else
		{
			midPoint.x = originPoint.x + (terminusPoint.x - originPoint.x) / 2;
			midPoint.y = originPoint.y + (terminusPoint.y - originPoint.y) / 2;
		}
		int minX = midPoint.x - 10;
		int minY = midPoint.y - 10;
		int maxX = midPoint.x + 10;
		int maxY = midPoint.y + 10;
		return (p.x >= minX) && (p.x <= maxX) && (p.y >= minY) && (p.y <= maxY);
	}

	public Next instantiate() {
		Next next = new Next();
		next.setID(getID());
		next.setOriginVertexId(getOriginVertexId());
		next.setTerminusVertexId(getTerminusVertexId());
		next.setProperties(getProperties());
		next.setOriginPoint(getOriginPoint());
		next.setTerminusPoint(getTerminusPoint());
		return next;
	}
}
