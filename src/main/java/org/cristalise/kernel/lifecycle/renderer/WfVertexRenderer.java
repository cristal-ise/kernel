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

import java.awt.Graphics2D;

import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.renderer.VertexRenderer;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.Join;
import org.cristalise.kernel.lifecycle.instance.Split;

public class WfVertexRenderer implements VertexRenderer {
    protected ActivityRenderer  mActivityRenderer  = new ActivityRenderer();
    protected SplitJoinRenderer mSplitJoinRenderer = new SplitJoinRenderer();

    @Override
    public void draw(Graphics2D g2d, Vertex vertex) {
        if (vertex instanceof Activity) {
            mActivityRenderer.draw(g2d, vertex);
        }
        else if ((vertex instanceof Split) || (vertex instanceof Join)) {
            mSplitJoinRenderer.draw(g2d, vertex);
        }
    }
}
