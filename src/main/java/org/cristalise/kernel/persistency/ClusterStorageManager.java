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

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.agent.JobList;
import org.cristalise.kernel.entity.proxy.ProxyMessage;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.kernel.utils.SoftCache;
import org.cristalise.kernel.utils.WeakCache;


/**
 * Instantiates ClusterStorages listed in properties file All read/write requests to storage pass through this object, 
 * which can query the capabilities of each declared storage, and channel requests accordingly. Transaction based.
 */
public class ClusterStorageManager {
    HashMap<String, ClusterStorage> allStores = new HashMap<String, ClusterStorage>();
    String[] clusterPriority = new String[0];
    HashMap<String, ArrayList<ClusterStorage>> clusterWriters = new HashMap<String, ArrayList<ClusterStorage>>();
    HashMap<String, ArrayList<ClusterStorage>> clusterReaders = new HashMap<String, ArrayList<ClusterStorage>>();
    ArrayList<TransactionalClusterStorage> transactionalStores = new ArrayList<TransactionalClusterStorage>();

    // we don't need a soft cache for the top level cache - the proxies and entities clear that when reaped
    HashMap<ItemPath, Map<String, C2KLocalObject>> memoryCache = new HashMap<ItemPath, Map<String, C2KLocalObject>>();

    /**
     * Initialises all ClusterStorage handlers listed by class name in the property "ClusterStorages"
     * This property is usually process specific, and so should be in the server/client.conf and not the connect file.
     * 
     * @param auth
     * @throws PersistencyException
     */
    public ClusterStorageManager(Authenticator auth) throws PersistencyException {
        Object clusterStorageProp = Gateway.getProperties().getObject("ClusterStorage");
        if (clusterStorageProp == null || clusterStorageProp.equals("")) {
            throw new PersistencyException("ClusterStorageManager.init() - no ClusterStorages defined. No persistency!");
        }

        ArrayList<ClusterStorage> rootStores;
        if (clusterStorageProp instanceof String)
            rootStores = instantiateStores((String)clusterStorageProp);
        else if (clusterStorageProp instanceof ArrayList<?>) {
            ArrayList<?> propStores = (ArrayList<?>)clusterStorageProp;
            rootStores = new ArrayList<ClusterStorage>();
            clusterPriority = new String[propStores.size()];
            for (Object thisStore : propStores) {
                if (thisStore instanceof ClusterStorage)
                    rootStores.add((ClusterStorage)thisStore);
                else
                    throw new PersistencyException("Supplied ClusterStorage "+thisStore.toString()+" was not an instance of ClusterStorage");
            }
        }
        else {
            throw new PersistencyException("Unknown class of ClusterStorage property: "+clusterStorageProp.getClass().getName());
        }

        int clusterNo = 0;
        for (ClusterStorage newStorage : rootStores) {
            try {
                newStorage.open(auth);
            }catch (PersistencyException ex) {
                Logger.error(ex);
                throw new PersistencyException("ClusterStorageManager.init() - Error initialising storage handler " + newStorage.getClass().getName() + ": " + ex.getMessage());
            }
            Logger.msg(5, "ClusterStorageManager.init() - Cluster storage " + newStorage.getClass().getName() + " initialised successfully.");
            allStores.put(newStorage.getId(), newStorage);
            clusterPriority[clusterNo++] = newStorage.getId();

            if (newStorage instanceof TransactionalClusterStorage) transactionalStores.add((TransactionalClusterStorage)newStorage);
        }
        clusterReaders.put(ClusterStorage.ROOT, rootStores); // all storages are queried for clusters at the root level
    }

    public ArrayList<ClusterStorage> instantiateStores(String allClusters) throws PersistencyException {
        ArrayList<ClusterStorage> rootStores = new ArrayList<ClusterStorage>();
        StringTokenizer tok = new StringTokenizer(allClusters, ",");
        clusterPriority = new String[tok.countTokens()];

        while (tok.hasMoreTokens()) {
            ClusterStorage newStorage = null;
            String newStorageClass = tok.nextToken();
            try {
                try {
                    newStorage = (ClusterStorage)(Class.forName(newStorageClass).newInstance());
                }
                catch (ClassNotFoundException ex2) {
                    newStorage = (ClusterStorage)(Class.forName("org.cristalise.storage."+newStorageClass).newInstance());
                }
            }
            catch (ClassNotFoundException ex) {
                throw new PersistencyException("ClusterStorageManager.init() - The cluster storage handler class "+newStorageClass+" could not be found.");
            }
            catch (InstantiationException ex) {
                throw new PersistencyException("ClusterStorageManager.init() - The cluster storage handler class "+newStorageClass+" could not be instantiated.");
            }
            catch (IllegalAccessException ex) {
                throw new PersistencyException("ClusterStorageManager.init() - The cluster storage handler class "+newStorageClass+" was not allowed to be instantiated.");
            }
            rootStores.add(newStorage);
        }
        return rootStores;
    }

    public void close() {
        for (ClusterStorage thisStorage : allStores.values()) {
            try {
                thisStorage.close();
            } 
            catch (PersistencyException ex) {
                Logger.error(ex);
            }
        }
    }

    /**
     * Check which storage can execute the given query
     * 
     * @param language the language of the query
     * @return the found store or null
     */
    private ClusterStorage findStorageForQuery(String language) {
        for (String element : clusterPriority) {
            ClusterStorage store = allStores.get(element);
            if (store.checkQuerySupport(language) ) return store;
        }
        return null;
    }

    /**
     * Returns the loaded storage that declare that they can handle writing or reading the specified cluster name (e.g.
     * Collection, Property) Must specify if the request is a read or a write.
     * 
     * @param clusterType
     * @param forWrite whether the request is for write or read
     * @return the list of usable storages
     */
    private ArrayList<ClusterStorage> findStorages(String clusterType, boolean forWrite) {
        // choose the right cache for readers or writers
        HashMap<String, ArrayList<ClusterStorage>> cache;

        if (forWrite) cache = clusterWriters;
        else          cache = clusterReaders;

        // check to see if we've been asked to do this before
        if (cache.containsKey(clusterType)) return cache.get(clusterType);

        // not done yet, we'll have to query them all
        Logger.msg(7, "ClusterStorageManager.findStorages() - finding storage for "+clusterType+" forWrite:"+forWrite);

        ArrayList<ClusterStorage> useableStorages = new ArrayList<ClusterStorage>();

        for (String element : clusterPriority) {
            ClusterStorage thisStorage = allStores.get(element);
            short requiredSupport = forWrite ? ClusterStorage.WRITE : ClusterStorage.READ;
            if ((thisStorage.queryClusterSupport(clusterType) & requiredSupport) == requiredSupport) {
                Logger.msg(7, "ClusterStorageManager.findStorages() - Got "+thisStorage.getName());
                useableStorages.add(thisStorage);
            }
        }
        cache.put(clusterType, useableStorages);
        return useableStorages;
    }

    /**
     * 
     * @param query
     * @return the result of the query
     * @throws PersistencyException
     */
    public String executeQuery(Query query) throws PersistencyException {
        ClusterStorage reader = findStorageForQuery(query.getLanguage());

        if (reader != null) return reader.executeQuery(query);
        else                throw new PersistencyException("No storage was found supporting language:"+query.getLanguage()+" query:"+query.getName());
    }

    /**
     * Retrieves the ids of the next level of a cluster
     * Does not look in any currently open transactions.
     * 
     * @param itemPath
     * @param path
     * @return list of keys found in the cluster
     * @throws PersistencyException
     */
    public String[] getClusterContents(ItemPath itemPath, String path) throws PersistencyException {
        ArrayList<String> contents = new ArrayList<String>();
        // get all readers
        Logger.msg(8, "ClusterStorageManager.getClusterContents() - path:"+path);
        ArrayList<ClusterStorage> readers = findStorages(ClusterStorage.getClusterType(path), false);
        // try each in turn until we get a result
        for (ClusterStorage thisReader : readers) {
            try {
                String[] thisArr = thisReader.getClusterContents(itemPath, path);
                if (thisArr != null) {
                    for (int j = 0; j < thisArr.length; j++)
                        if (!contents.contains(thisArr[j])) {
                            Logger.msg(9, "ClusterStorageManager.getClusterContents() - "+thisReader.getName()+" reports "+thisArr[j]);
                            contents.add(thisArr[j]);
                        }
                }
            }
            catch (PersistencyException e) {
                Logger.msg(5, "ClusterStorageManager.getClusterContents() - reader " + thisReader.getName() +
                        " could not retrieve contents of " + itemPath + "/" + path + ": " + e.getMessage());
            }
        }

        Logger.msg(8, "ClusterStorageManager.getClusterContents() - Returning "+contents.size()+" elements of path:"+path);

        String[] retArr = new String[0];
        retArr = contents.toArray(retArr);
        return retArr;
    }

    /** 
     * Internal get method. Retrieves clusters from ClusterStorages & maintains the memory cache.
     * <br>
     * There is a special case for Viewpoint. When path ends with /data it returns referenced Outcome instead of Viewpoint.
     * 
     * @param itemPath
     * @param path
     * @return the C2KObject located by path
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     */
    public C2KLocalObject get(ItemPath itemPath, String path) throws PersistencyException, ObjectNotFoundException {
        C2KLocalObject result = null;
        // check cache first
        Map<String, C2KLocalObject> sysKeyMemCache = null;
        sysKeyMemCache = memoryCache.get(itemPath);
        
        if (sysKeyMemCache != null) {
            synchronized(sysKeyMemCache) {
                C2KLocalObject obj = sysKeyMemCache.get(path);
                if (obj != null) {
                    Logger.msg(7, "ClusterStorageManager.get() - found "+itemPath+"/"+path+" in memcache");
                    return obj;
                }
            }
        }

        // special case for Viewpoint- When path ends with /data it returns referenced Outcome instead of Viewpoint
        if (path.startsWith(ClusterStorage.VIEWPOINT) && path.endsWith("/data")) {
            StringTokenizer tok = new StringTokenizer(path,"/");
            if (tok.countTokens() == 4) { // to not catch viewpoints called 'data'
                Viewpoint view = (Viewpoint)get(itemPath, path.substring(0, path.lastIndexOf("/")));

                if (view != null) return view.getOutcome();
                else              return null;
            }
        }

        // deal out top level remote maps
        if (path.indexOf('/') == -1) {
            if (path.equals(ClusterStorage.HISTORY)) result = new History(itemPath, null);
            if (path.equals(ClusterStorage.JOB)) {
                if (itemPath instanceof AgentPath) result = new JobList((AgentPath)itemPath, null);
                else                               throw new ObjectNotFoundException("Items do not have job lists");
            }
        }

        if (result == null) {
            // else try each reader in turn until we find it
            ArrayList<ClusterStorage> readers = findStorages(ClusterStorage.getClusterType(path), false);
            for (ClusterStorage thisReader : readers) {
                try {
                    result = thisReader.get(itemPath, path);
                    Logger.msg(7, "ClusterStorageManager.get() - reading "+path+" from "+thisReader.getName() + " for item " + itemPath);
                    if (result != null) break; // got it!
                }
                catch (PersistencyException e) {
                    Logger.msg(7, "ClusterStorageManager.get() - reader "+thisReader.getName()+" could not retrieve "+itemPath+"/"+ path+": "+e.getMessage());
                }
            }
        }

        //No result was found after reading the list of ClusterStorages
        if (result == null) {
            throw new ObjectNotFoundException("ClusterStorageManager.get() - Path "+path+" not found in "+itemPath);
        }

        // got it! store it in the cache
        if (sysKeyMemCache == null) { // create cache if needed
            boolean useWeak = Gateway.getProperties().getBoolean("Storage.useWeakCache", false);
            Logger.msg(7,"ClusterStorageManager.get() - Creating "+(useWeak?"Weak":"Strong")+" cache for item "+itemPath);
            sysKeyMemCache = useWeak?new WeakCache<String, C2KLocalObject>():new SoftCache<String, C2KLocalObject>(0);

            synchronized (memoryCache) {
                memoryCache.put(itemPath, sysKeyMemCache);
            }
        }
        synchronized(sysKeyMemCache) {
            sysKeyMemCache.put(path, result);
        }
        return result;
    }

    /**
     * 
     * @param itemPath
     * @param obj
     * @throws PersistencyException
     */
    public void put(ItemPath itemPath, C2KLocalObject obj) throws PersistencyException {
        put(itemPath, obj, null);
    }

    /**
     * Internal put method. Creates or overwrites a cluster in all writers. Used when committing transactions.
     * 
     * @param itemPath
     * @param obj
     * @param locker
     * @throws PersistencyException
     */
    public void put(ItemPath itemPath, C2KLocalObject obj, Object locker) throws PersistencyException {
        String path = ClusterStorage.getPath(obj);
        ArrayList<ClusterStorage> writers = findStorages(ClusterStorage.getClusterType(path), true);
        for (ClusterStorage thisWriter : writers) {
            try {
                Logger.msg(7, "ClusterStorageManager.put() - writing "+path+" to "+thisWriter.getName());
                if (thisWriter instanceof TransactionalClusterStorage && locker != null)
                    ((TransactionalClusterStorage)thisWriter).put(itemPath, obj, locker);
                else
                    thisWriter.put(itemPath, obj);
            }
            catch (PersistencyException e) {
                Logger.error("ClusterStorageManager.put() - writer " + thisWriter.getName() + " could not store " + itemPath + "/" + path + ": " + e.getMessage());
                throw e;
            }
        }
        // put in mem cache if that worked
        Map<String, C2KLocalObject> sysKeyMemCache;
        if (memoryCache.containsKey(itemPath))
            sysKeyMemCache = memoryCache.get(itemPath);
        else {
            boolean useWeak = Gateway.getProperties().getBoolean("Storage.useWeakCache", false);
            Logger.msg(7,"ClusterStorageManager.put() - Creating "+(useWeak?"Weak":"Strong")+" cache for item "+itemPath);
            sysKeyMemCache = useWeak?new WeakCache<String, C2KLocalObject>():new SoftCache<String, C2KLocalObject>(0);
            synchronized (memoryCache) {
                memoryCache.put(itemPath, sysKeyMemCache);
            }
        }

        synchronized(sysKeyMemCache) {
            sysKeyMemCache.put(path, obj);
        }

        if (Logger.doLog(9)) dumpCacheContents(9);

        // transmit proxy event
        if(Gateway.getProxyServer() != null)
            Gateway.getProxyServer().sendProxyEvent(new ProxyMessage(itemPath, path, ProxyMessage.ADDED));
        else
            Logger.warning("ClusterStorageManager.put() - ProxyServer is null - Proxies are not notified of this event");
    }

    public void remove(ItemPath itemPath, String path) throws PersistencyException {
        remove(itemPath, path, null);
    }

    /**
     * Deletes a cluster from all writers
     * 
     * @param itemPath
     * @param path
     * @param locker
     * @throws PersistencyException
     */
    public void remove(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        ArrayList<ClusterStorage> writers = findStorages(ClusterStorage.getClusterType(path), true);
        for (ClusterStorage thisWriter : writers) {
            try {
                Logger.msg(7, "ClusterStorageManager.delete() - removing "+path+" from "+thisWriter.getName());
                if (thisWriter instanceof TransactionalClusterStorage && locker != null)
                    ((TransactionalClusterStorage)thisWriter).delete(itemPath, path, locker);
                else
                    thisWriter.delete(itemPath, path);
            }
            catch (PersistencyException e) {
                Logger.error("ClusterStorageManager.delete() - writer " + thisWriter.getName() + " could not delete " + itemPath + "/" + path + ": " + e.getMessage());
                throw e;
            }
        }

        if (memoryCache.containsKey(itemPath)) {
            Map<String, C2KLocalObject> itemMemCache = memoryCache.get(itemPath);
            synchronized (itemMemCache) {
                itemMemCache.remove(path);
            }
        }

        // transmit proxy event
        if(Gateway.getProxyServer() != null)
            Gateway.getProxyServer().sendProxyEvent(new ProxyMessage(itemPath, path, ProxyMessage.DELETED));
        else
            Logger.warning("ClusterStorageManager.remove() - ProxyServer is null - Proxies are not notified of this event");
    }

    public void clearCache(ItemPath itemPath, String path) {
        Logger.msg(7, "ClusterStorageManager.clearCache() - removing "+itemPath+"/"+path);

        if (memoryCache.containsKey(itemPath)) {
            Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(itemPath);
            synchronized(sysKeyMemCache) {
                for (Iterator<String> iter = sysKeyMemCache.keySet().iterator(); iter.hasNext();) {
                    String thisPath = iter.next();
                    if (thisPath.startsWith(path)) {
                        Logger.msg(7, "ClusterStorageManager.clearCache() - removing "+itemPath+"/"+thisPath);
                        iter.remove();
                    }
                }
            }
        }
    }

    public void clearCache(ItemPath itemPath) {
        Logger.msg(5, "ClusterStorageManager.clearCache() - removing entire cache of "+itemPath);

        if (memoryCache.containsKey(itemPath)) {
            synchronized (memoryCache) {
                if (Logger.doLog(6)) {
                    Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(itemPath);
                    int size = sysKeyMemCache.size();
                    Logger.msg(6, "ClusterStorageManager.clearCache() - "+size+" objects to remove.");
                }
                memoryCache.remove(itemPath);
            }
        }
        else
            Logger.msg(6, "ClusterStorageManager.clearCache() - No objects cached");
    }

    public void clearCache() {
        synchronized (memoryCache) {
            memoryCache.clear();
        }
        Logger.msg(5, "ClusterStorageManager.clearCache() - cleared entire cache, "+memoryCache.size()+" entities.");
    }

    public void dumpCacheContents(int logLevel) {
        if (!Logger.doLog(logLevel)) return;
        synchronized(memoryCache) {
            for (ItemPath itemPath : memoryCache.keySet()) {
                Logger.msg(logLevel, "Cached Objects of Item " + itemPath);
                Map<String, C2KLocalObject> sysKeyMemCache = memoryCache
                        .get(itemPath);
                try {
                    synchronized (sysKeyMemCache) {
                        for (Object name : sysKeyMemCache.keySet()) {
                            String path = (String) name;
                            try {
                                Logger.msg(logLevel, "    Path " + path + ": " + sysKeyMemCache.get(path).getClass().getName());
                            }
                            catch (NullPointerException e) {
                                Logger.msg(logLevel, "    Path " + path + ": reaped");
                            }
                        }
                    }
                }
                catch (ConcurrentModificationException ex) {
                    Logger.msg(logLevel, "Cache modified - aborting");
                }
            }
            Logger.msg(logLevel, "Total number of cached entities: "+memoryCache.size());
        }
    }

    public void begin(Object locker) {
        for (TransactionalClusterStorage thisStore : transactionalStores) {
            thisStore.begin(locker);
        }	
    }

    public void commit(Object locker) throws PersistencyException {
        for (TransactionalClusterStorage thisStore : transactionalStores) {
            thisStore.commit(locker);
        }	
    }

    public void abort(Object locker) {
        for (TransactionalClusterStorage thisStore : transactionalStores) {
            thisStore.abort(locker);
        }	
    }
}
