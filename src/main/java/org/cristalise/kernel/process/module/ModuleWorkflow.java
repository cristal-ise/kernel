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
package org.cristalise.kernel.process.module;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.Version;

import java.util.ArrayList;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;

public class ModuleWorkflow extends ModuleActivity {

	ArrayList<ModuleDescRef> activities = new ArrayList<ModuleDescRef>();

	public ModuleWorkflow() {
		super();
		resourceType="CA";
	}
	
	public ModuleWorkflow(ItemProxy child, Integer version) throws ObjectNotFoundException, InvalidDataException {
		super(child, version);
		Collection<?> coll = child.getCollection(CompositeActivityDef.ACTCOL, version);
		for (CollectionMember collMem : coll.getMembers().list) {
			activities.add(new ModuleDescRef(null, collMem.getChildUUID(), Integer.valueOf(collMem.getProperties().get(Version.name()).toString())));			
		}
	}

	public ArrayList<ModuleDescRef> getActivities() {
		return activities;
	}
	

	public void setActivities(ArrayList<ModuleDescRef> activities) {
		this.activities = activities;
	}
	

	@Override
	public void populateActivityDef() throws ObjectNotFoundException, CannotManageException {
		super.populateActivityDef();
		CompositeActivityDef compActDef = (CompositeActivityDef)actDef;
		ArrayList<ActivityDef> graphActDefs = compActDef.getRefChildActDef();
		if (activities.size() != graphActDefs.size())
			throw new CannotManageException("There were "+activities.size()+" declared activities, but the graph uses "+graphActDefs.size());
		for (ModuleDescRef moduleDescRef : activities) {
			boolean found = false;
			for (ActivityDef childActDef : graphActDefs) {
				if (childActDef.getName().equals(moduleDescRef.getName()) &&
						childActDef.getVersion().equals(moduleDescRef.getVersion())) {
					found = true; break;
				}
			}
			if (!found) throw new CannotManageException("Graphed child activity "+moduleDescRef.getName()+" v"+moduleDescRef.getVersion()+" not referenced in module for "+getName());
		}
	}
	
}
