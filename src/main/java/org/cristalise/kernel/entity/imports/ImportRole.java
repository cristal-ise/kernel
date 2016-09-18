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
package org.cristalise.kernel.entity.imports;

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

    public boolean jobList;

    public ImportRole() {}

    @Override
    public Path create(AgentPath agentPath, boolean reset)
            throws ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, ObjectNotFoundException
    {
        RolePath newRolePath = new RolePath(name.split("/"), jobList);

        // checks if parent exists
        newRolePath.getParent();

        // checks if Role already exists
        Gateway.getLookupManager().createRole(newRolePath);

        return newRolePath;
    }

    public boolean hasJobList() {
        return jobList;
    }
}
