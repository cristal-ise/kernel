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
