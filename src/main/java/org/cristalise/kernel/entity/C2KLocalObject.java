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
package org.cristalise.kernel.entity;

import org.cristalise.kernel.persistency.ClusterType;

/**
 * Objects that are to be stored by CRISTAL-iSE Items must implement this interface and be
 * (un)marshallable by Castor i.e. have a map file properly registered in the kernel.
 * Domain implementors shall not create new C2KLocalObjects
 * 
 * @see org.cristalise.kernel.persistency.ClusterStorage
 * @see org.cristalise.kernel.persistency.ClusterStorageManager
 */
public interface C2KLocalObject {

    /**
     * Sets the name of the C2KLocalObject
     * 
     * @param name Name of the C2KLocalObject
     */
    public void setName(String name);

    /**
     * Gets the name of the C2KLocalObject
     * @return name of the C2KLocalObject
     */
    public String getName();

    /**
     * Each object belongs to a specific type defined in {@link org.cristalise.kernel.persistency.ClusterStorage}
     * 
     * @return string id of the type
     */
    public ClusterType getClusterType();

    /**
     * Each C2KLocalObject is stored with a path identifier starting with the ClusterType:
     * <ul>
     * <li>Properties:  /Property/Name
     * <li>Workflow:    /LifeCycle/workflow
     * <li>Collections: /Collection/Name/Version (default Name='last')
     * <li>Outcomes:    /Outcome/SchemaName/SchemaVersion/EventID
     * <li>Viewpoints:  /ViewPoint/SchemaName/Name (default Name='last')
     * <li>Events:      /AuditTrail/EventID
     * <li>Jobs:        /Job/JobID
     * </ul>
     * @return The path identifier (i.e. primary key) of the object
     */
    public String getClusterPath();

    /**
     * Use this method to ensure very strict name policy
     * 
     * @param name to be checked
     * @throws IllegalArgumentException name must be alphanumeric with '_-:~' characters
     */
    public static void enforceValidName(String name) {
        String regex = "^[\\w-:\\~]*$";

        if (!name.matches(regex))
            throw new IllegalArgumentException("Name='"+name+"' must be alphanumeric with '_-:~' characters");
    }
}
