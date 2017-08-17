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
package org.cristalise.kernel.entity.transfer;

import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.TraceableEntity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;

import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;
import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;

public class TransferItem {
    private ArrayList<String> domainPaths;
    protected ItemPath        itemPath;
    static AgentPath          importAgentId;

    public TransferItem() throws Exception {
        try {
            importAgentId = Gateway.getLookup().getAgentPath("system");
        }
        catch (ObjectNotFoundException e) {
            Logger.error("TransferItem - System agent not found!");
            throw e;
        }
    }

    public TransferItem(ItemPath itemPath) throws Exception {
        this.itemPath = itemPath;
        domainPaths = new ArrayList<String>();
        Iterator<Path> paths = Gateway.getLookup().searchAliases(itemPath);
        while (paths.hasNext()) {
            DomainPath thisPath = (DomainPath) paths.next();
            domainPaths.add(thisPath.toString());
        }
    }

    public ArrayList<String> getDomainPaths() {
        return domainPaths;
    }

    public void setDomainPaths(ArrayList<String> domainPaths) {
        this.domainPaths = domainPaths;
    }

    public void setUUID(String uuid) throws InvalidItemPathException {
        itemPath = new ItemPath(uuid);
    }

    public String getUUID() {
        return itemPath.getUUID().toString();
    }

    public void exportItem(File dir, String path) throws Exception {
        Logger.msg("Path " + path + " in " + itemPath);
        String[] contents = Gateway.getStorage().getClusterContents(itemPath, path);
        if (contents.length > 0) {
            FileStringUtility.createNewDir(dir.getCanonicalPath());
            for (String content : contents) {
                exportItem(new File(dir, content), path + "/" + content);
            }
        }
        else { // no children, try to dump object
            try {
                C2KLocalObject obj = Gateway.getStorage().get(itemPath, path, null);
                Logger.msg("Dumping object " + path + " in " + itemPath);
                File dumpPath = new File(dir.getCanonicalPath() + ".xml");
                FileStringUtility.string2File(dumpPath, Gateway.getMarshaller().marshall(obj));
                return;
            }
            catch (ObjectNotFoundException ex) {} // not an object
        }
    }

    public void importItem(File dir) throws Exception {
        // check if already exists
        try {
            Property name = (Property) Gateway.getStorage().get(itemPath, PROPERTY + "/" + NAME, null);
            throw new Exception("Item " + itemPath + " already in use as " + name.getValue());
        }
        catch (Exception ex) {}

        // retrieve objects
        ArrayList<String> objectFiles = FileStringUtility.listDir(dir.getCanonicalPath(), false, true);
        ArrayList<C2KLocalObject> objects = new ArrayList<C2KLocalObject>();
        for (String element : objectFiles) {
            String xmlFile = FileStringUtility.file2String(element);
            C2KLocalObject newObj;
            String choppedPath = element.substring(dir.getCanonicalPath().length() + 1, element.length() - 4);

            Logger.msg(choppedPath);

            if (choppedPath.startsWith(OUTCOME.getName())) newObj = new Outcome(choppedPath, xmlFile);
            else                                                newObj = (C2KLocalObject) Gateway.getMarshaller().unmarshall(xmlFile);

            objects.add(newObj);
        }

        // create item
        TraceableEntity newItem = Gateway.getCorbaServer().createItem(itemPath);
        Gateway.getLookupManager().add(itemPath);

        PropertyArrayList props = new PropertyArrayList();
        CollectionArrayList colls = new CollectionArrayList();
        Workflow wf = null;
        // put objects
        for (C2KLocalObject obj : objects) {
            if (obj instanceof Property)        props.list.add((Property) obj);
            else if (obj instanceof Collection) colls.list.add((Collection<?>) obj);
            else if (obj instanceof Workflow)   wf = (Workflow) obj;
        }

        if (wf == null) throw new Exception("No workflow found in import for " + itemPath);

        // init item
        newItem.initialise(importAgentId.getSystemKey(),
                           Gateway.getMarshaller().marshall(props),
                           Gateway.getMarshaller().marshall(wf.search("workflow/domain")),
                           Gateway.getMarshaller().marshall(colls));

        // store objects
        importByType(ClusterType.HISTORY, objects);
        importByType(ClusterType.OUTCOME, objects);
        importByType(ClusterType.VIEWPOINT, objects);
        Gateway.getStorage().commit(this);

        // add domPaths
        for (String element : domainPaths) {
            DomainPath newPath = new DomainPath(element, itemPath);
            Gateway.getLookupManager().add(newPath);
        }
    }

    private void importByType(ClusterType type, ArrayList<C2KLocalObject> objects) throws Exception {
        for (C2KLocalObject element : objects) {
            if (element.getClusterType().equals(type)) Gateway.getStorage().put(itemPath, element, this);
        }

    }
}