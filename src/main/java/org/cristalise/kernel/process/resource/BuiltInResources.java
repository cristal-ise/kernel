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


/*
case COMP_ACT_DESC_RESOURCE:
    schemaName = "CompositeActivityDef";
    typeRoot = "/desc/ActivityDesc";
    wfDef = "ManageCompositeActDef";
    break;

case ELEM_ACT_DESC_RESOURCE:
    schemaName = "ElementaryActivityDef";
    typeRoot = "/desc/ActivityDesc";
    wfDef = "ManageElementaryActDef";
    break;

case SCHEMA_RESOURCE:
    schemaName = "Schema";
    typeRoot = "/desc/OutcomeDesc";
    wfDef = "ManageSchema";
    break;

case SCRIPT_RESOURCE:
    schemaName = "Script";
    typeRoot = "/desc/Script";
    wfDef = "ManageScript";
    break;

case STATE_MACHINE_RESOURCE:
    schemaName = "StateMachine";
    typeRoot = "/desc/StateMachine";
    wfDef = "ManageStateMachine";
    break;

case QUERY_RESOURCE:
    schemaName = "Query";
    typeRoot = "/desc/Query";
    wfDef = "ManageQuery";
    break;
*/

/**
 *
 */
@Getter
public enum BuiltInResources {
    PROPERTY_DESC_RESOURCE("property", "PropertyDescription",   null,                 null),
    SCHEMA_RESOURCE(       "OD",       "Schema",                "/desc/OutcomeDesc",  "ManageSchema"),
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
