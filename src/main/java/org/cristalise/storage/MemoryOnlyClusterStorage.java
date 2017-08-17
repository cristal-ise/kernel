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
package org.cristalise.storage;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.Logger;


public class MemoryOnlyClusterStorage extends ClusterStorage {

    HashMap<ItemPath, Map<String, C2KLocalObject>> memoryCache = new HashMap<ItemPath, Map<String, C2KLocalObject>>();

    public void clear() {
        memoryCache.clear();
    }
    /**
     * 
     */
    public MemoryOnlyClusterStorage() {
        memoryCache = new HashMap<ItemPath, Map<String,C2KLocalObject>>();
    }

    @Override
    public void open(Authenticator auth) throws PersistencyException {

    }

    @Override
    public void close() throws PersistencyException {
    }

    @Override
    public boolean checkQuerySupport(String language) {
        Logger.warning("MemoryOnlyClusterStorage DOES NOT Support any query");
        return false;
    }

    @Override
    public short queryClusterSupport(String clusterType) {
        return ClusterStorage.READWRITE;
    }

    @Override
    public String getName() {
        return "Memory Cache";
    }

    @Override
    public String getId() {
        return "Memory Cache";
    }

    @Override
    public String executeQuery(Query query) throws PersistencyException {
        throw new PersistencyException("UNIMPLEMENTED funnction");
    }

    @Override
    public C2KLocalObject get(ItemPath thisItem, String path)
            throws PersistencyException
    {
        Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(thisItem);
    
        if (sysKeyMemCache != null) return sysKeyMemCache.get(path);

        return null;
    }

    @Override
    public void put(ItemPath thisItem, C2KLocalObject obj) throws PersistencyException {
        // create item cache if not present
        Map<String, C2KLocalObject> sysKeyMemCache;
        synchronized (memoryCache) {
            if (memoryCache.containsKey(thisItem))
                sysKeyMemCache = memoryCache.get(thisItem);
            else {
                sysKeyMemCache = new HashMap<String, C2KLocalObject>();
                memoryCache.put(thisItem, sysKeyMemCache);
            }
        }

        // store object in the cache
        String path = ClusterStorage.getPath(obj);
        synchronized(sysKeyMemCache) {
            sysKeyMemCache.put(path, obj);
        }

    }

    @Override
    public void delete(ItemPath thisItem, String path) throws PersistencyException {
        Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(thisItem);
        if (sysKeyMemCache != null) {
            synchronized (sysKeyMemCache) {
                if (sysKeyMemCache.containsKey(path)) {
                    sysKeyMemCache.remove(path);
                    if (sysKeyMemCache.isEmpty()) {
                        synchronized (memoryCache) {
                            memoryCache.remove(thisItem);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String[] getClusterContents(ItemPath thisItem, String path) throws PersistencyException {
        Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(thisItem);
        ArrayList<String> result = new ArrayList<String>();
        if (sysKeyMemCache != null) {
            while (path.endsWith("/")) 
                path = path.substring(0,path.length()-1);
            path = path+"/";
            for (String thisPath : sysKeyMemCache.keySet()) {
                if (thisPath.startsWith(path)) {
                    String end = thisPath.substring(path.length());
                    int slash = end.indexOf('/');
                    String suffix = slash>-1?end.substring(0, slash):end;
                    if (!result.contains(suffix)) result.add(suffix);
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    public String[] getClusterContents(ItemPath itemPath, ClusterType type) throws PersistencyException {
        return getClusterContents(itemPath, type.getName());
    }

    public void dumpContents(ItemPath thisItem) {
        synchronized(memoryCache) {
            Logger.msg(0, "Cached Objects of Entity "+thisItem);
            Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(thisItem);
            if (sysKeyMemCache == null) {
                Logger.msg(0, "No cache found");
                return;
            }
            try {
                synchronized(sysKeyMemCache) {
                    for (Object name : sysKeyMemCache.keySet()) {
                        String path = (String) name;
                        try {
                            Logger.msg(0, "    Path "+path+": "+sysKeyMemCache.get(path).getClass().getName());
                        } catch (NullPointerException e) {
                            Logger.msg(0, "    Path "+path+": reaped");
                        }
                    }
                }
            } catch (ConcurrentModificationException ex) {
                Logger.msg(0, "Cache modified - aborting");
            }
        }
        Logger.msg(0, "Total number of cached entities: "+memoryCache.size());
    }
}
