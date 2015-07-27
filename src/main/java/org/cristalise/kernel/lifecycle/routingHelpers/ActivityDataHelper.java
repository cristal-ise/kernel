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

public class ActivityDataHelper implements DataHelper {

	@Override
	public String get(ItemPath item, String dataPath, Object locker)
			throws InvalidDataException, PersistencyException,
			ObjectNotFoundException {
		//Syntax of search : <ActivityPath>:<XPathinOutcome>
		Workflow wf = (Workflow) Gateway.getStorage().get(item, ClusterStorage.LIFECYCLE, locker);
		String[] paths = dataPath.split(":");
		// Find the referenced activity
		GraphableVertex act = wf.search(paths[0]);
		// Get the schema and viewpoint names
		String schemaName = act.getProperties().get("SchemaType").toString();
		String viewName = act.getProperties().get("Viewpoint").toString();
		// get the viewpoint
		Viewpoint view = (Viewpoint) Gateway.getStorage().get(item, ClusterStorage.VIEWPOINT+"/"+schemaName+"/"+viewName, locker);
		// apply the XPath to its outcome
		Outcome oc = (Outcome)Gateway.getStorage().get(item, ClusterStorage.OUTCOME+"/"+schemaName+"/"+view.getSchemaVersion()+"/"+view.getEventId(), locker);
		try {
			return oc.getFieldByXPath(paths[1]);
		} catch (XPathExpressionException e) {
			throw new InvalidDataException("Invalid XPath: "+paths[1]);
		}
	}

}
