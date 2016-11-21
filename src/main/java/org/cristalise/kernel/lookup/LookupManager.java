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
package org.cristalise.kernel.lookup;

import java.security.NoSuchAlgorithmException;

import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;


/**
 * The LookupManager interface contains all of the directory modifying methods
 * of the Lookup. This allows read-only Lookup implementations. Server processes
 * will attempt to cast their Lookups into LookupManagers, and fail to start up
 * if this is not possible.
 *
 */
public interface LookupManager extends Lookup {

    /**
     * Called when a server starts up. The Lookup implementation should ensure 
     * that the initial structure of its directory is valid, and create it on 
     * first boot.
     * 
     * @throws ObjectNotFoundException When initialization data is not found
     */
    public void initializeDirectory() throws ObjectNotFoundException;

    // Path management

    /**
     * Register a new a Path in the directory.
     * 
     * @param newPath The path to add
     * @throws ObjectCannotBeUpdated When there is an error writing to the 
     * directory
     * @throws ObjectAlreadyExistsException When the Path has already been registered
     */
    public void add(Path newPath) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException;

    /**
     * Remove a Path from the directory.
     * 
     * @param path The path to remove
     * @throws ObjectCannotBeUpdated When an error occurs writing to the directory
     */
    public void delete(Path path) throws ObjectCannotBeUpdated;

    // Role and agent management

    /**
     * Creates a new Role. Checks if parent role exists or not and throws ObjectCannotBeUpdated if parent does not exist
     * Called by the server predefined step 'CreateNewRole'
     * 
     * @param role The new role path
     * @return
     * @throws ObjectAlreadyExistsException 
     * @throws ObjectCannotBeUpdated
     */
    public RolePath createRole(RolePath role) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated;

    /**
     * Adds the given Agent to the given Role, if they both exist.
     * 
     * @param agent 
     * @param rolePath
     * @throws ObjectCannotBeUpdated
     * @throws ObjectNotFoundException
     */
    public void addRole(AgentPath agent, RolePath rolePath) throws ObjectCannotBeUpdated, ObjectNotFoundException;

    /**
     * Remove the given Agent from the given Role. Does not delete the Role.
     * 
     * @param agent
     * @param role
     * @throws ObjectCannotBeUpdated
     * @throws ObjectNotFoundException
     */
    public void removeRole(AgentPath agent, RolePath role) throws ObjectCannotBeUpdated, ObjectNotFoundException;

    /**
     * Set an Agent's password
     * 
     * @param agent The Agent
     * @param newPassword The Agent's new password
     * @throws ObjectNotFoundException
     * @throws ObjectCannotBeUpdated
     * @throws NoSuchAlgorithmException
     */
    public void setAgentPassword(AgentPath agent, String newPassword) throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException;

    /**
     * Set the flag specifying whether Activities holding this Role should push
     * Jobs its Agents.
     * 
     * @param role The role to modify
     * @param hasJobList boolean flag
     * 
     * @throws ObjectNotFoundException When the Role doesn't exist
     * @throws ObjectCannotBeUpdated 
     */
    public void setHasJobList(RolePath role, boolean hasJobList) throws ObjectNotFoundException, ObjectCannotBeUpdated;
}
