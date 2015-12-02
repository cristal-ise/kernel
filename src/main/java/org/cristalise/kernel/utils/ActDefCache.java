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

	Boolean isComposite;
	public ActDefCache(Boolean isComposite) {
		super();
		this.isComposite = isComposite;
	}

	@Override
	public String getTypeCode() {
		if (isComposite == null) return "AC";
		return isComposite?"CA":"EA";
	}
	
	@Override
	public String getSchemaName() {
		if (isComposite == null) return "ActivityDef"; // this won't work for resource loads, but loadObject is overridden below 
		return isComposite?"CompositeActivityDef":"ActivityDef";
	}
	
	@Override
	protected boolean isBootResource(String filename, String resName) {
		if (isComposite==null)
			return filename.endsWith("/"+resName) && (filename.startsWith("CA") || filename.startsWith("EA"));
		else
			return super.isBootResource(filename, resName);
	}

	@Override
	public ActivityDef loadObject(String name, int version, ItemProxy proxy) throws ObjectNotFoundException, InvalidDataException {
		String actType;
		if (isComposite == null)
			actType = proxy.getProperty("Complexity");
		else 
			actType= isComposite?"Composite":"Elementary";
        Viewpoint actView = (Viewpoint)proxy.getObject(ClusterStorage.VIEWPOINT + "/" + actType + "ActivityDef/" + version);
        String marshalledAct;
		try {
			marshalledAct = actView.getOutcome().getData();
		} catch (PersistencyException ex) {
			Logger.error(ex);
			throw new ObjectNotFoundException("Problem loading "+name+" v"+version+": "+ex.getMessage());
		}
		return buildObject(name, version, proxy.getPath(), marshalledAct);
	}

	@Override
	public ActivityDef buildObject(String name, int version, ItemPath path, String data) throws InvalidDataException {
		try {
			ActivityDef thisActDef = (ActivityDef)Gateway.getMarshaller().unmarshall(data);
			thisActDef.getProperties().put("Version", version);
	        thisActDef.setName(name);
	        thisActDef.setVersion(version);
	        thisActDef.setItemPath(path);
	        return thisActDef;
		} catch (Exception ex) {
			Logger.error(ex);
			throw new InvalidDataException("Could not unmarshall '"+name+"' v"+version+": "+ex.getMessage());
		}
	}
}