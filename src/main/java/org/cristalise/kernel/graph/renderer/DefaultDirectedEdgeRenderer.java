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
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.utils.Logger;

/**
 * Default edge renderer implementing the DirectedEdgeRenderer. Use it as is or as a base class
 */
public class DefaultDirectedEdgeRenderer implements DirectedEdgeRenderer {

    public static final Paint LINE_PAINT  = Color.BLACK;
    public static final Paint TEXT_PAINT  = Color.BLACK;
    public static final Paint ERROR_PAINT = Color.RED; //new Color(255, 50, 0)

    /**
     * The different routing calculations to draw the edge
     */
    public enum EdgeRouting {
        /**
         * Draw a straight line between origin and terminus point
         */
        STRAIGHT("Straight"),

        /**
         * Draw a line of 3 segments (vertical, horizontal, vertical) between origin and terminus point
         */
        BROKEN_PLUS("Broken +"),

        /**
         * Draw a line of 2 segments (horizontal, vertical) between origin and terminus point
         */
        BROKEN_MINUS("Broken -"),

        /**
         * Draw a line of 2 segments (vertical, horizontal) between origin and terminus point
         */
        BROKEN_PIPE("Broken |");

        private String routingName;

        private EdgeRouting(final String n) {
            routingName = n;
        }

        public String getName() {
            return routingName;
        }

        @Override
        public String toString() {
            return getName();
        }

        /**
         * Create EdgeRouting value from the given string matching it with the names
         *
         * @param name the name to be converted
         * @return the value or null of there was no match
         */
        public static EdgeRouting getValue(String name) {
            if (StringUtils.isBlank(name)) return STRAIGHT;

            for (EdgeRouting routing : EdgeRouting.values()) {
                if(routing.getName().equals(name) || routing.name().equals(name)) return routing;
            }
            return null;
        }
    }

    /**
     * The template of the arrow
     */
    private GeneralPath mArrowTemplate = new GeneralPath();

    /**
     * Setup arrow template
     */
    public DefaultDirectedEdgeRenderer() {
        mArrowTemplate.moveTo(-5, 5);
        mArrowTemplate.lineTo(0, 0);
        mArrowTemplate.lineTo(5, 5);
    }

    /**
     * Default implementation, can only draw straight line and does not add any label
     */
    @Override
    public void draw(Graphics2D g2d, DirectedEdge directedEdge) {
        g2d.setPaint(LINE_PAINT);
        drawStraightWithArrow(g2d, directedEdge);
    }

    /**
     * Draws an edge using different calculations, coloring and label
     *
     * @param g2d the canvas
     * @param directedEdge edge to be drawn
     * @param label the label to put on the line
     * @param type the type of the line
     * @param hasError if the edge is in an erroneous state
     */
    public void draw(Graphics2D g2d, DirectedEdge directedEdge, String label, String type, boolean hasError) {
        g2d.setPaint(hasError ? ERROR_PAINT : LINE_PAINT);

        switch (EdgeRouting.getValue(type)) {
            case STRAIGHT:     drawStraightWithArrow(g2d, directedEdge);    break;
            case BROKEN_PLUS:  drawBrokenPlusWithArrow(g2d, directedEdge);  break;
            case BROKEN_MINUS: drawBrokenMinusWithArrow(g2d, directedEdge); break;
            case BROKEN_PIPE:  drawBrokenPipeWithArrow(g2d, directedEdge);  break;

            default:
                Logger.warning("DefaultDirectedEdgeRenderer.draw() - unknow edge type:%s - drawing straight line", type);
                drawStraightWithArrow(g2d, directedEdge);
                break;
        }

        GraphPoint originPoint = directedEdge.getOriginPoint();
        GraphPoint terminusPoint = directedEdge.getTerminusPoint();
        GraphPoint midPoint = new GraphPoint();

        midPoint.x = (originPoint.x + terminusPoint.x) / 2;
        midPoint.y = (originPoint.y + terminusPoint.y) / 2;

        if (StringUtils.isNotBlank(label)) g2d.drawString(label, midPoint.x + 10, midPoint.y);
    }

    /**
     * Draw a line of 3 segments (vertical, horizontal, vertical) between origin and terminus point
     *
     * @param g2d the canvas
     * @param directedEdge edge to be drawn
     */
    private void drawBrokenPlusWithArrow(Graphics2D g2d, DirectedEdge directedEdge) {
        GraphPoint originPoint = directedEdge.getOriginPoint();
        GraphPoint terminusPoint = directedEdge.getTerminusPoint();

        g2d.drawLine(originPoint.x, originPoint.y, originPoint.x, (originPoint.y + terminusPoint.y) / 2);
        g2d.drawLine(originPoint.x, (originPoint.y + terminusPoint.y) / 2, terminusPoint.x, (originPoint.y + terminusPoint.y) / 2);
        g2d.drawLine(terminusPoint.x, (originPoint.y + terminusPoint.y) / 2, terminusPoint.x, terminusPoint.y);

        GraphPoint midPoint = new GraphPoint();

        midPoint.x = (originPoint.x + terminusPoint.x) / 2;
        midPoint.y = (originPoint.y + terminusPoint.y) / 2;

        //Draw the transformed arrow
        AffineTransform transform = new AffineTransform();

        transform.translate(midPoint.x, midPoint.y);
        transform.rotate( calcArrowAngle(
                originPoint.x,
                originPoint.x - terminusPoint.x > -5 && originPoint.x - terminusPoint.x < 5 ? originPoint.y   : (originPoint.y + terminusPoint.y) / 2,
                terminusPoint.x,
                originPoint.x - terminusPoint.x > -5 && originPoint.x - terminusPoint.x < 5 ? terminusPoint.y : (originPoint.y + terminusPoint.y) / 2)
        );

        g2d.draw(mArrowTemplate.createTransformedShape(transform));
    }

    /**
     * Draw a line of 2 segments (horizontal, vertical) between origin and terminus point
     *
     * @param g2d the canvas
     * @param directedEdge edge to be drawn
     */
    private void drawBrokenMinusWithArrow(Graphics2D g2d, DirectedEdge directedEdge) {
        GraphPoint originPoint = directedEdge.getOriginPoint();
        GraphPoint terminusPoint = directedEdge.getTerminusPoint();

        g2d.drawLine(originPoint.x, originPoint.y, terminusPoint.x, originPoint.y);
        g2d.drawLine(terminusPoint.x, originPoint.y, terminusPoint.x, terminusPoint.y);

        boolean arrowOnY = !(originPoint.y - terminusPoint.y < 60 && originPoint.y - terminusPoint.y > -60);

        GraphPoint midPoint = new GraphPoint();

        midPoint.x = arrowOnY ? terminusPoint.x                       : (originPoint.x + terminusPoint.x) / 2;
        midPoint.y = arrowOnY ? (originPoint.y + terminusPoint.y) / 2 : originPoint.y;

        //Draw the transformed arrow
        AffineTransform transform = new AffineTransform();

        transform.translate(midPoint.x, midPoint.y);
        transform.rotate( calcArrowAngle(
                arrowOnY ? terminusPoint.x : originPoint.x,
                arrowOnY ? originPoint.y   : originPoint.y,
                arrowOnY ? terminusPoint.x : terminusPoint.x,
                arrowOnY ? terminusPoint.y : originPoint.y)
        );

        g2d.draw(mArrowTemplate.createTransformedShape(transform));
    }

    /**
     * Draw a line of 2 segments (vertical, horizontal) between origin and terminus point
     *
     * @param g2d the canvas
     * @param directedEdge edge to be drawn
     */
    private void drawBrokenPipeWithArrow(Graphics2D g2d, DirectedEdge directedEdge) {
        GraphPoint originPoint = directedEdge.getOriginPoint();
        GraphPoint terminusPoint = directedEdge.getTerminusPoint();

        g2d.drawLine(originPoint.x, originPoint.y, originPoint.x, terminusPoint.y);
        g2d.drawLine(originPoint.x, terminusPoint.y, terminusPoint.x, terminusPoint.y);

        boolean arrowOnY = !(originPoint.y - terminusPoint.y < 60 && originPoint.y - terminusPoint.y > -60);

        GraphPoint midPoint = new GraphPoint();

        midPoint.x = arrowOnY ? originPoint.x : (originPoint.x + terminusPoint.x) / 2;
        midPoint.y = arrowOnY ? (originPoint.y + terminusPoint.y) / 2 : terminusPoint.y;

        //Draw the transformed arrow
        AffineTransform transform = new AffineTransform();

        transform.translate(midPoint.x, midPoint.y);
        transform.rotate(
            calcArrowAngle(
                arrowOnY ? terminusPoint.x : originPoint.x,
                arrowOnY ? originPoint.y   : originPoint.y,
                arrowOnY ? terminusPoint.x : terminusPoint.x,
                arrowOnY ? terminusPoint.y : originPoint.y)
        );

        g2d.draw(mArrowTemplate.createTransformedShape(transform));
    }

    /**
     * Draw a straight line between origin and terminus point
     *
     * @param g2d the canvas
     * @param directedEdge edge to be drawn
     */
    private void drawStraightWithArrow(Graphics2D g2d, DirectedEdge directedEdge) {
        GraphPoint originPoint = directedEdge.getOriginPoint();
        GraphPoint terminusPoint = directedEdge.getTerminusPoint();

        g2d.drawLine(originPoint.x, originPoint.y, terminusPoint.x, terminusPoint.y);

        GraphPoint midPoint = new GraphPoint();

        midPoint.x = originPoint.x + (terminusPoint.x - originPoint.x) / 2;
        midPoint.y = originPoint.y + (terminusPoint.y - originPoint.y) / 2;

        //Draw the transformed arrow
        AffineTransform transform = new AffineTransform();

        transform.translate(midPoint.x, midPoint.y);
        transform.rotate(calcArrowAngle(originPoint.x, originPoint.y, terminusPoint.x, terminusPoint.y));

        g2d.draw(mArrowTemplate.createTransformedShape(transform));
    }

    /**
     * Calculates the angle of the arrow which is in the direction of the line
     *
     * @param originX x coordinate of the origin point
     * @param originY y coordinate of the origin point
     * @param terminusX x coordinate of the termination point
     * @param terminusY y coordinate of the termination point
     * @return the computed angle
     */
    private static double calcArrowAngle(int originX, int originY, int terminusX, int terminusY) {
        double width  = terminusX - originX;
        double height = terminusY - originY;

        if ((width == 0) && (height >  0)) { return Math.PI; }
        if ((width == 0) && (height <  0)) { return 0; }
        if ((width >  0) && (height == 0)) { return Math.PI / 2.0; }
        if ((width <  0) && (height == 0)) { return -1.0 * Math.PI / 2.0; }
        if ((width >  0) && (height >  0)) { return Math.PI / 2.0 + Math.atan(Math.abs(height) / Math.abs(width)); }
        if ((width >  0) && (height <  0)) { return Math.atan(Math.abs(width) / Math.abs(height)); }
        if ((width <  0) && (height <  0)) { return -1.0 * Math.atan(Math.abs(width) / Math.abs(height)); }
        if ((width <  0) && (height >  0)) { return -1.0 * (Math.PI / 2.0 + Math.atan(Math.abs(height) / Math.abs(width))); }
        return 0.0;
    }
}
