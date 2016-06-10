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
package org.cristalise.kernel.lifecycle.instance.predefined;

//Java
import java.util.Arrays;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


public class ReplaceDomainWorkflow extends PredefinedStep
{
	public ReplaceDomainWorkflow()
	{
		super();
		getProperties().put("Agent Role", "Admin");
	}

	@Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData, Object locker) throws InvalidDataException, PersistencyException {
		
		Workflow lifeCycle = getWf();
 
		String[] params = getDataList(requestData);
        if (Logger.doLog(3)) Logger.msg(3, "AddC2KObject: called by "+agent+" on "+item+" with parameters "+Arrays.toString(params));
        if (params.length != 1) throw new InvalidDataException("AddC2KObject: Invalid parameters "+Arrays.toString(params));

        lifeCycle.getChildrenGraphModel().removeVertex(lifeCycle.search("workflow/domain"));
		CompositeActivity domain;
		try {
			domain = (CompositeActivity) Gateway.getMarshaller().unmarshall(params[0]);
		} catch (Exception e) {
			Logger.error(e);
			throw new InvalidDataException("ReplaceDomainWorkflow: Could not unmarshall new workflow: "+e.getMessage());
		}
		domain.setName("domain");
		lifeCycle.initChild(domain, true, new GraphPoint(150, 100));
		// if new workflow, activate it, otherwise refresh the jobs
		if (!domain.active) lifeCycle.run(agent, item, locker);
		else lifeCycle.refreshJobs(item);
		
		// store new wf
		try {
			Gateway.getStorage().put(item, lifeCycle, locker);
		} catch (PersistencyException e) {
			throw new PersistencyException("ReplaceDomainWorkflow: Could not write new workflow to storage: "+e.getMessage());
		}
		return requestData;
	}
}
