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
/**
 *
 */
package org.cristalise.kernel.utils;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Viewpoint;


public abstract class DescriptionObjectCache<D extends DescriptionObject> {

	SoftCache<String, CacheEntry<D>> cache = new SoftCache<String, CacheEntry<D>>();

	public D get(String name, int version) throws ObjectNotFoundException, InvalidDataException {
		D thisDef;
		synchronized(cache) {
			CacheEntry<D> thisDefEntry = cache.get(name+"_"+version);
			if (thisDefEntry == null) {
				Logger.msg(6, name+" v"+version+" not found in cache. Retrieving.");
		        ItemProxy defItem = LocalObjectLoader.loadLocalObjectDef(getDefRoot(), name);
		        thisDef = loadObject(name, version, defItem);
		        cache.put(name+"_"+version, new CacheEntry<D>(thisDef, defItem, this));
			}
			else {
				Logger.msg(6, name+" v"+version+" found in cache.");
				thisDef = thisDefEntry.def;
			}
		}
        return thisDef;
	}
	
	public abstract String getDefRoot();
	
	public abstract D loadObject(String name, int version, ItemProxy proxy) throws ObjectNotFoundException, InvalidDataException;

	public void removeObject(String id) {
		synchronized(cache) {
		if (cache.keySet().contains(id)) {
			Logger.msg(7, "ActDefCache: Removing activity def "+id+" from cache");
			cache.remove(id);
			}
		}
	}

	public class CacheEntry<E extends DescriptionObject> implements ProxyObserver<Viewpoint> {
		public String id;
		public ItemProxy proxy;
		public E def;
		public DescriptionObjectCache<E> parent;
		public CacheEntry(E def, ItemProxy proxy, DescriptionObjectCache<E> parent) {
			this.id = def.getName()+"_"+def.getVersion();
			this.def = def;
			this.parent = parent;
			this.proxy = proxy;
			proxy.subscribe(new MemberSubscription<Viewpoint>(this, ClusterStorage.VIEWPOINT, false));
		}
		@Override
		public void finalize() {
			parent.removeObject(id);
			proxy.unsubscribe(this);
		}
		@Override
		public void add(Viewpoint contents) {
			parent.removeObject(id);
		}

		@Override
		public void remove(String oldId) {
			parent.removeObject(oldId);
		}

		@Override
		public String toString() {
			return "Cache entry: "+id;
		}
		@Override
		public void control(String control, String msg) {
		}
	}
}