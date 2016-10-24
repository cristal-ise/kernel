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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;
import static org.cristalise.kernel.process.resource.BuiltInResources.ACTIVITY_DESC_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.COMP_ACT_DESC_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.ELEM_ACT_DESC_RESOURCE;
import static org.cristalise.kernel.property.BuiltInItemProperties.COMPLEXITY;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;

public class ActDefCache extends DescriptionObjectCache<ActivityDef> {

    Boolean isComposite = null;

    public ActDefCache(Boolean isComposite) {
        super();
        this.isComposite = isComposite;
    }

    @Override
    public String getTypeCode() {
        if (isComposite == null) return ACTIVITY_DESC_RESOURCE.getTypeCode();
        return isComposite ? COMP_ACT_DESC_RESOURCE.getTypeCode() : ELEM_ACT_DESC_RESOURCE.getTypeCode();
    }

    @Override
    public String getSchemaName() {
        if (isComposite == null)
            return ACTIVITY_DESC_RESOURCE.getSchemaName(); // this won't work for resource loads, but loadObject is overridden below
        else 
            return isComposite ? COMP_ACT_DESC_RESOURCE.getSchemaName() : ELEM_ACT_DESC_RESOURCE.getSchemaName();
    }

    @Override
    protected boolean isBootResource(String filename, String resName) {
        if (isComposite == null)
            return filename.endsWith("/" + resName) && (filename.startsWith("CA") || filename.startsWith("EA"));
        else
            return super.isBootResource(filename, resName);
    }

    @Override
    public ActivityDef loadObject(String name, int version, ItemProxy proxy) throws ObjectNotFoundException, InvalidDataException {
        String viewName;

        if (isComposite == null) {
            String prop = proxy.getProperty(COMPLEXITY);

            if(     "Composite".equals(prop))  viewName = COMP_ACT_DESC_RESOURCE.getSchemaName();
            else if("Elementary".equals(prop)) viewName = ELEM_ACT_DESC_RESOURCE.getSchemaName();
            else                               throw new InvalidDataException("Missing Item property:" + COMPLEXITY);
        }
        else {
            viewName = isComposite ? COMP_ACT_DESC_RESOURCE.getSchemaName() : ELEM_ACT_DESC_RESOURCE.getSchemaName();
        }

        try {
            Viewpoint actView = (Viewpoint) proxy.getObject(ClusterStorage.VIEWPOINT + "/" + viewName + "/" + version);
            String marshalledAct = actView.getOutcome().getData();
            return buildObject(name, version, proxy.getPath(), marshalledAct);
        }
        catch (PersistencyException ex) {
            Logger.error(ex);
            throw new ObjectNotFoundException("Problem loading Activity " + name + " v" + version + ": " + ex.getMessage());
        }
    }

    @Override
    public ActivityDef buildObject(String name, int version, ItemPath path, String data) throws InvalidDataException {
        try {
            ActivityDef thisActDef = (ActivityDef) Gateway.getMarshaller().unmarshall(data);
            thisActDef.setBuiltInProperty(VERSION, version);
            thisActDef.setName(name);
            thisActDef.setVersion(version);
            thisActDef.setItemPath(path);
            return thisActDef;
        }
        catch (Exception ex) {
            Logger.error(ex);
            throw new InvalidDataException("Could not unmarshall Activity '" + name + "' v" + version + ": " + ex.getMessage());
        }
    }
}