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
/**
 *
 */
package org.cristalise.kernel.utils;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.scripting.Script;


public class ScriptCache extends DescriptionObjectCache<Script> {

	
	@Override
	public String getTypeCode() {
		return "SC";
	}
	
	@Override
	public Script loadObject(String name, int version, ItemProxy proxy) throws ObjectNotFoundException, InvalidDataException {
		Script thisScript;
        Viewpoint scrView = (Viewpoint)proxy.getObject(ClusterStorage.VIEWPOINT + "/Script/" + version);
        String scriptData;
		try {
			scriptData = scrView.getOutcome().getData();
		} catch (PersistencyException ex) {
			Logger.error(ex);
			throw new ObjectNotFoundException("Problem loading Script "+name+" v"+version+": "+ex.getMessage());
		}
		try {
			thisScript = new Script(name, version, proxy.getPath(), scriptData);
		} catch (Exception ex) {
			Logger.error(ex);
			throw new InvalidDataException("Error parsing script '"+name+"' v"+version+": "+ex.getMessage());
		}
        return thisScript;
	}

}