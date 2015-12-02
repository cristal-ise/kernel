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
import java.lang.ref.Reference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
/**************************************************************************
 * TransientCache - Uses transient references to allow unused entries to be
 * reaped by the java garbage collector.
 *
 * $Revision: 1.1 $
 * $Date: 2004/04/20 09:37:02 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/
public abstract class TransientCache<K, V> extends AbstractMap<K, V> {

    private Map<K, Reference<V>> map = new Hashtable<K, Reference<V>>();

    @Override
	public synchronized Set<Entry<K, V>> entrySet() {
        Map<K, V> newMap = new Hashtable<K,V>();
        Iterator<Entry<K, Reference<V>>> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
        	Entry<K, Reference<V>> me = iter.next();
            Reference<V> ref = me.getValue();
            V o = ref.get();
            if (o == null) {
                // Delete cleared reference
                iter.remove();
            } else {
                // Copy out interior object
                newMap.put(me.getKey(), o);
            }
        }
        // Return set of interior objects
        return newMap.entrySet();
    }

    @Override
	public synchronized V put(K key, V value) {
        Reference<V> ref = makeReference(value);
        ref = map.put(key, ref);
        if (ref != null)
            return (ref.get());
        return null;
    }

    public abstract Reference<V> makeReference(Object value);

    @Override
	public V remove(Object key) {
        Iterator<Entry<K, Reference<V>>> i = map.entrySet().iterator();
        Entry<K, Reference<V>> correctEntry = null;
        if (key == null) {
            while (correctEntry == null && i.hasNext()) {
                Entry<K, Reference<V>> e = i.next();
                if (e.getKey() == null)
                    correctEntry = e;
            }
        } else {
            while (correctEntry == null && i.hasNext()) {
            	Entry<K, Reference<V>> e = i.next();
                if (key.equals(e.getKey()))
                    correctEntry = e;
            }
        }
        V oldValue = null;
        if (correctEntry != null) {
            Reference<V> correctReference = correctEntry.getValue();
            oldValue = correctReference.get();
            i.remove();
        }
        return oldValue;
    }
    /**
     *
     */
    @Override
	public void clear() {
        map.entrySet().clear();
    }

    private transient Set<K> keySet = null;

    @Override
	public Set<K> keySet() {
        if (keySet == null) {
            keySet = new AbstractSet<K>() {
            @Override
			public Iterator<K> iterator() {
                return new Iterator<K>() {
                private Iterator<Entry<K, Reference<V>>> i = map.entrySet().iterator();

                @Override
				public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
				public K next() {
                    return i.next().getKey();
                }

                @Override
				public void remove() {
                    i.remove();
                }
                        };
            }

            @Override
			public int size() {
                return TransientCache.this.size();
            }

            @Override
			public boolean contains(Object k) {
                return TransientCache.this.containsKey(k);
            }
            };
        }
        return keySet;
        }

}
