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

import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m") @Getter @Setter
public abstract class DirectedEdge {

    private int        mID               = -1;
    private GraphPoint mOriginPoint      = new GraphPoint(0, 0);
    private GraphPoint mTerminusPoint    = new GraphPoint(0, 0);
    private int        mOriginVertexId   = -1;
    private int        mTerminusVertexId = -1;

    /**
     * Alternative way to store the points of the Edge. If it is not empty mOriginPoint and mTerminusPoint can be omitted.
     */
    private HashMap<Integer, GraphPoint> mMultiPoints = new HashMap<>();

    /**
     * Used by the GUI only, and does not work with MultiPoints. Checks if the given point is 'close' to the Edge
     *
     * @param p the point to check
     * @return true if the point is 'close' to the Edge
     */
    public boolean containsPoint(GraphPoint p) {
        int midX = mOriginPoint.x + (mTerminusPoint.x - mOriginPoint.x)/2;
        int midY = mOriginPoint.y + (mTerminusPoint.y - mOriginPoint.y)/2;
        int minX = midX - 10;
        int minY = midY - 10;
        int maxX = midX + 10;
        int maxY = midY + 10;

        return (p.x >= minX) && (p.x <= maxX) && (p.y >= minY) && (p.y <= maxY);
    }

    public void   setName(String name) {}
    public String getName() { return null; }
}
