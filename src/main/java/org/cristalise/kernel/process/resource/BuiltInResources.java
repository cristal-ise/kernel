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


/**
 *
 */
public enum BuiltInResources {
    PROPERTY_DESC("property"),
    SCHEMA("OD"),
    SCRIPT("SC"),
    QUERY("query"),
    STATE_MACHINE("SM"),
    COMPOSITE_ACTIVITY_DESC("CA"),
    ELEMENTARY_ACTIVITY_DESC("EA");

    private String typeName;

    private BuiltInResources(final String n) {
        typeName = n;
    }

    public String getName() {
        return typeName;
    }

    public String toString() {
        return getName();
    }

    public static BuiltInResources getValue(String typeName) {
        for (BuiltInResources res : BuiltInResources.values()) {
            if(res.getName().equals(typeName) || res.name().equals(typeName)) return res;
        }
        return null;
    }
}
