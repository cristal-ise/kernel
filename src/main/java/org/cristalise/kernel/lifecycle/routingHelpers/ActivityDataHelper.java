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
package org.cristalise.kernel.lifecycle.routingHelpers;

import javax.xml.xpath.XPathExpressionException;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;

/**
 * Implements the Routing DataHelper to get Outcome data using Activity path and XPath. 
 */
public class ActivityDataHelper implements DataHelper {

    Workflow workflow = null;

    public ActivityDataHelper() {}

    public ActivityDataHelper(Workflow wf) { workflow = wf; }

    /**
     * Retrieves the Workflow of the given Item, searches the Activity using the activity path and
     * retrieves a single value based on XPath
     * 
     * @param item the current item to be used
     * @param dataPath syntax is <pre><ActivityPath>:<XPathinOutcome></pre> e.g. workflow/domain/first:/testdata/counter.
     *                 XPath must select a single node.
     * @param locker the transaction locker object
     * @return
     * @throws InvalidDataException dataPath has incorrect syntax
     * @throws PersistencyException 
     * @throws ObjectNotFoundException item or its data cannot be found in storage 
     */
    @Override
	public String get(ItemPath item, String dataPath, Object locker)
            throws InvalidDataException, PersistencyException, ObjectNotFoundException
    {
        if(workflow == null) workflow = (Workflow) Gateway.getStorage().get(item, ClusterStorage.LIFECYCLE, locker);

        String[] paths = dataPath.split(":");

        if (paths.length != 2) throw new InvalidDataException("Invalid path '"+dataPath+"' it must have only one colon (:)");

        String actPath = paths[0];
        String xpath   = paths[1];

        // Find the referenced activity
        GraphableVertex act = workflow.search(actPath);

        // Get the schema and viewpoint names
        String schemaName = act.getProperties().get("SchemaType").toString();
        String viewName   = act.getProperties().get("Viewpoint").toString();

        // get the viewpoint and outcome
        Viewpoint view = (Viewpoint) Gateway.getStorage().get(item, ClusterStorage.VIEWPOINT+"/"+schemaName+"/"+viewName, locker);
        Outcome oc = (Outcome)Gateway.getStorage().get(item, ClusterStorage.OUTCOME+"/"+schemaName+"/"+view.getSchemaVersion()+"/"+view.getEventId(), locker);

        // apply the XPath to its outcome
        try {
            return oc.getFieldByXPath(xpath);
        }
        catch (XPathExpressionException e) {
            throw new InvalidDataException("Invalid XPath: "+paths[1]);
        }
    }

}
