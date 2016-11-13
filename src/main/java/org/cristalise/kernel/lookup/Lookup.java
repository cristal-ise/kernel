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

import java.util.Iterator;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyDescriptionList;


/**
 */
public interface Lookup {

    /**
     * Connect to the directory using the credentials supplied in the Authenticator. 
     * 
     * @param user The connected Authenticator. The Lookup implementation may use the AuthObject in this to communicate with the database.
     */
    public void open(Authenticator user);

    /**
     * Shutdown the lookup
     */
    public void close();

    /**
     * Fetch the correct subclass class of ItemPath for a particular Item, derived from its lookup entry. 
     * This is used by the CORBA Server to make sure the correct Item subclass is used. 
     * 
     * @param sysKey The system key of the Item
     * @return an ItemPath or AgentPath
     * @throws InvalidItemPathException When the system key is invalid/out-of-range
     * @throws ObjectNotFoundException When the Item does not exist in the directory.
     */
    public ItemPath getItemPath(String sysKey) throws InvalidItemPathException, ObjectNotFoundException;

    /**
     * Find the ItemPath for which a DomainPath is an alias.
     * 
     * @param domainPath The path to resolve
     * @return The ItemPath it points to (should be an AgentPath if the path references an Agent)
     * @throws InvalidItemPathException
     * @throws ObjectNotFoundException
     */
    public ItemPath resolvePath(DomainPath domainPath) throws InvalidItemPathException, ObjectNotFoundException;

    /**
     * Resolve a path to a CORBA Object Item or Agent
     * 
     * @param path The path to be resolved
     * @return The CORBA Object's IOR
     * @throws ObjectNotFoundException When the Path doesn't exist, or doesn't have an IOR associated with it
     */
    public String getIOR(Path path) throws ObjectNotFoundException;

    /**
     * Checks if a particular Path exists in the directory
     * @param path The path to check
     * @return boolean true if the path exists, false if it doesn't
     */
    public boolean exists(Path path);

    /**
     * List the next-level-deep children of a Path
     * 
     * @param path The parent Path
     * @return An Iterator of child Paths
     */
    public Iterator<Path> getChildren(Path path);

    /**
     * Find a path with a particular name (last component)
     * 
     * @param start Search root
     * @param name The name to search for
     * @return An Iterator of matching Paths. Should be an empty Iterator if there are no matches.
     */
    public Iterator<Path> search(Path start, String name);

    /**
     * Search for Items in the specified path with the given property list
     * @param start Search root
     * @param props list of Properties
     * @return An Iterator of matching Paths
     */
    public Iterator<Path> search(Path start, Property... props);

    /**
     * Search for Items of a particular type, based on its PropertyDescription outcome
     * @param start Search root
     * @param props Properties unmarshalled from an ItemDescription's property description outcome.
     * @return An Iterator of matching Paths
     */
    public Iterator<Path> search(Path start, PropertyDescriptionList props);

    /**
     * Find all DomainPaths that are aliases for a particular Item or Agent
     * @param itemPath The ItemPath
     * @return An Iterator of DomainPaths that are aliases for that Item
     */
    public Iterator<Path> searchAliases(ItemPath itemPath);

    /**
     * @param agentName
     * @return
     * @throws ObjectNotFoundException
     */
    public AgentPath getAgentPath(String agentName) throws ObjectNotFoundException;

    /**
     * Find the RolePath for the named Role
     * 
     * @param roleName
     * @return
     * @throws ObjectNotFoundException
     */
    public RolePath getRolePath(String roleName) throws ObjectNotFoundException;

    /**
     * Returns all of the Agents in this centre who hold this role (including sub-roles)
     * 
     * @param rolePath
     * @return the list of Agents
     * @throws ObjectNotFoundException
     */
    public AgentPath[] getAgents(RolePath rolePath) throws ObjectNotFoundException;

    /**
     * Get all roles held by the given Agent
     * 
     * @param agentPath
     * @return the list of Roles
     */
    public RolePath[] getRoles(AgentPath agentPath);

    /**
     * Checks if an agent qualifies as holding the stated Role, including any sub-role logic. 
     * 
     * @param agentPath
     * @param role
     * @return true or false 
     */
    public boolean hasRole(AgentPath agentPath, RolePath role);

    /**
     * Return the name of the Agent referenced by an AgentPath
     * @param agentPath
     * @return the name string
     * @throws ObjectNotFoundException
     */
    public String getAgentName(AgentPath agentPath) throws ObjectNotFoundException;	
}
