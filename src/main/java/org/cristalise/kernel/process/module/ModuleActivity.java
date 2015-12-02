package org.cristalise.kernel.process.module;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
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

	public ModuleActivity() {
		super();
		resourceType="EA";
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

		ActivityDef actDef = new ActivityDef();
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
