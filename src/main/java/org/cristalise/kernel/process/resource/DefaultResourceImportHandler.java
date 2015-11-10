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
package org.cristalise.kernel.process.resource;

import java.util.HashSet;
import java.util.Set;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.utils.LocalObjectLoader;


public class DefaultResourceImportHandler implements ResourceImportHandler {

	String schemaName;
	String typeRoot;
	DomainPath typeRootPath;
	String wfDef;
	PropertyDescriptionList props;
	
	public DefaultResourceImportHandler(String resType) throws Exception {
    	if (resType.equals("CA")) {
    		schemaName = "CompositeActivityDef";
    		typeRoot = "/desc/ActivityDesc";
    		wfDef = "ManageCompositeActDef";
    	}
    	else if (resType.equals("EA")) {
    		schemaName = "ElementaryActivityDef";
    		typeRoot = "/desc/ActivityDesc";
    		wfDef = "ManageElementaryActDef";
    	}
    	else if (resType.equals("OD")) {
    		schemaName = "Schema";
    		typeRoot = "/desc/OutcomeDesc";
    		wfDef = "ManageSchema";
    	}
    	else if (resType.equals("SC")) {
    		schemaName = "Script";
    		typeRoot = "/desc/Script";
    		wfDef = "ManageScript";
    	}
    	else if (resType.equals("SM")) {
    		schemaName = "StateMachine";
    		typeRoot = "/desc/StateMachine";
    		wfDef = "ManageStateMachine";
    	}
    	else throw new Exception("Unknown bootstrap item type: "+resType);
    	typeRootPath = new DomainPath(typeRoot);
		props = (PropertyDescriptionList)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, "boot/property/"+resType+"Prop.xml"));
	}
	
	@Override
	public CollectionArrayList getCollections(String resType, String ns, String location, Integer version) throws Exception {
		
		if (resType.equals("CA") || resType.equals("EA")) {
			String actData = Gateway.getResource().getTextResource(ns, location);
			ActivityDef actDef = (ActivityDef)Gateway.getMarshaller().unmarshall(actData);
			actDef.setVersion(version);
			return actDef.makeDescCollections();
		}
		else
			return new CollectionArrayList();
	}

	@Override
	public DomainPath getTypeRoot() {
		return typeRootPath;
	}
	
	@Override
	public String getName() {
		return schemaName;
	}

	@Override
	public DomainPath getPath(String name, String ns) throws Exception {
		return new DomainPath(typeRoot+"/system/"+(ns==null?"kernel":ns)+'/'+name);
	}

	@Override
	public Set<Outcome> getResourceOutcomes(String name, String ns, String location, Integer version) throws Exception {
		HashSet<Outcome> retArr = new HashSet<Outcome>();
		String data = Gateway.getResource().getTextResource(ns, location);
        if (data == null)
            throw new Exception("No data found for "+schemaName+" "+name);
		Outcome resOutcome = new Outcome(0, data, LocalObjectLoader.getSchema(schemaName, 0));
		retArr.add(resOutcome);
		return retArr;
	}
	
    @Override
    public PropertyDescriptionList getPropDesc() throws Exception {
    	return props;
	}

	@Override
	public String getWorkflowName() throws Exception {
		return wfDef;
	}
}
