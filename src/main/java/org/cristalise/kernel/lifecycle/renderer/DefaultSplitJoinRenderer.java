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
package org.cristalise.kernel.lifecycle.renderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.renderer.DefaultVertexRenderer;
import org.cristalise.kernel.lifecycle.AndSplitDef;
import org.cristalise.kernel.lifecycle.JoinDef;
import org.cristalise.kernel.lifecycle.LoopDef;
import org.cristalise.kernel.lifecycle.OrSplitDef;
import org.cristalise.kernel.lifecycle.XOrSplitDef;
import org.cristalise.kernel.lifecycle.instance.AndSplit;
import org.cristalise.kernel.lifecycle.instance.Join;
import org.cristalise.kernel.lifecycle.instance.Loop;
import org.cristalise.kernel.lifecycle.instance.OrSplit;
import org.cristalise.kernel.lifecycle.instance.XOrSplit;

public class DefaultSplitJoinRenderer extends DefaultVertexRenderer {

    public static final Paint BOX_PAINT= new Color(204, 204, 204);

    private static final String AND_LABEL   = "And";
    private static final String OR_LABEL    = "Or";
    private static final String XOR_LABEL   = "XOr";
    private static final String JOIN_LABEL  = "Join";
    private static final String LOOP_LABEL  = "Loop";
    private static final String ROUTE_LABEL = "";
    private static final String XXX_LABEL   = "XXX";

    /**
     * Join/Split specific implementation of draw method
     *
     * @param g2d the canvas
     * @param vertex the vertex to be drawn
     * @param errors the errors to be shown, can be bull
     */
    public void draw(Graphics2D g2d, Vertex vertex, String errors) {
        drawOutlineRect(g2d, vertex, StringUtils.isNotBlank(errors) ? ERROR_PAINT : BOX_PAINT, TEXT_PAINT);

        ArrayList<String>linesOfText = new ArrayList<String>();

        linesOfText.add(getLabel(vertex));

        if (StringUtils.isNotBlank(errors)) linesOfText.add(errors);

        drawLinesOfTexts(g2d, vertex, linesOfText, TEXT_PAINT);
    }

    /**
     * Set the label of the Join/Join
     *
     * @param vertex the vertex to be drawn
     * @return the label for the given type
     */
    private String getLabel(Vertex vertex) {
        // ORDER IS IMPORTANT because of the inheritance between classes (e.g. AndSplitDef is superclass of OrSplitDef)
        if (vertex instanceof Loop || vertex instanceof LoopDef) {
            return LOOP_LABEL;
        }
        else if (vertex instanceof XOrSplit || vertex instanceof XOrSplitDef) {
            return XOR_LABEL;
        }
        else if (vertex instanceof OrSplit || vertex instanceof OrSplitDef) {
            return OR_LABEL;
        }
        else if (vertex instanceof AndSplit || vertex instanceof AndSplitDef) {
            return AND_LABEL;
        }
        else if (vertex instanceof Join || vertex instanceof JoinDef) {
            String type = (String) ((GraphableVertex) vertex).getBuiltInProperty(BuiltInVertexProperties.TYPE);

            if (StringUtils.isNotBlank(type) && type.equals("Route")) {
                return ROUTE_LABEL;
            }
            else {
                return JOIN_LABEL;
            }
        }

        return XXX_LABEL;
    }
}
