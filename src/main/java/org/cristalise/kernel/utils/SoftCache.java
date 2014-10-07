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
package org.cristalise.kernel.utils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/*******************************************************************************
 * SoftReferences are reaped if no strong references are left and the vm is
 * running out of memory. Most caches in the kernel use this.
 *
 * $Revision: 1.5 $ $Date: 2004/10/29 13:29:09 $
 ******************************************************************************/
public class SoftCache<K, V> extends AbstractMap<K, V> {

    private final Map<K, SoftValue<V>> hash = new HashMap<K, SoftValue<V>>();
    private final int minSize;
    private final LinkedList<V> hardCache = new LinkedList<V>();
    private final ReferenceQueue<V> queue = new ReferenceQueue<V>();

    public SoftCache() {
        this(0);
    }

    public SoftCache(int minSize) {
        this.minSize = minSize;
    }

    @Override
    public V get(Object key) {
        V result = null;
        SoftValue<V> soft_ref = hash.get(key);
        if (soft_ref != null) {
            result = soft_ref.get();
            if (result == null)
                hash.remove(key);
            else
                if (minSize > 0) { // add to hard cache so it's not reaped for a while
                	synchronized(hardCache) {
                		hardCache.addFirst(result);
                		if (hardCache.size() > minSize) // trim last one off
                			hardCache.removeLast();
                	}
                }
        }
        return result;
    }

    @Override
	public V put(K key, V value) {
        processQueue();
        if (minSize > 0) {
        	synchronized(hardCache) {
        		hardCache.addFirst(value);
        		if (hardCache.size() > minSize)
        			hardCache.removeLast();
        	}
        }
        hash.put(key, new SoftValue<V>(key, value, queue));
        return value;
    }

    @Override
	public V remove(Object key) {
        processQueue();
        if (hash.containsKey(key)) return hash.remove(key).get();
        return null;
    }

    @Override
	public void clear() {
    	synchronized(hardCache) {
    		hardCache.clear();
    	}
        while(queue.poll()!=null);
        hash.clear();
    }

    @Override
	public int size() {
        processQueue();
        return hash.size();
    }

    @Override
	public Set<K> keySet() {
        processQueue();
        return hash.keySet();
    }

    @Override
	public Set<Map.Entry<K, V>> entrySet() {
        // Would have to create another Map to do this - too expensive
        // Throwing runtime expensive is dangerous, but better than nulls
        throw new UnsupportedOperationException();
    }

    private static class SoftValue<V> extends SoftReference<V> {
        private final Object key;
        private SoftValue(Object key, V value, ReferenceQueue<V> q) {
            super(value, q);
            this.key = key;
        }
    }

    /**
     * Look for values that have been reaped, and remove their keys from the cache
     */
    private void processQueue() {
        SoftValue<V> sv;
        while ((sv = (SoftValue<V>) queue.poll()) != null) {
            hash.remove(sv.key);
        }
    }

}
