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
package org.cristalise.kernel.graph.layout;

import java.util.Vector;

import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.utils.Logger;


public class DefaultGraphLayoutGenerator {
    private static int mTopMargin = 100;
    private static int mLeftMargin = 100;
    private static int mHorzGap = 180;
    private static int mVertGap = 100;

    private DefaultGraphLayoutGenerator() {
    }

    public static void layoutGraph(GraphModel graphModel) {
        Vertex start = graphModel.getStartVertex();
        Vector<Vector<Vertex>> rowVector = new Vector<Vector<Vertex>>(10, 10);
        int[] midPoints = null;
        int valueOfLargestMidPoint = 0;
        if (start == null) {
            Logger.msg(1,"Error graph must have a starting vertex to be layed out");
            return;
        }
        graphModel.clearTags(start);
        visitVertex(graphModel, start, 0, rowVector, start);
        midPoints = new int[rowVector.size()];
        valueOfLargestMidPoint = calculateRowMidPoints(rowVector, midPoints, valueOfLargestMidPoint);
        fillInVertexLocations(graphModel, rowVector, valueOfLargestMidPoint, midPoints);
        fillInEdgeLocations(graphModel);
        graphModel.forceNotify();
    }

    private static void visitVertex(GraphModel graphModel, Vertex vertex, int rowIndex, Vector<Vector<Vertex>> rowVector, Object tag) {
        int i = 0;
        Vertex[] children = graphModel.getOutVertices(vertex);
        vertex.setTag(tag);
        addVertexToRow(vertex, rowIndex, rowVector);
        for (i = 0; i < children.length; i++) {
            if (!(children[i].hasTag(tag))) {
                visitVertex(graphModel, children[i], rowIndex + 1, rowVector, tag);
            }
        }
    }

    private static void addVertexToRow(Vertex vertex, int rowIndex, Vector<Vector<Vertex>> rowVector) {
        Vector<Vertex> rowsVertices = null;
        // If there is no vector of vertices already created for this row,
        // then create one
        if (rowVector.size() == rowIndex) {
            rowVector.add(new Vector<Vertex>(10, 10));
        }
        // Add the vertex to the row's vector of vertices
        rowsVertices = rowVector.elementAt(rowIndex);
        rowsVertices.add(vertex);
    }

    private static int calculateRowMidPoints(Vector<Vector<Vertex>> rowVector, int[] midPoints, int valueOfLargestMidPoint) {
        Vector<Vertex> rowsVertices = null;
        int newValueOfLargestMidPoint = valueOfLargestMidPoint;
        int rowsWidth = 0;
        int i = 0;
        for (i = 0; i < midPoints.length; i++) {
            rowsVertices = rowVector.elementAt(i);
            rowsWidth = mHorzGap * (rowsVertices.size() - 1);
            midPoints[i] = rowsWidth / 2;
            if (midPoints[i] > newValueOfLargestMidPoint) {
            	newValueOfLargestMidPoint = midPoints[i];
            }
        }
        return newValueOfLargestMidPoint;
    }

    private static void fillInVertexLocations(GraphModel graphModel, Vector<Vector<Vertex>> rowVector,
        int valueOfLargestMidPoint, int[] midPoints) {
            Vector<Vertex> rowsVertices = null;
            Vertex vertex = null;
            int rowIndex = 0;
            int column = 0;
            int rowsLeftMargin = 0;
            GraphPoint point = new GraphPoint(0, 0);
            for (rowIndex = 0; rowIndex < rowVector.size(); rowIndex++) {
                rowsVertices = rowVector.elementAt(rowIndex);
                rowsLeftMargin = mLeftMargin + valueOfLargestMidPoint - midPoints[rowIndex];
                for (column = 0; column < rowsVertices.size(); column++) {
                    vertex = rowsVertices.elementAt(column);
                    point.x = rowsLeftMargin + column * mHorzGap;
                    point.y = mTopMargin + rowIndex * mVertGap;
                    vertex.moveAbsolute(point);
                    graphModel.checkSize(vertex);
                }
            }
    }

    private static void fillInEdgeLocations(GraphModel graphModel) {
        Vertex[] vertices = graphModel.getVertices();
        GraphPoint centrePoint = null;
        DirectedEdge[] inEdges = null;
        DirectedEdge[] outEdges = null;
        int i = 0;
        int j = 0;
        for (i = 0; i < vertices.length; i++) {
            centrePoint = vertices[i].getCentrePoint();
            inEdges = graphModel.getInEdges(vertices[i]);
            outEdges = graphModel.getOutEdges(vertices[i]);
            for (j = 0; j < inEdges.length; j++) {
                inEdges[j].setTerminusPoint(centrePoint);
            }
            for (j = 0; j < outEdges.length; j++) {
                outEdges[j].setOriginPoint(centrePoint);
            }
        }
    }
}
