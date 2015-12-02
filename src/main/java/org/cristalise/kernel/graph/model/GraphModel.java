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

import java.util.Hashtable;

import org.cristalise.kernel.graph.event.ClearedEvent;
import org.cristalise.kernel.graph.event.EdgeRemovedEvent;
import org.cristalise.kernel.graph.event.EdgesChangedEvent;
import org.cristalise.kernel.graph.event.ForcedNotifyEvent;
import org.cristalise.kernel.graph.event.GraphModelEvent;
import org.cristalise.kernel.graph.event.GraphModelResizedEvent;
import org.cristalise.kernel.graph.event.NewEdgeEndPointChangedEvent;
import org.cristalise.kernel.graph.event.StartVertexIdChangedEvent;
import org.cristalise.kernel.graph.event.VertexAddedEvent;
import org.cristalise.kernel.graph.event.VertexCreatedEvent;
import org.cristalise.kernel.graph.event.VertexMovedEvent;
import org.cristalise.kernel.graph.event.VertexRemovedEvent;
import org.cristalise.kernel.graph.event.VerticesChangedEvent;
import org.cristalise.kernel.utils.Logger;


public class GraphModel {
    /* Persistent data */

    private int mWidth = 0;
    private int mHeight = 0;
    private int mNextId = 0;
    protected int mStartVertexId = -1;
    protected Hashtable<String, Vertex> mVertexHashtable = new Hashtable<String, Vertex>();
    protected Hashtable<String, DirectedEdge> mEdgeHashtable = new Hashtable<String, DirectedEdge>();
    private GraphableVertex mContainingVertex;

    /* Transient data */

    protected transient Vertex            mNewEdgeOriginVertex = null;
    protected transient GraphPoint        mNewEdgeEndPoint = null;

    private transient GraphModelManager mManager = null;

    /* External factories */

    private VertexFactory mExternalVertexFactory = null;
    private EdgeFactory mExternalEdgeFactory = null;

    /* Vertex outline creator */

    private VertexOutlineCreator mVertexOutlineCreator = null;

    /* Notification Events */

    private final ClearedEvent mClearedEvent = new ClearedEvent();
    private final EdgeRemovedEvent mEdgeRemovedEvent = new EdgeRemovedEvent();
    private final EdgesChangedEvent mEdgesChangedEvent = new EdgesChangedEvent();
    private final ForcedNotifyEvent mForcedNotifyEvent = new ForcedNotifyEvent();
    private final NewEdgeEndPointChangedEvent mNewEdgeEndPointChangedEvent = new NewEdgeEndPointChangedEvent();
    private final StartVertexIdChangedEvent mStartVertexIdChangedEvent = new StartVertexIdChangedEvent();
    private final VertexAddedEvent mVertexAddedEvent = new VertexAddedEvent();
    private final VertexCreatedEvent mVertexCreatedEvent = new VertexCreatedEvent();
    private final VertexMovedEvent mVertexMovedEvent = new VertexMovedEvent();
    private final VertexRemovedEvent mVertexRemovedEvent = new VertexRemovedEvent();
    private final VerticesChangedEvent mVerticesChangedEvent = new VerticesChangedEvent();
    private final GraphModelResizedEvent mGraphModelResizedEvent = new GraphModelResizedEvent();

    // Calling this constructor does not create a vertex outline creator
    // which is required by the method addVertexAndCreateId()

    private static int count=0;

    // count instances for debugging
    private int number;

    public GraphModel() {
        number=++count;

    }

    public int getNumber() {
        return number;
    }

    public void setNextId(int id) {
        mNextId = id;
    }

    public int getNextId() {
        return mNextId;
    }

    public void setManager(GraphModelManager mgr) {
        mManager = mgr;
    }

    public GraphModelManager getManager() {
        return mManager;
    }

    public GraphModel(VertexOutlineCreator vertexOutlineCreator) {
        mVertexOutlineCreator = vertexOutlineCreator;
    }

    public void setWidth(int width) {

        mWidth = width;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setHeight(int height) {

        mHeight = height;
    }

    public int getHeight() {
        return mHeight;
    }

    public void checkSize(Vertex v) {
        boolean resized = false;
        GraphPoint centre = v.getCentrePoint();
        if (getWidth() < centre.x + v.getWidth()/2 +10 ) {
            setWidth( centre.x + v.getWidth()/2 +10 );
            resized = true;
        }

        if (getHeight() < centre.y + v.getHeight()/2 +10 ) {
            setHeight(centre.y + v.getHeight()/2 +10 );
            resized = true;
        }

        if (resized) {
            publishEvent(mGraphModelResizedEvent);
        }

    }

    public void setStartVertexId(int id) {
        mStartVertexId = id;
        publishEvent(mStartVertexIdChangedEvent);
    }

    public int getStartVertexId() {
        return mStartVertexId;
    }

    public Vertex getStartVertex() {
        return resolveVertex(getStartVertexId());
    }

    /**
     * @return Returns the mParentVertex.
     */
    public GraphableVertex getContainingVertex() {
        return mContainingVertex;
    }
    /**
     * @param parentVertex The mParentVertex to set.
     */
    public void setContainingVertex(GraphableVertex vertex) {
        mContainingVertex = vertex;
    }

    public void setVertices(Vertex[] vertices) {
        mVertexHashtable = new Hashtable<String, Vertex>();
        for (Vertex vertice : vertices) {
            mVertexHashtable.put(String.valueOf(vertice.getID()), vertice);
            checkSize(vertice);

        }
        publishEvent(mVerticesChangedEvent);
    }

    public Vertex[] getVertices() {
        Object[] vertexObjs = mVertexHashtable.values().toArray();
        Vertex[] vertices = new Vertex[vertexObjs.length];
        int i = 0;
        for (i = 0; i < vertices.length; i++) {
            vertices[i] = (Vertex)vertexObjs[i];
        }
        return vertices;
    }

    public void setEdges(DirectedEdge[] edges) {
        mEdgeHashtable = new Hashtable<String, DirectedEdge>();
        for (DirectedEdge edge : edges) {
            mEdgeHashtable.put(String.valueOf(edge.getID()), edge);
        }
        publishEvent(mEdgesChangedEvent);
    }

    public DirectedEdge[] getEdges() {
        Object[] edgeObjs = mEdgeHashtable.values().toArray();
        DirectedEdge[] edges = new DirectedEdge[edgeObjs.length];
        int i = 0;
        for (i = 0; i < edges.length; i++) {
            edges[i] = (DirectedEdge)edgeObjs[i];
        }
        return edges;
    }

	public Vertex getVertexById(int id) {
		return mVertexHashtable.get(String.valueOf(id));
	}


    public int addEdgeAndCreateId(DirectedEdge e, int originId, int terminusId) {
        return addEdgeAndCreateId(e, resolveVertex(originId), resolveVertex(terminusId));
    }

    public int addEdgeAndCreateId(DirectedEdge e, Vertex origin, Vertex terminus) {
        e.setID(mNextId);
        e.setOriginVertexId(origin.getID());
        e.setOriginPoint(origin.getCentrePoint());
        e.setTerminusVertexId(terminus.getID());
        e.setTerminusPoint(terminus.getCentrePoint());
        origin.addOutEdgeId(mNextId);
        terminus.addInEdgeId(mNextId);
        mEdgeHashtable.put(String.valueOf(mNextId), e);
        mNextId++;
        return mNextId - 1;
    }

    // Removes an edge, but does not modify the selection
    public void removeEdge(DirectedEdge e) {
        Vertex origin = getOrigin(e);
        Vertex terminus = getTerminus(e);
        int edgeId = e.getID();
        // Remove the id of the edge from the origin and terminus vertices
        origin.removeOutEdgeId(edgeId);
        terminus.removeInEdgeId(edgeId);
        // Remove the edge
        mEdgeHashtable.remove(String.valueOf(e.getID()));
        publishEvent(mEdgeRemovedEvent);
    }

    public int addVertexAndCreateId(Vertex v, GraphPoint location) {
        if (location != null) {
	        if (mVertexOutlineCreator == null) {
	            Logger.warning("You cannot add a vertex with no outline creator");
	            return -1;
	        }
	        placeVertex(v, location);
	    }
        mVertexHashtable.put(String.valueOf(mNextId), v);
		v.setID(mNextId);
        return mNextId++;
    }

    public void placeVertex(Vertex v, GraphPoint location) {
        v.setCentrePoint(location);
        if (mVertexOutlineCreator != null) {
            mVertexOutlineCreator.setOutline(v);
        }
        publishEvent(mVertexAddedEvent);
        checkSize(v);
    }

    // Removes a vertex, but does not modify the selection
    public void removeVertex(Vertex v) {
        DirectedEdge[] inEdges = getInEdges(v);
        DirectedEdge[] outEdges = getOutEdges(v);
        Vertex origin = null;
        Vertex terminus = null;
        int edgeId = -1;
        int i = 0;
        // For each in edge
        for (i = 0; i < inEdges.length; i++) {
            edgeId = inEdges[i].getID();
            origin = getOrigin(inEdges[i]);
            // Remove the id of the edge from the origin vertex
            origin.removeOutEdgeId(edgeId);
            // Remove the edge
            mEdgeHashtable.remove(String.valueOf(edgeId));
        }
        // Remove all the out edges
        for (i = 0; i < outEdges.length; i++) {
            edgeId = outEdges[i].getID();
            terminus = getTerminus(outEdges[i]);
            // Remove the id of the edge from the terminus vertex
            terminus.removeInEdgeId(edgeId);
            // Remove the edge
            mEdgeHashtable.remove(String.valueOf(edgeId));
        }
        // Remove the vertex
        mVertexHashtable.remove(String.valueOf(v.getID()));
        publishEvent(mVertexRemovedEvent);
    }

    public void moveAbsoluteVertex(Vertex v, GraphPoint p) {
        // Make sure the new position stays within the graph
        if (p.x < 0) p.x = 0;
        if (p.y < 0) p.y = 0;
        if (p.x > mWidth) p.x = mWidth;
        if (p.y > mHeight) p.y = mHeight;
        moveAbsoluteVertexAndConnectingEdges(v, p);
        publishEvent(mVertexMovedEvent);
    }

    private void moveAbsoluteVertexAndConnectingEdges(Vertex v, GraphPoint p) {
        DirectedEdge[] inEdges = getInEdges(v);
        DirectedEdge[] outEdges = getOutEdges(v);
        int i = 0;
        // Move the vertex to the new position
        v.moveAbsolute(p);
        // Move the ends of the incoming edges to the new position
        for (i = 0; i < inEdges.length; i++) {
            inEdges[i].setTerminusPoint(p);
        }
        // Move the ends of the outgoing edges to the new position
        for (i = 0; i < outEdges.length; i++) {
            outEdges[i].setOriginPoint(p);
        }
        checkSize(v);
    }



    public Vertex resolveVertex(int id) {
        return mVertexHashtable.get(String.valueOf(id));
    }

    public DirectedEdge resolveEdge(int id) {
        return mEdgeHashtable.get(String.valueOf(id));
    }

    public DirectedEdge[] getInEdges(Vertex v) {
        int[] ids = v.getInEdgeIds();
        return resolveEdges(ids);
    }

    public DirectedEdge[] getOutEdges(Vertex v) {
        int[] ids = v.getOutEdgeIds();
        return resolveEdges(ids);
    }

    private DirectedEdge[] resolveEdges(int[] ids) {
        DirectedEdge[] edges = new DirectedEdge[ids.length];
        int i = 0;
        for (i = 0; i < ids.length; i++) {
            edges[i] = resolveEdge(ids[i]);
        }
        return edges;
    }

    public Vertex getOrigin(DirectedEdge e) {
        return resolveVertex(e.getOriginVertexId());
    }

    public Vertex getTerminus(DirectedEdge e) {
        return resolveVertex(e.getTerminusVertexId());
    }

    public Vertex[] getInVertices(Vertex v) {
        DirectedEdge[] inEdges = getInEdges(v);
        Vertex[] inVertices = new Vertex[inEdges.length];
        int i = 0;
        for (i = 0; i < inEdges.length; i++) {
            inVertices[i] = getOrigin(inEdges[i]);
        }
        return inVertices;
    }

    public Vertex[] getOutVertices(Vertex v) {
        DirectedEdge[] outEdges = getOutEdges(v);
        Vertex[] outVertices = new Vertex[outEdges.length];
        int i = 0;
        for (i = 0; i < outEdges.length; i++) {
            outVertices[i] = getTerminus(outEdges[i]);
        }
        return outVertices;
    }

    public DirectedEdge[] getConnectingEdges(int originVertexId, int terminusVertexId) {
        Vertex origin = resolveVertex(originVertexId);
        DirectedEdge[] outEdges = null;
        int numEdgesFound = 0;
        DirectedEdge[] edgesFound = null;
        int i = 0;
        int j = 0;
        if (origin == null) return null;
        outEdges = getOutEdges(origin);
        for (i = 0; i < outEdges.length; i++) {
            if (outEdges[i].getTerminusVertexId() == terminusVertexId) {
                numEdgesFound++;
            }
        }
        edgesFound = new DirectedEdge[numEdgesFound];
        for (i = 0; i < outEdges.length; i++) {
            if (outEdges[i].getTerminusVertexId() == terminusVertexId) {
                edgesFound[j] = outEdges[i];
                j++;
            }
        }
        return edgesFound;
    }

    public void clearTags(Object tag) {
        Vertex vertex = null;
        Object[] vertexObjs = mVertexHashtable.values().toArray();
        int i = 0;
        for (i = 0; i < vertexObjs.length; i++) {
            vertex = (Vertex)vertexObjs[i];
            vertex.clearTag(tag);
        }
    }

    public void forceNotify() {
        publishEvent(mForcedNotifyEvent);
    }

    public void clear() {
        mVertexHashtable = new Hashtable<String, Vertex>();
        mEdgeHashtable = new Hashtable<String, DirectedEdge>();
        mStartVertexId = -1;
        publishEvent(mClearedEvent);
    }



    public void setNewEdgeOriginVertex(Vertex v) {
        mNewEdgeOriginVertex = v;
    }

    public Vertex getNewEdgeOriginVertex() {
        return mNewEdgeOriginVertex;
    }

    public void setNewEdgeEndPoint(GraphPoint p) {
        mNewEdgeEndPoint = p;
        publishEvent(mNewEdgeEndPointChangedEvent);
    }

    public GraphPoint getNewEdgeEndPoint() {
        return mNewEdgeEndPoint;
    }

    public void setExternalVertexFactory(VertexFactory factory) {
        mExternalVertexFactory = factory;
    }

    public void createVertex(GraphPoint location, TypeNameAndConstructionInfo typeNameAndConstructionInfo) throws Exception {
        if (mExternalVertexFactory != null) {
            mExternalVertexFactory.create(mManager, location, typeNameAndConstructionInfo);
            publishEvent(mVertexCreatedEvent);
        }
    }

    private void publishEvent(GraphModelEvent event) {
		if (mManager!=null)
			mManager.notifyObservers(event);
		
	}

	public void setExternalEdgeFactory(EdgeFactory factory) {
        mExternalEdgeFactory = factory;
    }

    public void setVertexOutlineCreator(VertexOutlineCreator outlineCreator) {
        mVertexOutlineCreator = outlineCreator;
    }

    public void createDirectedEdge(Vertex origin, Vertex terminus, TypeNameAndConstructionInfo typeNameAndConstructionInfo) {
        if (mExternalEdgeFactory != null) {
            mExternalEdgeFactory.create(mManager, origin, terminus, typeNameAndConstructionInfo);
        }
    }



    public void resetVertexOutlines() {
        Vertex[] vertices = getVertices();
        int i = 0;
        for (i = 0; i < vertices.length; i++) {
            mVertexOutlineCreator.setOutline(vertices[i]);
        }
    }

    public void setGraphModelCastorData(GraphModelCastorData data) {
        int i = 0;

        // Create and populate the vertex hashtable
        mVertexHashtable = new Hashtable<String, Vertex>();
        for (i = 0; i < data.mVertexImpls.length; i++) {
            mVertexHashtable.put(String.valueOf(data.mVertexImpls[i].getID()), data.mVertexImpls[i]);
            checkSize(data.mVertexImpls[i]);
        }
        // Create and populate the edge hastable
        mEdgeHashtable = new Hashtable<String, DirectedEdge>();
        for (i = 0; i < data.mEdgeImpls.length; i++) {
            mEdgeHashtable.put(String.valueOf(data.mEdgeImpls[i].getID()), data.mEdgeImpls[i]);
        }
        // Set the start vertex id and the id generation counter
        mStartVertexId = data.mStartVertexId;
        mNextId = data.mNextId;
    }

    public GraphModelCastorData getGraphModelCastorData() {
        Object[] vertexObjs = mVertexHashtable.values().toArray();
        Vertex[] vertexImpls = new Vertex[vertexObjs.length];
        Object[] edgeObjs = mEdgeHashtable.values().toArray();
        DirectedEdge[] directedEdgeImpls = new DirectedEdge[edgeObjs.length];
        String className = null;
        int i = 0;
        // Put in the vertices
        for (i = 0; i < vertexImpls.length; i++) {
            vertexImpls[i] = (Vertex)vertexObjs[i];
        }
        // Put in the edges
        for (i = 0; i < directedEdgeImpls.length; i++) {
            directedEdgeImpls[i] = (DirectedEdge)edgeObjs[i];
        }
        // Disable persistency of the vertex outline creator: determined by container
        // Determine the class name of the vertex outline creator
//        if (mVertexOutlineCreator == null) {
//            className = "";
//        }
//        else {
//            className = mVertexOutlineCreator.getClass().getName();
//        }
        return new GraphModelCastorData(className, vertexImpls, directedEdgeImpls, mStartVertexId, mNextId);
    }
}
