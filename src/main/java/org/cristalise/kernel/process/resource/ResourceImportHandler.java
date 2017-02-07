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
package org.cristalise.kernel.process.resource;

import java.util.Set;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.property.PropertyDescriptionList;


public interface ResourceImportHandler {
    /**
     * Returns the DomainPath for a specific resource
     * 
     * @param ns - module namespace
     * @param name - resource name
     * @return DomainPath initialised
     */
    public DomainPath getPath(String name, String ns) throws Exception;

    /**
     * Generates the outcomes that the resource should contain.
     * 
     * @param name the name of the resource 
     * @param ns the namespace defined in the module
     * @param location the location of the resource file
     * @param version the specified version
     * @return a set of outcomes to be synced with the resource item.
     * @throws Exception - if the supplied resources are not valid
     */
    public Set<Outcome> getResourceOutcomes(String name, String ns, String location, Integer version) throws Exception;

    /** 
     * Gives the CompActDef name to instantiate to provide the workflow for this type of resource. 
     * Should be found in the CA typeroot (/desc/ActivityDesc/)
     * 
     * @return String workflow name
     */
    public String getWorkflowName() throws Exception; 

    /**
     * Should return all of the Properties the resource Item will have on creation. 
     * The Property 'Name' will be created and populated automatically, even if not declared.
     * 
     * @return a PropertyDescriptionList - an arraylist of PropertyDescriptions
     */
    public PropertyDescriptionList getPropDesc() throws Exception;

    /**
     * The directory context to search for existing resources. The name of the resource must
     * be unique below this point.
     * 
     * @return Root DomainPath
     */
    public DomainPath getTypeRoot();


    /**
     * Returns any collections that this Resource Item should contain.
     * 
     * @param name the name of the Resource Item
     * @param ns the namaspace of the module declaring the Resource Item
     * @param location the location of the XML file on the boot directory
     * @param version the version of the Resource Item
     * @return CollectionArrayList
     * @throws Exception something went wrong
     */
    public CollectionArrayList getCollections(String name, String ns, String location, Integer version) throws Exception;

    /**
     * Returns any collections that this Resource Item stored in the outcome should contain.
     * 
     * @param name the name of the Resource Item
     * @param version the version of the Resource Item
     * @param outcome Outcome containing the XML representing the Resource Item
     * @return CollectionArrayList
     * @throws Exception something went wrong
     */
    public CollectionArrayList getCollections(String name, Integer version, Outcome outcome) throws Exception;

    /**
     * The name of the imported resource managed by the handler
     * 
     * @return The name of the imported resource managed by the handler
     */
    public String getName();

}
