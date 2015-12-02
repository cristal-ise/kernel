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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import java.util.Stack;

import org.cristalise.kernel.graph.event.EntireModelChangedEvent;
import org.cristalise.kernel.graph.event.ForcedNotifyEvent;
import org.cristalise.kernel.graph.event.GraphModelEvent;
import org.cristalise.kernel.utils.Logger;



public class GraphModelManager extends Observable
{

    private GraphModel                    mGraphModel;
    private EdgeFactory                   mEdgeFactory;
    private VertexFactory                 mVertexFactory;
    private VertexOutlineCreator          mVertexOutlineCreator;
    private final EntireModelChangedEvent mEntireModelChangedEvent = new EntireModelChangedEvent();
    private final ForcedNotifyEvent       mForcedNotifyEvent       = new ForcedNotifyEvent();
    private final Stack<GraphModel>       mParentModels            = new Stack<GraphModel>();
    private final ArrayList<Integer>      mParentIds               = new ArrayList<Integer>();
    private boolean	                      mEditable                = true;

    // Calling this constructor does not create a vertex outline creator
    // which is required by the method addVertexAndCreateId()
    public GraphModelManager()
    {
        mGraphModel = new GraphModel();
        mGraphModel.setManager(this);
    }

    public GraphModelManager(GraphModel newModel) {
        newModel.setManager(this);
        mGraphModel = newModel;
    }

    public void replace(GraphModel newModel) {
        mParentModels.clear();

        //zoom back to where we were
        for (Iterator<Integer> iter = mParentIds.iterator(); iter.hasNext();) {
            Integer parentId = iter.next();
            GraphableVertex childModelVertex = (GraphableVertex)newModel.getVertexById(parentId.intValue());
            if (childModelVertex == null) { // we've been deleted, stay here
                Logger.msg(7, "Didn't find "+parentId+" in new model tree. Stopping here.");
                do { iter.remove(); } while (iter.hasNext());
                break;
            }
            else {
                mParentModels.push(newModel);
                Logger.msg(7, "Pushing model and switching to "+parentId);
                newModel = childModelVertex.getChildGraphModel();
            }
        }
        setModel(newModel);
    }

    public void setModel(GraphModel newModel) {
        // reset transient data
        newModel.mNewEdgeOriginVertex = null;
        newModel.mNewEdgeEndPoint     = null;

        // copy factories over
        newModel.setExternalEdgeFactory(mEdgeFactory);
        newModel.setExternalVertexFactory(mVertexFactory);
        newModel.setVertexOutlineCreator(mVertexOutlineCreator);
        mVertexFactory.setCreationContext(newModel.getContainingVertex());
        newModel.setManager(this);
        mGraphModel.setManager(null);
        mGraphModel = newModel;

        // notify
        notifyObservers(mEntireModelChangedEvent);
    }

    public void zoomIn(Vertex child) {
        GraphModel childModel = child.getChildGraphModel();
        if (childModel != null) {
            mParentModels.push(mGraphModel);
            mParentIds.add(Integer.valueOf(child.getID()));
            setModel(childModel);
            Logger.msg(7, "ZoomIn - Stack size: "+mParentModels.size()+" ids:"+mParentIds.size());
        }
    }

    public void zoomOut() {
        if (!mParentModels.empty()) {
            setModel(mParentModels.pop());
            mParentIds.remove(mParentIds.size()-1);
        }
        Logger.msg(7, "ZoomOut - Stack size: "+mParentModels.size()+" ids:"+mParentIds.size());

    }

    public void forceNotify()
    {
        notifyObservers(mForcedNotifyEvent);
    }

    public GraphModel getModel() {
        return mGraphModel;
    }

    public void setExternalEdgeFactory(EdgeFactory newEdgeFactory) {
        mEdgeFactory = newEdgeFactory;
        mGraphModel.setExternalEdgeFactory(newEdgeFactory);
    }

    public void setExternalVertexFactory(VertexFactory newVertexFactory) {
        mVertexFactory = newVertexFactory;
        mGraphModel.setExternalVertexFactory(newVertexFactory);
    }

    public void setVertexOutlineCreator(VertexOutlineCreator newVertexOutlineCreator) {
        mVertexOutlineCreator = newVertexOutlineCreator;
        mGraphModel.setVertexOutlineCreator(newVertexOutlineCreator);
    }

    public void notifyObservers(GraphModelEvent ev) {
    	setChanged();
        super.notifyObservers(ev);
    }

    public void setEditable(boolean editable) {
        mEditable = editable;
    }

    public boolean isEditable() {
        return mEditable;
    }


}
