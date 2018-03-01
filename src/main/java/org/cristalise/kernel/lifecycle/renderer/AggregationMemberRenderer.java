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
package org.cristalise.kernel.lifecycle.renderer;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;

import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.AggregationMember;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.renderer.DefaultVertexRenderer;
import org.cristalise.kernel.utils.Logger;

/**
 *
 */
public class AggregationMemberRenderer extends DefaultVertexRenderer {

    private Aggregation mAggregation = null;

    public AggregationMemberRenderer() {}

    public void setAggregation(Aggregation agg) {
        mAggregation = agg;
    }

    @Override
    public void draw(Graphics2D g2d, Vertex vertex) {
        GraphPoint centre = vertex.getCentrePoint();
        GraphPoint[] outline = vertex.getOutlinePoints();
        FontMetrics metrics = g2d.getFontMetrics();

        AggregationMember memberPair = mAggregation.getMemberPair(vertex.getID());

        try {
            String name = memberPair.getItemName();

            g2d.drawString(name,
                    centre.x - metrics.stringWidth(name) / 2,
                    vertex.getID() % 2 == 0 ? topYOfOutline(outline) : bottomYOfOutline(outline) + metrics.getHeight());

            g2d.drawImage(
                    getImage(memberPair),
                    centre.x - 8,
                    centre.y - 8,
                    null);

            // Draw the outline of the vertex
            if (outline.length > 1) {
                for (int i = 0; i < outline.length - 1; i++) {
                    g2d.drawLine(
                            outline[i].x,
                            outline[i].y,
                            outline[i + 1].x,
                            outline[i + 1].y);
                }

                g2d.drawLine(
                        outline[outline.length - 1].x,
                        outline[outline.length - 1].y,
                        outline[0].x,
                        outline[0].y);
            }

        }
        catch (Exception ex) {
            Logger.error("AggregationMemberRenderer::draw() " + ex);
        }
    }

    int topYOfOutline(GraphPoint[] outline) {
        int topY = outline[0].y;

        for (int i = 1; i < outline.length; i++) {
            if (outline[i].y < topY) topY = outline[i].y;
        }

        return topY;
    }

    int bottomYOfOutline(GraphPoint[] outline) {
        int bottomY = outline[0].y;

        for (int i = 1; i < outline.length; i++) {
            if (outline[i].y > bottomY) bottomY = outline[i].y;
        }

        return bottomY;
    }

    public Image getImage(AggregationMember am) {
        return null;// ImageLoader.findImage("typeicons/"+am.getProperties().get("Type")+"_16.png").getImage();
    }
}
