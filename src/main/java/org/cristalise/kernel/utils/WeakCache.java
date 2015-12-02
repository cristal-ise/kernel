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
package org.cristalise.kernel.utils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/*******************************************************************************
 * WeakReferences are reaped if no strong references are left next time the gc has a chance. 
 * The ClusterStorageManager caches can optionally use this one, for high volume imports etc
 *
 * $Revision: 1.5 $ $Date: 2004/10/29 13:29:09 $
 ******************************************************************************/
public class WeakCache<K, V> extends AbstractMap<K, V> {

    private final Map<K, WeakValue<V>> hash = new HashMap<K, WeakValue<V>>();
    private final int minSize;
    private final LinkedList<V> hardCache = new LinkedList<V>();
    private final ReferenceQueue<V> queue = new ReferenceQueue<V>();

    public WeakCache() {
        this(0);
    }

    public WeakCache(int minSize) {
        this.minSize = minSize;
    }

    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.NonStrongRefCache#get(java.lang.Object)
	 */
	@Override
    public V get(Object key) {
        V result = null;
        WeakValue<V> weak_ref = hash.get(key);
        if (weak_ref != null) {
            result = weak_ref.get();
            if (result == null)
                hash.remove(key);
            else
                if (minSize > 0) { // add to hard cache so it's not reaped for a while
                    hardCache.addFirst(result);
                    if (hardCache.size() > minSize) // trim last one off
                        hardCache.removeLast();
                }
        }
        return result;
    }

    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.NonStrongRefCache#put(K, V)
	 */
	@Override
	public V put(K key, V value) {
        processQueue();
        if (minSize > 0) {
            hardCache.addFirst(value);
            if (hardCache.size() > minSize)
                hardCache.removeLast();
        }
        hash.put(key, new WeakValue<V>(key, value, queue));
        return value;
    }

    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.NonStrongRefCache#remove(java.lang.Object)
	 */
	@Override
	public V remove(Object key) {
        processQueue();
        if (hash.containsKey(key)) return hash.remove(key).get();
        return null;
    }

    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.NonStrongRefCache#clear()
	 */
	@Override
	public void clear() {
        hardCache.clear();
        while(queue.poll()!=null);
        hash.clear();
    }

    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.NonStrongRefCache#size()
	 */
    @Override
	public int size() {
        processQueue();
        return hash.size();
    }

    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.NonStrongRefCache#keySet()
	 */
    @Override
	public Set<K> keySet() {
        processQueue();
        return hash.keySet();
    }

    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.NonStrongRefCache#entrySet()
	 */
    @Override
	public Set<Map.Entry<K, V>> entrySet() {
        // Would have to create another Map to do this - too expensive
        // Throwing runtime expensive is dangerous, but better than nulls
        throw new UnsupportedOperationException();
    }

    private static class WeakValue<V> extends WeakReference<V> {
        private final Object key;
        private WeakValue(Object key, V value, ReferenceQueue<V> q) {
            super(value, q);
            this.key = key;
        }
    }

    /**
     * Look for values that have been reaped, and remove their keys from the cache
     */
    private void processQueue() {
    	WeakValue<V> sv;
        while ((sv = (WeakValue<V>) queue.poll()) != null) {
            hash.remove(sv.key);
        }
    }

}
