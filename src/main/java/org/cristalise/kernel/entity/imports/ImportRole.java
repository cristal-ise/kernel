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
package org.cristalise.kernel.entity.imports;

import java.util.Iterator;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.module.ModuleImport;


public class ImportRole extends ModuleImport {

	private boolean jobList;
	
	public ImportRole() {
	}
	
	@Override
	public void create(AgentPath agentPath, boolean reset) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, ObjectNotFoundException {
		RolePath parent = new RolePath();
		if (name.indexOf('/') > -1) {
			String[] roleComp = name.split("/");
			for (int i=0; i<roleComp.length-1; i++) {
				Iterator<Path> childIter = parent.getChildren();
				boolean found = false;
				while (childIter.hasNext()) {
					RolePath childRole = (RolePath)childIter.next();
					if (childRole.getName().equals(roleComp[i])) {
						parent = childRole;
						found = true;
						break;
					}
				}
				if (!found) throw new ObjectNotFoundException("Parent role "+roleComp[i]+" was not found");
			}
			name = roleComp[roleComp.length-1];
		}
		RolePath newRole = new RolePath(parent, name, jobList);
		if (!newRole.exists()) Gateway.getLookupManager().createRole(newRole);
	}

	public boolean hasJobList() {
		return jobList;
	}

	public void setJobList(boolean jobList) {
		this.jobList = jobList;
	}

}
