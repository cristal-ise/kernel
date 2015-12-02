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
package org.cristalise.kernel.collection;

import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.model.VertexOutlineCreator;

/**
 * AggregationMember vertex outline creator. Sets up the new dimensions and
 * position on the GraphModel.
 *
 */
public class AggregationVertexOutlineCreator implements VertexOutlineCreator
{
    @Override
	public void setOutline(Vertex vertex)
    {
        GraphPoint centre = vertex.getCentrePoint();
        int height = vertex.getHeight();
        int width = vertex.getWidth();


        if (height==0 || width==0)
            vertex.setOutlinePoints
            (
                new GraphPoint[]
                {
                    new GraphPoint(centre.x-20, centre.y-20),
                    new GraphPoint(centre.x+20, centre.y-20),
                    new GraphPoint(centre.x+20, centre.y+20),
                    new GraphPoint(centre.x-20, centre.y+20)

                }
            );
        else

            vertex.setOutlinePoints
            (
                new GraphPoint[]
                {
                    new GraphPoint(centre.x-width/2, centre.y-height/2),
                    new GraphPoint(centre.x+width/2, centre.y-height/2),
                    new GraphPoint(centre.x+width/2, centre.y+height/2),
                    new GraphPoint(centre.x-width/2, centre.y+height/2)

                }
            );
    }
}
