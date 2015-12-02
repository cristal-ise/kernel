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
package org.cristalise.kernel.graph.model;

import java.awt.Polygon;
import java.util.Vector;


public class Vertex
{
    private int          mId              = -1;
    private String       mName            = "";
    private GraphPoint   mCentrePoint     = new GraphPoint(0, 0);
    private int          mHeight          = 0;
    private int          mWidth           = 0;
    private Vector<Integer>       mInEdgeIdVector  = new Vector<Integer>();
    private Vector<Integer>       mOutEdgeIdVector = new Vector<Integer>();
    private final Vector<Object>       mTags             = new Vector<Object>();

    // The Java Polygon class is used to determine if a point
    // lies within the outline of a vertex.  Unfortunately
    // both the polygon and the set of outline points need to
    // kept in memory because a polygon never has 0 points
    // which is required by Castor's unmarshall object mechanism
    private Polygon      mOutlinePolygon  = new Polygon();
    private GraphPoint[] mOutlinePoints   = new GraphPoint[0];


	private GraphModel graphModel;

    public void setID(int id)
    {
        mId = id;
    }


    public int getID()
    {
        return mId;
    }


    public void setName(String n)
    {
        mName = n;
    }


    public String getName()
    {
        return mName;
    }


    public void setCentrePoint(GraphPoint p)
    {
        mCentrePoint = p;
    }


    public GraphPoint getCentrePoint()
    {
        return mCentrePoint;
    }


    public void setHeight(int h)
    {
        mHeight = h;
    }


    public int getHeight()
    {
        return mHeight;
    }


    public void setWidth(int w)
    {
        mWidth = w;
    }


    public int getWidth()
    {
        return mWidth;
    }


    // Sets the outline points and re-calculates the
    // height and width
    public void setOutlinePoints(GraphPoint[] outline)
    {
        int topLeftX     = outline[0].x;
        int topLeftY     = outline[0].y;
        int bottomRightX = 0;
        int bottomRightY = 0;
        int i            = 0;

        mOutlinePoints = outline;

        // Construct a polygon in the outline of the vertex
        // and calculate the top left and bottom right corners
        mOutlinePolygon = new Polygon();

        for(i=0; i<outline.length; i++)
        {
            mOutlinePolygon.addPoint(outline[i].x, outline[i].y);

            if(outline[i].x < topLeftX)
            {
                topLeftX = outline[i].x;
            }

            if(outline[i].y < topLeftY)
            {
                 topLeftY = outline[i].y;
            }

            if(outline[i].x > bottomRightX)
            {
                bottomRightX = outline[i].x;
            }


            if(outline[i].y > bottomRightY)
            {
                bottomRightY = outline[i].y;
            }
        }

        // Set the height and width
        mHeight = bottomRightY - topLeftY;
        mWidth  = bottomRightX - topLeftX;
    }


    public GraphPoint[] getOutlinePoints()
    {
        return mOutlinePoints;
    }

    public void moveAbsolute(GraphPoint p)
    {
        int deltaX = p.x - mCentrePoint.x;
        int deltaY = p.y - mCentrePoint.y;
        int i      = 0;

        // Update the outline points and the polygon
        for(i=0; i<mOutlinePoints.length; i++)
        {
            mOutlinePoints[i].x += deltaX;
            mOutlinePoints[i].y += deltaY;
        }

        mOutlinePolygon.translate(deltaX, deltaY);

        mCentrePoint.x = p.x;
        mCentrePoint.y = p.y;
    }


    public boolean containsPoint(GraphPoint p)
    {
        return mOutlinePolygon.contains(p.x, p.y);
    }


    public void setInEdgeIds(int[] ids)
    {
        int i = 0;

        mInEdgeIdVector = new Vector<Integer>(10, 10);
       	for(i=0; i<ids.length; i++)
       		mInEdgeIdVector.add(Integer.valueOf(ids[i]));
    }


    public int[] getInEdgeIds()
    {
        return integerVectorToIntArray(mInEdgeIdVector);
    }


    public void setOutEdgeIds(int[] ids)
    {
        int i = 0;

        mOutEdgeIdVector = new Vector<Integer>(10, 10);
       	for(i=0; i<ids.length; i++)
       		mOutEdgeIdVector.add(Integer.valueOf(ids[i]));
    }


    public int[] getOutEdgeIds()
    {
        return integerVectorToIntArray(mOutEdgeIdVector);
    }


    private static int[] integerVectorToIntArray(Vector<Integer> vector)
    {
        int[]   array   = new int[vector.size()];
        Integer integer = null;
        int     i       = 0;

        for(i=0; i<array.length; i++)
        {
            integer  = vector.elementAt(i);
            array[i] = integer.intValue();
        }

        return array;
    }


    public void addInEdgeId(int id)
    {
        mInEdgeIdVector.add(Integer.valueOf(id));
    }


    public void removeInEdgeId(int id)
    {
        Integer integer = null;
        int     i       = 0;

        for(i=0; i<mInEdgeIdVector.size(); i++)
        {
            integer = mInEdgeIdVector.elementAt(i);

            if(integer.intValue() == id)
            {
                mInEdgeIdVector.removeElementAt(i);
                return;
            }
        }
    }


    public void addOutEdgeId(int id)
    {
        mOutEdgeIdVector.add(Integer.valueOf(id));
    }


    public void removeOutEdgeId(int id)
    {
        Integer integer = null;
        int     i       = 0;

        for(i=0; i<mOutEdgeIdVector.size(); i++)
        {
            integer = mOutEdgeIdVector.elementAt(i);

            if(integer.intValue() == id)
            {
                mOutEdgeIdVector.removeElementAt(i);
                return;
            }
        }
    }


    public void setTag(Object o)
    {
        mTags.add(o);
    }


    public boolean hasTag(Object o)
    {
        return mTags.contains(o);
    }

    public void clearTag(Object o)
    {
        mTags.remove(o);
    }


    public GraphModel getChildGraphModel() {
        return null;
    }

    public Object getCreationContext() {
        return null;
    }


	public GraphModel getGraphModel()
	{
		return graphModel;
	}

	public void setGraphModel(GraphModel graphModel)
	{
		this.graphModel = graphModel;
	}

    public boolean isJoin() {
        return false;
    }
    public boolean isLoop() {
        return false;
    }

}
