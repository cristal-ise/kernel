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

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Bootstrap;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

public class ModuleActivity extends ModuleResource {

	ModuleDescRef script, schema, stateMachine;
	ActivityDef actDef;

	public ModuleActivity() {
		super();
		resourceType="EA";
		actDef = new ActivityDef();
	}
	
	public ModuleActivity(ItemProxy child, Integer version) throws ObjectNotFoundException, InvalidDataException {
		this();
		this.version = version;
		script = getDescRef(child, ActivityDef.SCRCOL);
		schema = getDescRef(child, ActivityDef.SCHCOL);
		stateMachine = getDescRef(child, ActivityDef.SMCOL);
		
	}
	
	public ModuleDescRef getDescRef(ItemProxy child, String collName) throws ObjectNotFoundException, InvalidDataException {
		Collection<?> coll = child.getCollection(collName, version);
		if (coll.size() == 1) throw new InvalidDataException("Too many members in "+collName+" collection in "+name);
		CollectionMember collMem = coll.getMembers().list.get(0);
		return new ModuleDescRef(null, collMem.getChildUUID(), Integer.valueOf(collMem.getProperties().get("Version").toString()));
	}

	@Override
	public Path create(AgentPath agentPath, boolean reset)
			throws ObjectNotFoundException, ObjectCannotBeUpdated,
			CannotManageException, ObjectAlreadyExistsException {
		try {
			domainPath = Bootstrap.verifyResource(ns, name, version, resourceType, itemPath, resourceLocation, reset);
			itemPath = domainPath.getItemPath();
		} catch (Exception e) {
			Logger.error(e);
			throw new CannotManageException("Exception verifying module resource "+ns+"/"+name);
		}		

		populateActivityDef();
		
		CollectionArrayList colls;
		try {
			colls = actDef.makeDescCollections();
		} catch (InvalidDataException e) {
			Logger.error(e);
			throw new CannotManageException("Could not create description collections for "+getName()+".");
		}
		for (Collection<?> coll : colls.list) {
			try {
				Gateway.getStorage().put(itemPath, coll, null);
				// create last collection if not present
				try {
					Gateway.getStorage().get(itemPath, ClusterStorage.COLLECTION+"/"+coll.getName()+"/last", null);
				} catch (ObjectNotFoundException ex) {
					coll.setVersion(null);
					Gateway.getStorage().put(itemPath, coll, null);
				}
			} catch (PersistencyException e) {
				Logger.error(e);
				throw new CannotManageException("Persistency exception storing description collections for "+getName()+".");
			}
		}

		return domainPath;
	}
	
	public void populateActivityDef() throws ObjectNotFoundException, CannotManageException {
		try {
			if (schema != null) 
				actDef.setSchema(LocalObjectLoader.getSchema(schema.id == null? schema.name:schema.id, Integer.valueOf(schema.version)));
		} catch (NumberFormatException | InvalidDataException e) {
			Logger.error(e);
			throw new CannotManageException("Schema definition in "+getName()+" not valid.");
		}
		try {
			if (script != null) 
				actDef.setScript(LocalObjectLoader.getScript(script.id == null? script.name:script.id, Integer.valueOf(script.version)));
		} catch (NumberFormatException | InvalidDataException e) {
			Logger.error(e);
			throw new CannotManageException("Script definition in "+getName()+" not valid.");
		}
		try {
			if (stateMachine != null) 
				actDef.setStateMachine(LocalObjectLoader.getStateMachine(stateMachine.id == null? stateMachine.name:stateMachine.id, Integer.valueOf(stateMachine.version)));
		} catch (NumberFormatException | InvalidDataException e) {
			Logger.error(e);
			throw new CannotManageException("State Machine definition in "+getName()+" not valid.");
		}
	}

	public ModuleDescRef getScript() {
		return script;
	}
	

	public void setScript(ModuleDescRef script) {
		this.script = script;
	}
	

	public ModuleDescRef getSchema() {
		return schema;
	}
	

	public void setSchema(ModuleDescRef schema) {
		this.schema = schema;
	}
	

	public ModuleDescRef getStateMachine() {
		return stateMachine;
	}
	

	public void setStateMachine(ModuleDescRef stateMachine) {
		this.stateMachine = stateMachine;
	}
	
	
}
