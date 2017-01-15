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
package org.cristalise.kernel.persistency;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.Logger;


/**
 * <p>Interface for persistency managers of entities. It allows different kernel
 * objects to be stored in different db backend. For instance, Properties may be
 * stored in LDAP, while Events, Outcomes and Viewpoints could be stored in a
 * relational database. The kernel does and needs no analytical querying of the 
 * ClusterStorages, only simple gets and puts. This may be implemented on top
 * of the storage implementation separately.
 * 
 * <p>Each item is indexed by its {@link ItemPath}, which is may be constructed from its
 * UUID, equivalent {@link SystemKey} object, or 
 * 
 * <p>Each first-level path under the Item is defined as a Cluster. Different
 * Clusters may be stored in different places. Each ClusterStorage must support
 * {@link #get(ItemPath, String)} and
 * {@link #getClusterContents(ItemPath, String)} for clusters they return
 * {@link #READ} and {@link #READWRITE} from queryClusterSupport and
 * {@link #put(ItemPath, C2KLocalObject)} and {@link #delete(ItemPath, String)}
 * for clusters they return {@link #WRITE} and {@link #READWRITE} from
 * {@link #getClusterContents(ItemPath, String)}. Operations that have not been
 * declared as not supported should throw a PersistencyException. If a
 * cluster does not exist, get should return null, and delete should return with
 * no action.
 */
public abstract class ClusterStorage {
    /**
     * Constant to return from {@link #queryClusterSupport(String)} for Cluster
     * types this storage does not support.
     */
    public static final short NONE = 0;
    /**
     * Constant to return from {@link #queryClusterSupport(String)} for Cluster
     * types this storage can read from a database but not write. An example
     * would be pre-existing data in a database that is mapped to Items in some
     * way.
     */
    public static final short READ = 1;
    /**
     * Constant to return from {@link #queryClusterSupport(String)} for Cluster
     * types this storage can write to a database but not read. An example would
     * be a realtime database export of data, which is transformed in an
     * unrecoverable way for use in other systems.
     */
    public static final short WRITE = 2;
    /**
     * Constant to return from {@link #queryClusterSupport(String)} for data
     * stores that CRISTAL may use for both reading and writing for the given
     * Cluster type.
     */
    public static final short READWRITE = 3;

    // Cluster types
    /**
     * The defined path of the root of the CRISTAL Kernel object cluster tree. A
     * zero-length string.
     */
    public static final String ROOT = "";
    /**
     * The root of the Property object cluster. All Property paths start with
     * this. Defined as "Property". Properties are stored underneath according
     * to their name e.g. "Property/Name"
     */
    public static final String PROPERTY = "Property";
    /**
     * The root of the Collection object cluster. All Collection paths start
     * with this. Defined as "Collection". Collections are stored underneath by
     * name e.g. "Collection/Composition"
     */
    public static final String COLLECTION = "Collection";
    /**
     * The cluster which holds the Item workflow. Defined as "LifeCycle". Holds
     * the workflow inside, which is named "workflow", hence
     * "LifeCycle/workflow".
     * 
     * @see org.cristalise.kernel.lifecycle.instance.Workflow
     */
    public static final String LIFECYCLE = "LifeCycle";
    /**
     * This cluster holds all outcomes of this Item. The path to each outcome is
     * "Outcome/<i>Schema Name</i>/<i>Schema Version</i>/<i>Event ID</i>"
     */
    public static final String OUTCOME = "Outcome";
    /**
     * This is the cluster that contains all event for this Item. This cluster
     * may be instantiated in a client as a History, which is a RemoteMap.
     * Events are stored with their ID: "/AuditTrail/<i>Event ID</i>"
     */
    public static final String HISTORY = "AuditTrail";
    /**
     * This cluster contains all viewpoints. Its name is defined as "ViewPoint".
     * The paths of viewpoint objects stored here follow this pattern:
     * "ViewPoint/<i>Schema Name</i>/<i>Viewpoint Name</i>"
     */
    public static final String VIEWPOINT = "ViewPoint";
    /**
     * Agents store their persistent jobs in this cluster that have been pushed
     * to them by activities configured to do so. The name is defined as "Job"
     * and each new job received is assigned an integer ID one more than the
     * highest already present.
     */
    public static final String JOB = "Job";

    /**
     * An array of all currently supported cluster types, for iterative
     * purposes.
     */
    public static final String[] allClusterTypes = { PROPERTY, COLLECTION, LIFECYCLE, OUTCOME, HISTORY, VIEWPOINT, JOB };

    /**
     * Connects to the storage. It must be possible to retrieve CRISTAL local
     * objects after this method returns.
     * 
     * @param auth
     *            The Authenticator instance that the user or server logged in
     *            with.
     * @throws PersistencyException
     *             If storage initialization failed
     */
    public abstract void open(Authenticator auth) throws PersistencyException;

    /**
     * Shuts down the storage. Data must be completely written to disk before
     * this method returns, so the process can exit. No further gets or puts
     * should follow.
     * 
     * @throws PersistencyException
     *             If closing failed
     */
    public abstract void close() throws PersistencyException;

    /**
     * Declares whether or not this ClusterStorage can read or write a
     * particular CRISTAL local object type.
     * 
     * @param clusterType
     *            The Cluster type requested. Must be one of the Cluster type
     *            constants from this class.
     * @return A ClusterStorage constant: NONE, READ, WRITE, or READWRITE
     */
    public abstract short queryClusterSupport(String clusterType);

    /**
     * Checks whether the storage support the given type of query or not
     * 
     * @param language type of the query (e.g. SQL/XQuery/XPath/....)
     * @return whether the Storage supports the type of the query or not
     */
    public abstract boolean checkQuerySupport(String language);

    /**
     * @return A full name of this storage for logging
     */
    public abstract String getName();

    /**
     * @return A short code for this storage for reference
     */
    public abstract String getId();

    /**
     * Utility method to find the cluster for a particular Local Object (the first part of its path)
     * 
     * @param path object path
     * @return The cluster to which it belongs
     */
    protected static String getClusterType(String path) {
        try {
            if (path == null || path.length() == 0) return ClusterStorage.ROOT;

            int start = path.charAt(0) == '/' ? 1 : 0;
            int end = path.indexOf('/', start + 1);
            
            if (end == -1) end = path.length();

            return path.substring(start, end);
        }
        catch (Exception ex) {
            Logger.error(ex);
            return ClusterStorage.ROOT;
        }
    }

    /**
     * Gives the path for a local object. Varies by Cluster.
     * 
     * @param obj C2KLocalObject
     * @return Its path
     */
    public static String getPath(C2KLocalObject obj) {
        String root = obj.getClusterType();

        if (root == null) return null; // no storage allowed

        return obj.getClusterPath();
    }

    /**
     * Executes an SQL/OQL/XQuery/XPath/etc query in the target database. 
     * 
     * @param query the query to be executed
     * @return the xml result of the query
     */
    public abstract String executeQuery(Query query) throws PersistencyException;

    /**
     * Fetches a CRISTAL local object from storage by path
     * 
     * @param itemPath
     *            The ItemPath of the containing Item
     * @param path
     *            The path of the local object
     * @return The C2KLocalObject, or null if the object was not found
     * @throws PersistencyException
     *             when retrieval failed
     */
    public abstract C2KLocalObject get(ItemPath itemPath, String path) throws PersistencyException;

    /**
     * Stores a CRISTAL local object. The path is automatically generated.
     * 
     * @param itemPath
     *            The Item that the object will be stored under
     * @param obj
     *            The C2KLocalObject to store
     * @throws PersistencyException
     *             When storage fails
     */
    public abstract void put(ItemPath itemPath, C2KLocalObject obj) throws PersistencyException;

    /**
     * Remove a CRISTAL local object from storage. This should be used sparingly
     * and responsibly, as it violated traceability. Objects removed in this way
     * are not expected to be recoverable.
     * 
     * @param itemPath
     *            The containing Item
     * @param path
     *            The path of the object to be removed
     * @throws PersistencyException
     *             When deletion fails or is not allowed
     */
    public abstract void delete(ItemPath itemPath, String path) throws PersistencyException;

    /**
     * Queries the local path below the given root and returns the possible next
     * elements.
     * 
     * @param itemPath
     *            The Item to query
     * @param path
     *            The path within that Item to query. May be ClusterStorage.ROOT
     *            (empty String)
     * @return A String array of the possible next path elements
     * @throws PersistencyException
     *             When an error occurred during the query
     */
    public abstract String[] getClusterContents(ItemPath itemPath, String path) throws PersistencyException;
}
