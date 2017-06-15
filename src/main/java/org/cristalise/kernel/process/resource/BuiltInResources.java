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

import lombok.Getter;

/**
 *
 */
@Getter
public enum BuiltInResources {
    //                     typeCode,   schemaName,              typeRoot,             workflowDef
    PROPERTY_DESC_RESOURCE("property", "PropertyDescription",   null,                 null), //'abstract' resource - does not have an Item
    ACTIVITY_DESC_RESOURCE("AC",       "ActivityDef",           null,                 null), //'abstract' resource - does not have an Item
    MODULE_RESOURCE(       "module",   "Module",                "/desc/Module",       "ManageModule"),
    SCHEMA_RESOURCE(       "OD",       "Schema",                "/desc/Schema",       "ManageSchema"),
    SCRIPT_RESOURCE(       "SC",       "Script",                "/desc/Script",       "ManageScript"),
    QUERY_RESOURCE(        "query",    "Query",                 "/desc/Query",        "ManageQuery"),
    STATE_MACHINE_RESOURCE("SM",       "StateMachine",          "/desc/StateMachine", "ManageStateMachine"),
    COMP_ACT_DESC_RESOURCE("CA",       "CompositeActivityDef",  "/desc/ActivityDesc", "ManageCompositeActDef"),
    ELEM_ACT_DESC_RESOURCE("EA",       "ElementaryActivityDef", "/desc/ActivityDesc", "ManageElementaryActDef");

    private String  typeCode;
    private String  schemaName;
    private String  typeRoot;
    private String  workflowDef;

    private BuiltInResources(final String code, final String schema, final String root, final String wf) {
        typeCode = code;
        schemaName = schema;
        typeRoot = root;
        workflowDef = wf;
    }

    public String toString() {
        return getTypeCode();
    }

    public static BuiltInResources getValue(String typeCode) {
        for (BuiltInResources res : BuiltInResources.values()) {
            if(res.getTypeCode().equals(typeCode) || res.name().equals(typeCode)) return res;
        }
        return null;
    }
}
