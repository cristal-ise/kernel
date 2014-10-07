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
package org.cristalise.kernel.graph.model;




public abstract class DirectedEdge 
{
    // Persistent data
    private int        mId               = -1;
    private GraphPoint mOriginPoint      = new GraphPoint(0, 0);
    private GraphPoint mTerminusPoint    = new GraphPoint(0, 0);
    private int        mOriginVertexId   = -1;
    private int        mTerminusVertexId = -1;


    public void setID(int id)
    {
        mId = id;
    }


    public int getID()
    {
        return mId;
    }


    public void setOriginPoint(GraphPoint p)
    {
        mOriginPoint = p;
    }


    public GraphPoint getOriginPoint()
    {
        return mOriginPoint;
    }


    public void setTerminusPoint(GraphPoint p)
    {
        mTerminusPoint = p;
    }


    public GraphPoint getTerminusPoint()
    {
        return mTerminusPoint;
    }


    public boolean containsPoint(GraphPoint p)
    {
        int midX = mOriginPoint.x + (mTerminusPoint.x - mOriginPoint.x)/2;
        int midY = mOriginPoint.y + (mTerminusPoint.y - mOriginPoint.y)/2;
        int minX = midX - 10;
        int minY = midY - 10;
        int maxX = midX + 10;
        int maxY = midY + 10;

        return (p.x >= minX) && (p.x <= maxX) && (p.y >= minY) && (p.y <= maxY);
    }


    public void setOriginVertexId(int id)
    {
        mOriginVertexId = id;
    }


    public int getOriginVertexId()
    {
        return mOriginVertexId;
    }


    public void setTerminusVertexId(int id)
    {
        mTerminusVertexId = id;
    }


    public int getTerminusVertexId()
    {
        return mTerminusVertexId;
    }


    public void setName(String name)
    {
    }


    public String getName()
    {
        return null;
    }
}
