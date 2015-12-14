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

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;


public abstract class ModuleImport {
	
	protected String ns;
	protected String name;
	protected DomainPath domainPath;
	protected ItemPath itemPath;
	
	public ModuleImport() {
	}
    
	public abstract Path create(AgentPath agentPath, boolean reset) throws ObjectNotFoundException,
			ObjectCannotBeUpdated, CannotManageException, ObjectAlreadyExistsException, InvalidCollectionModification;
	
    public void setID( String uuid ) throws InvalidItemPathException 
    {
    	if (uuid != null && uuid.length() > 0) itemPath = new ItemPath(uuid);
    }
    
    public String getID() {
    	return itemPath==null?null:itemPath.getUUID().toString();
    }
    
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNamespace(String ns) {
		this.ns = ns;
	}
	
	public String getNamespace() {
		return ns;
	}
	
	public DomainPath getDomainPath() {
		return domainPath;
	}

	public void setDomainPath(DomainPath domainPath) {
		this.domainPath = domainPath;
	}

	public ItemPath getItemPath() {
		return itemPath;
	}

	public void setItemPath(ItemPath itemPath) {
		this.itemPath = itemPath;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode()+(ns == null?0:ns.hashCode());
	}


}