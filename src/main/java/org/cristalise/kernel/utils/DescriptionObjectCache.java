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
/**
 *
 */
package org.cristalise.kernel.utils;

import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyDescription;
import org.cristalise.kernel.property.PropertyDescriptionList;

public abstract class DescriptionObjectCache<D extends DescriptionObject> {

    SoftCache<String, CacheEntry<D>> cache = new SoftCache<String, CacheEntry<D>>();
    Property[]                       classIdProps;

    public DescriptionObjectCache() {
        try {
            String propDescXML = Gateway.getResource().findTextResource("boot/property/" + getTypeCode() + "Prop.xml");
            PropertyDescriptionList propDescs = (PropertyDescriptionList) Gateway.getMarshaller().unmarshall(propDescXML);
            ArrayList<Property> classIdPropList = new ArrayList<Property>();
            for (PropertyDescription propDesc : propDescs.list) {
                if (propDesc.getIsClassIdentifier()) classIdPropList.add(propDesc.getProperty());
            }
            classIdProps = classIdPropList.toArray(new Property[classIdPropList.size()]);
        }
        catch (Exception ex) {
            Logger.error(ex);
            Logger.error("Could not load property description for " + getTypeCode() + ". Cannot filter.");
            classIdProps = new Property[0];
        }
    }

    public D loadObjectFromBootstrap(String name) throws InvalidDataException, ObjectNotFoundException {
        Logger.msg(3, "DescriptionObjectCache.loadObjectFromBootstrap() - name:" + name);

        try {
            String bootItems = FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/allbootitems.txt"));
            StringTokenizer str = new StringTokenizer(bootItems, "\n\r");
            while (str.hasMoreTokens()) {
                String resLine = str.nextToken();
                String[] resElem = resLine.split(",");
                if (resElem[0].equals(name) || isBootResource(resElem[1], name)) {
                    Logger.msg(3, "DescriptionObjectCache.loadObjectFromBootstrap() - Shimming " + getTypeCode() + " " + name + " from bootstrap");
                    String resData = Gateway.getResource().getTextResource(null, "boot/" + resElem[1] + (resElem[1].startsWith("OD") ? ".xsd" : ".xml"));
                    return buildObject(name, 0, new ItemPath(resElem[0]), resData);
                }
            }
        }
        catch (Exception e) {
            Logger.error(e);
            throw new InvalidDataException("Error finding bootstrap resources");
        }
        throw new ObjectNotFoundException("Resource " + getSchemaName() + " " + name + " not found in bootstrap resources");
    }

    protected boolean isBootResource(String filename, String resName) {
        return filename.equals(getTypeCode() + "/" + resName);
    }

    public ItemPath findItem(String name) throws ObjectNotFoundException, InvalidDataException {
        if (Gateway.getLookup() == null) throw new ObjectNotFoundException("Cannot find Items without a Lookup");

        // first check for a UUID name
        try {
            ItemPath resItem = new ItemPath(name);
            if (resItem.exists()) return resItem;
        }
        catch (InvalidItemPathException ex) {}

        // then check for a direct path
        DomainPath directPath = new DomainPath(name);
        if (directPath.exists() && directPath.getItemPath() != null) { return directPath.getItemPath(); }

        // else search for it in the whole tree using property description
        Property[] searchProps = new Property[classIdProps.length + 1];
        searchProps[0] = new Property(NAME, name);
        System.arraycopy(classIdProps, 0, searchProps, 1, classIdProps.length);

        Iterator<Path> e = Gateway.getLookup().search(new DomainPath(), searchProps);
        if (e.hasNext()) {
            Path defPath = e.next();
            if (e.hasNext()) throw new ObjectNotFoundException("Too many matches for " + getTypeCode() + " " + name);

            if (defPath.getItemPath() == null)
                throw new InvalidDataException(getTypeCode() + " " + name + " was found, but was not an Item");

            return defPath.getItemPath();
        }
        else {
            throw new ObjectNotFoundException("No match for " + getTypeCode() + " " + name);
        }
    }

    public D get(String name, int version) throws ObjectNotFoundException, InvalidDataException {
        D thisDef = null;
        synchronized (cache) {
            CacheEntry<D> thisDefEntry = cache.get(name + "_" + version);
            if (thisDefEntry == null) {
                Logger.msg(6, "DescriptionObjectCache.get() - " + name + " v" + version + " not found in cache. Checking id.");
                try {
                    ItemPath defItemPath = findItem(name);
                    String defId = defItemPath.getUUID().toString();
                    thisDefEntry = cache.get(defId + "_" + version);
                    if (thisDefEntry == null) {
                        Logger.msg(6, "DescriptionObjectCache.get() - " + name + " v" + version
                                + " not found in cache. Loading from database.");
                        ItemProxy defItemProxy = Gateway.getProxyManager().getProxy(defItemPath);
                        if (name.equals(defId)) {
                            String itemName = defItemProxy.getName();
                            if (itemName != null) name = itemName;
                        }
                        thisDef = loadObject(name, version, defItemProxy);
                        cache.put(defId + "_" + version, new CacheEntry<D>(thisDef, defItemProxy, this));
                    }
                }
                catch (ObjectNotFoundException ex) {
                    // for bootstrap and testing, try to load built-in kernel objects from resources
                    if (version == 0) {
                        try {
                            return loadObjectFromBootstrap(name);
                        }
                        catch (ObjectNotFoundException ex2) {}
                    }
                    throw ex;
                }
            }
            if (thisDefEntry != null && thisDef == null) {
                Logger.msg(6, "DescriptionObjectCache.get() - " + name + " v" + version + " found in cache.");
                thisDef = thisDefEntry.def;
            }
        }
        return thisDef;
    }

    public abstract String getTypeCode();

    public abstract String getSchemaName();

    public abstract D buildObject(String name, int version, ItemPath path, String data) throws InvalidDataException;

    public D loadObject(String name, int version, ItemProxy proxy) throws ObjectNotFoundException, InvalidDataException {

        Viewpoint smView = (Viewpoint) proxy.getObject(ClusterType.VIEWPOINT + "/" + getSchemaName() + "/" + version);
        String rawRes;
        try {
            rawRes = smView.getOutcome().getData();
        }
        catch (PersistencyException ex) {
            Logger.error(ex);
            throw new ObjectNotFoundException("Problem loading " + getSchemaName() + " " + name + " v" + version + ": " + ex.getMessage());
        }
        return buildObject(name, version, proxy.getPath(), rawRes);
    }

    public void removeObject(String id) {
        synchronized (cache) {
            if (cache.keySet().contains(id)) {
                Logger.msg(7, "DescriptionObjectCache.remove() - activityDef:" + id);
                cache.remove(id);
            }
        }
    }

    public class CacheEntry<E extends DescriptionObject> implements ProxyObserver<Viewpoint> {
        public String                    id;
        public ItemProxy                 proxy;
        public E                         def;
        public DescriptionObjectCache<E> parent;

        public CacheEntry(E def, ItemProxy proxy, DescriptionObjectCache<E> parent) {
            this.id = def.getItemID() + "_" + def.getVersion();
            this.def = def;
            this.parent = parent;
            this.proxy = proxy;
            proxy.subscribe(new MemberSubscription<Viewpoint>(this, ClusterType.VIEWPOINT.getName(), false));
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
            parent.removeObject(id);
        }

        @Override
        public String toString() {
            return "Cache entry: " + id;
        }

        @Override
        public void control(String control, String msg) {
        }
    }
}