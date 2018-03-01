/**
 * This file is part of the CRISTAL-iSE SVG Generator module.
 * Copyright (c) 2001-2018 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.lifecycle.renderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.renderer.DirectedEdgeRenderer;
import org.cristalise.kernel.graph.renderer.VertexRenderer;
import org.cristalise.kernel.utils.Logger;

/**
 *
 *
 */
public class LifecycleRenderer {

    GraphModel           mGraphModel;
    DirectedEdgeRenderer mDirectedEdgeRenderer;
    VertexRenderer       mVertexRenderer;

    public LifecycleRenderer(GraphModel graphModel, boolean defGeneration) {
        mGraphModel = graphModel;

        if (defGeneration) {
            mDirectedEdgeRenderer = new WfDirectedEdgeDefRenderer();
            mVertexRenderer = new WfVertexDefRenderer();
        }
        else {
            mDirectedEdgeRenderer = new WfDirectedEdgeRenderer();
            mVertexRenderer = new WfVertexRenderer();
        }
    }

    public int getZoomFactor(int maxHeight, int maxWidth) {
        int width = mGraphModel.getWidth();
        int height = mGraphModel.getHeight();

        return (maxHeight == 0 && maxWidth == 0) ?
                100 : Math.min(maxWidth * 100 / width, Math.min(maxHeight * 100 / height, 100));
    }

    public BufferedImage getWorkFlowModelImage(int maxHeight, int maxWidth) throws IOException {
        int width = mGraphModel.getWidth();
        int height = mGraphModel.getHeight();

        int zoomFactor = getZoomFactor(maxHeight, maxWidth);

        BufferedImage img = new BufferedImage(width * zoomFactor / 100, height * zoomFactor / 100, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = img.createGraphics();
        g2d.scale((double) zoomFactor / 100, (double) zoomFactor / 100);

        //set the background color transparent
        //g2d.setComposite(AlphaComposite.Clear);

        //set the 'canvas' white
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, width, height);

        draw(g2d);

        return img;
    }

    public void draw(Graphics2D g2d) {
        if (mGraphModel == null) {
            Logger.warning("LifecycleGenerator.draw() - GraphModel is NULL!");
            return;
        }

        DirectedEdge[] edges = mGraphModel.getEdges();
        Vertex[] vertices = mGraphModel.getVertices();

        // Draw the edges
        for (int i = 0; i < edges.length; i++) mDirectedEdgeRenderer.draw(g2d, edges[i]);

        // Draw the vertices
        for (int i = 0; i < vertices.length; i++) mVertexRenderer.draw(g2d, vertices[i]);

        g2d.setPaint(Color.green);

        // Highlight the start vertex if there is one
        Vertex startVertex = mGraphModel.getStartVertex();
        if (startVertex != null) drawVertexHighlight(g2d, startVertex, 1);
    }

    // Draws the highlight of the specified edge
    protected void drawEdgeHighlight(Graphics2D g2d, DirectedEdge edge) {
        GraphPoint originPoint = edge.getOriginPoint();
        GraphPoint terminusPoint = edge.getTerminusPoint();
        int midX = originPoint.x + (terminusPoint.x - originPoint.x) / 2;
        int midY = originPoint.y + (terminusPoint.y - originPoint.y) / 2;
        int minX = midX - 10;
        int minY = midY - 10;
        int maxX = midX + 10;
        int maxY = midY + 10;
        g2d.drawLine(minX, minY, maxX, minY);
        g2d.drawLine(maxX, minY, maxX, maxY);
        g2d.drawLine(maxX, maxY, minX, maxY);
        g2d.drawLine(minX, maxY, minX, minY);
    }

    // Draws the highlight of the specified vertex the specified dist from its outline
    protected void drawVertexHighlight(Graphics2D g2d, Vertex vertex, int dist) {
        GraphPoint[] outlinePoints = vertex.getOutlinePoints();
        GraphPoint centrePoint = vertex.getCentrePoint();

        /*
         * float dash1[] ={5.0f}; BasicStroke bs = new BasicStroke(5.0f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,10.0f, dash1,0.0f);
         */
        for (int i = 0; i < outlinePoints.length - 1; i++) {
            drawShiftedLine(
                    dist,
                    g2d,
                    centrePoint,
                    outlinePoints[i].x,
                    outlinePoints[i].y,
                    outlinePoints[i + 1].x,
                    outlinePoints[i + 1].y);
        }

        drawShiftedLine(
                dist,
                g2d,
                centrePoint,
                outlinePoints[outlinePoints.length - 1].x,
                outlinePoints[outlinePoints.length - 1].y,
                outlinePoints[0].x,
                outlinePoints[0].y);
    }

    // Draws the specifed line the specified distance away from the specified centre point
    private static void drawShiftedLine(int dist, Graphics2D g2d, GraphPoint centrePoint, int x1, int y1, int x2, int y2) {
        if (x1 > centrePoint.x) x1 += dist;
        if (x1 < centrePoint.x) x1 -= dist;
        if (y1 > centrePoint.y) y1 += dist;
        if (y1 < centrePoint.y) y1 -= dist;
        if (x2 > centrePoint.x) x2 += dist;
        if (x2 < centrePoint.x) x2 -= dist;
        if (y2 > centrePoint.y) y2 += dist;
        if (y2 < centrePoint.y) y2 -= dist;

        g2d.drawLine(x1, y1, x2, y2);
    }
}
