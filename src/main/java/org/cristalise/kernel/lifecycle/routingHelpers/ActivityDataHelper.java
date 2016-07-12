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
package org.cristalise.kernel.lifecycle.routingHelpers;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VIEW_POINT;

import javax.xml.xpath.XPathExpressionException;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

/**
 * Implements the DataHelper to get Outcome data using Activity path and XPath. DataHelpers ares 
 */
public class ActivityDataHelper implements DataHelper {

    public ActivityDataHelper() {}

    /**
     * Retrieves the Workflow of the given Item, searches the Activity using the activity path and
     * retrieves a single value based on XPath
     */
    @Override
	public String get(ItemPath itemPath, String actContext, String dataPath, Object locker)
            throws InvalidDataException, PersistencyException, ObjectNotFoundException
    {
        Logger.msg(5,"ActivityDataHelper.get() - item:"+itemPath+", actContext:"+actContext+", dataPath:"+dataPath);

        String[] paths = dataPath.split(":");

        if (paths.length != 2) throw new InvalidDataException("Invalid path '"+dataPath+"' it must have only one colon (:)");

        String actPath = paths[0];
        String xpath   = paths[1];

        if (actPath.startsWith(".")) {
            actPath = actContext+(actContext.endsWith("/") ? "" : "/")+actPath.substring(2);
        }

        // Find the referenced activity, so get the workflow and search
        Workflow workflow = (Workflow) Gateway.getStorage().get(itemPath, ClusterStorage.LIFECYCLE+"/workflow", locker);
        GraphableVertex act = workflow.search(actPath);

        if (act == null) {
            throw new InvalidDataException("Workflow search failed for actPath:"+actPath+" - item:"+itemPath+", actContext:"+actContext+", dataPath:"+dataPath);
        }

        // Get the schema and viewpoint names
        String schemaName = act.getBuiltInProperty(SCHEMA_NAME).toString();
        Integer schemaVersion = Integer.valueOf(act.getBuiltInProperty(SCHEMA_VERSION).toString());
        Schema schema = LocalObjectLoader.getSchema(schemaName, schemaVersion);
        String viewName   = act.getBuiltInProperty(VIEW_POINT).toString();

        if (viewName == null || viewName.equals("")) viewName = "last";

        // get the viewpoint and outcome
        Viewpoint view = (Viewpoint) Gateway.getStorage().get(itemPath, ClusterStorage.VIEWPOINT+"/"+schema.getName()+"/"+viewName, locker);
        Outcome   oc   = (Outcome)   Gateway.getStorage().get(itemPath, ClusterStorage.OUTCOME+"/"  +schema.getName()+"/"+view.getSchemaVersion()+"/"+view.getEventId(), locker);

        // apply the XPath to its outcome
        try {
            return oc.getFieldByXPath(xpath);
        }
        catch (XPathExpressionException e) {
            throw new InvalidDataException("Invalid xpath:"+xpath+" - item:"+itemPath+", actContext:"+actContext+", dataPath:"+dataPath);
        }
    }
}
