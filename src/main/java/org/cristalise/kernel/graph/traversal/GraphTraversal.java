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
package org.cristalise.kernel.graph.traversal;


import java.util.Vector;

import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.graph.model.Vertex;



public class GraphTraversal
{
    public static final int kUp   = 1;
    public static final int kDown = 2;


    private GraphTraversal()
    {
    }


    public static Vertex[] getTraversal(GraphModel graphModel, Vertex startVertex, int direction, boolean ignoreBackLinks)
    {
        Vector<Vertex> path = new Vector<Vertex>(10, 10);

        graphModel.clearTags(startVertex);
        visitVertex(startVertex, graphModel, path, direction, startVertex, ignoreBackLinks);

        return vectorToVertexArray(path);
    }


    private static void visitVertex(Vertex vertex, GraphModel graphModel, Vector<Vertex> path, int direction, Object tag, boolean ignoreBackLinks)
    {
        Vertex[] children = null;
        int i = 0;

        if(direction == kDown)
        {
            children = graphModel.getOutVertices(vertex);
        }
        else
        {
            children = graphModel.getInVertices(vertex);
        }

        vertex.setTag(tag);
        path.add(vertex);

        for(i=0; i<children.length; i++)
        {
            if(!(children[i].hasTag(tag)))
            {
                boolean skipBackLink = false;
                if ( ignoreBackLinks &&
                    ((vertex.isJoin() && direction == kUp) ||
                    (vertex.isLoop() && direction == kDown))) {
                    Vertex[] following = getTraversal(graphModel, children[i], direction, false);
                    for (Vertex element : following) {
                        if (element == vertex) {
                            skipBackLink = true;
                            break;
                        }
                    }
                }
                if (!skipBackLink)
                    visitVertex(children[i], graphModel, path, direction, tag, ignoreBackLinks);
            }
        }
    }


    private static Vertex[] vectorToVertexArray(Vector<Vertex> vector)
    {
        Vertex[] vertices = new Vertex[vector.size()];
        int      i        = 0;


        for(i=0; i<vertices.length; i++)
        {
            vertices[i] = vector.elementAt(i);
        }

        return vertices;
    }
}
