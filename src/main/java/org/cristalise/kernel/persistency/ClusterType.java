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
package org.cristalise.kernel.persistency;

public enum ClusterType {

    /**
     * The defined path of the root of the CRISTAL Kernel object cluster tree. A
     * zero-length string.
     */
    ROOT(""),
    /**
     * 
     */
    PATH("Path"),
    /**
     * The root of the Property object cluster. All Property paths start with
     * this. Defined as "Property". Properties are stored underneath according
     * to their name e.g. "Property/Name"
     */
    PROPERTY("Property"),
    /**
     * The root of the Collection object cluster. All Collection paths start
     * with this. Defined as "Collection". Collections are stored underneath by
     * name e.g. "Collection/Composition"
     */
    COLLECTION("Collection"),
    /**
     * The cluster which holds the Item workflow. Defined as "LifeCycle". Holds
     * the workflow inside, which is named "workflow", hence
     * "LifeCycle/workflow".
     * 
     * @see org.cristalise.kernel.lifecycle.instance.Workflow
     */
    LIFECYCLE("LifeCycle"),
    /**
     * This cluster holds all outcomes of this Item. The path to each outcome is
     * "Outcome/<i>Schema Name</i>/<i>Schema Version</i>/<i>Event ID</i>"
     */
    OUTCOME("Outcome"),
    /**
     * This is the cluster that contains all event for this Item. This cluster
     * may be instantiated in a client as a History, which is a RemoteMap.
     * Events are stored with their ID: "/AuditTrail/<i>Event ID</i>"
     */
    HISTORY("AuditTrail"),
    /**
     * This cluster contains all viewpoints. Its name is defined as "ViewPoint".
     * The paths of viewpoint objects stored here follow this pattern:
     * "ViewPoint/<i>Schema Name</i>/<i>Viewpoint Name</i>"
     */
    VIEWPOINT("ViewPoint"),
    /**
     * Agents store their persistent jobs in this cluster that have been pushed
     * to them by activities configured to do so. The name is defined as "Job"
     * and each new job received is assigned an integer ID one more than the
     * highest already present.
     */
    JOB("Job");

    private String clusterName;

    private ClusterType(final String n) {
        clusterName = n;
    }

    public String getName() {
        return clusterName;
    }

    public String toString() {
        return getName();
    }

    public static ClusterType getValue(String name) {
        for (ClusterType type : ClusterType.values()) {
            if(type.getName().equals(name) || type.name().equals(name)) return type;
        }
        return null;
    }

}
