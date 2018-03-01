/**
 * This file is part of the CRISTAL-iSE default user interface.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.graph.renderer;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.util.ArrayList;

import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;

/**
 * Default vertex renderer implementing VertexRenderer. Use it as is or as a base class
 */
public class DefaultVertexRenderer implements VertexRenderer {
    public static final Paint FILL_PAINT  = Color.WHITE;
    public static final Paint LINE_PAINT  = Color.BLACK;
    public static final Paint TEXT_PAINT  = Color.BLACK;
    public static final Paint ERROR_PAINT = Color.RED; //new Color(255, 50, 0)

    /**
     * Default implementation draws the outline and the name of the vertex
     */
    @Override
    public void draw(Graphics2D g2d, Vertex vertex) {
        drawOutline(g2d, vertex, FILL_PAINT, LINE_PAINT);
        drawName(g2d, vertex, TEXT_PAINT);
    }

    /**
     * Fill and then draw the outline as a Polygon
     *
     * @param g2d the canvas
     * @param vertex the vertex to be drawn
     */
    public void drawOutline(Graphics2D g2d, Vertex vertex, Paint fillPaint, Paint linePaint) {
        Polygon outline = vertex.getOutlinePolygon();

        g2d.setPaint(fillPaint);
        g2d.fill(outline);

        g2d.setPaint(linePaint);
        g2d.draw(outline);
    }

    /**
     * Fills the outline as 3D rectangle, but it does not draw the border of the outline
     *
     * @param g2d the canvas
     * @param vertex the vertex to be drawn
     * @param fillPaint the fill color
     */
    public void drawOutline3DRect(Graphics2D g2d, Vertex vertex, Paint fillPaint) {
        drawOutline3DRect(g2d, vertex, fillPaint, null);
    }

    /**
     * Fills the outline as 3D rectangle. It draws the border of the outline if linePaint is not null
     *
     * @param g2d the canvas
     * @param vertex the vertex to be drawn
     * @param fillPaint the fill color
     * @param linePaint the color of the line, can be null
     */
    public void drawOutline3DRect(Graphics2D g2d, Vertex vertex, Paint fillPaint, Paint linePaint) {
        GraphPoint centrePoint = vertex.getCentrePoint();

        g2d.setPaint(fillPaint);
        g2d.fill3DRect(
                centrePoint.x - vertex.getWidth() / 2,
                centrePoint.y - vertex.getHeight() / 2,
                vertex.getWidth(),
                vertex.getHeight(),
                true);

        if (linePaint != null) {
            g2d.setPaint(linePaint);
            g2d.draw3DRect(
                    centrePoint.x - vertex.getWidth() / 2,
                    centrePoint.y - vertex.getHeight() / 2,
                    vertex.getWidth(),
                    vertex.getHeight(),
                    true);
        }
    }

    /**
     * Fills the outline as rectangle, but it does not draw the border of the outline
     *
     * @param g2d the canvas
     * @param vertex the vertex to be drawn
     * @param fillPaint the fill color
     */
    public void drawOutlineRect(Graphics2D g2d, Vertex vertex, Paint fillPaint) {
        drawOutline3DRect(g2d, vertex, fillPaint, null);
    }

    /**
     * Fills the outline as rectangle. It draws the border of the outline if linePaint is not null
     *
     * @param g2d the canvas
     * @param vertex the vertex to be drawn
     * @param fillPaint the fill color
     * @param linePaint the color of the line, can be null
     */
    public void drawOutlineRect(Graphics2D g2d, Vertex vertex, Paint fillPaint, Paint linePaint) {
        GraphPoint centrePoint = vertex.getCentrePoint();

        g2d.setPaint(fillPaint);
        g2d.fillRect(
                centrePoint.x - vertex.getWidth() / 2,
                centrePoint.y - vertex.getHeight() / 2,
                vertex.getWidth(),
                vertex.getHeight());

        if (linePaint != null) {
            g2d.setPaint(linePaint);
            g2d.drawRect(
                    centrePoint.x - vertex.getWidth() / 2,
                    centrePoint.y - vertex.getHeight() / 2,
                    vertex.getWidth(),
                    vertex.getHeight());
        }
    }

    /**
     * Draw the text within the boundaries of the vertex
     *
     * @param g2d the canvas
     * @param vertex the vertex in which the text is drawn
     * @param text the text to draw
     * @param textPaint the color of the text
     */
    public void drawText(Graphics2D g2d, Vertex vertex, String text, Paint textPaint) {
        GraphPoint centrePoint = vertex.getCentrePoint();
        FontMetrics metrics = g2d.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();
        int textX = centrePoint.x - textWidth / 2;
        int textY = centrePoint.y + textHeight / 3;

        g2d.setPaint(textPaint);
        g2d.drawString(text, textX, textY);
    }

    /**
     * Write the name of the vertex in the centre of it
     *
     * @param g2d the canvas
     * @param vertex the vertex in which the name is drawn
     */
    public void drawName(Graphics2D g2d, Vertex vertex, Paint textPaint) {
        drawText(g2d, vertex, vertex.getName(), textPaint);
    }

    /**
     * Draw lines of text within the boundaries of the vertex
     *
     * @param g2d the canvas
     * @param vertex the vertex for which the text to be drawn
     * @param linesOfText lines of the text
     * @param textPaint the color of the text
     */
    public void drawLinesOfTexts(Graphics2D g2d, Vertex vertex, ArrayList<String> linesOfText, Paint textPaint) {
        g2d.setPaint(textPaint);

        GraphPoint centrePoint = vertex.getCentrePoint();
        FontMetrics metrics = g2d.getFontMetrics();
        int lineHeight  = metrics.getHeight();
        int linesHeight = lineHeight * linesOfText.size();
        int linesStartY = centrePoint.y - linesHeight / 2 + lineHeight * 2 / 3;

        int i = 0;
        for (String line : linesOfText) {
            if (line == null) line = "";
            int lineWidth = metrics.stringWidth(line);
            int x = centrePoint.x - lineWidth / 2;
            int y = linesStartY + i++ * lineHeight;
            g2d.drawString(line, x, y);
        }
    }
}
