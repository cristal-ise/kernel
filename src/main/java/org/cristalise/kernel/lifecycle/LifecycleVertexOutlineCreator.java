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

import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.model.VertexOutlineCreator;
import org.cristalise.kernel.lifecycle.instance.Activity;

public class LifecycleVertexOutlineCreator implements VertexOutlineCreator {

    private static final int mActivityWidth   = 130;
    private static final int mActivityHeight  = 60;
    private static final int mSplitJoinWidth  = 60;
    private static final int mSplitJoinHeight = 25;

    @Override
    public void setOutline(Vertex vertex) {
        GraphPoint   centrePoint   = vertex.getCentrePoint();
        GraphPoint[] outlinePoints = new GraphPoint[ 4 ];
        int          vertexWidth   = 0;
        int          vertexHeight  = 0;

        if (vertex instanceof Activity ||  vertex instanceof ActivitySlotDef  || vertex instanceof ActivityDef) {
            vertexWidth  = mActivityWidth;
            vertexHeight = mActivityHeight;
        }
        else {
            vertexWidth  = mSplitJoinWidth;
            vertexHeight = mSplitJoinHeight;
        }

        outlinePoints[ 0 ]   = new GraphPoint(centrePoint.x - vertexWidth / 2, centrePoint.y - vertexHeight / 2);
        outlinePoints[ 1 ]   = new GraphPoint(centrePoint.x + vertexWidth / 2, centrePoint.y - vertexHeight / 2);
        outlinePoints[ 2 ]   = new GraphPoint(centrePoint.x + vertexWidth / 2, centrePoint.y + vertexHeight / 2);
        outlinePoints[ 3 ]   = new GraphPoint(centrePoint.x - vertexWidth / 2, centrePoint.y + vertexHeight / 2);

        vertex.setOutlinePoints( outlinePoints );
    }
}

