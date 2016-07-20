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
package org.cristalise.kernel.lifecycle;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_ROLE;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.BREAKPOINT;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.DESCRIPTION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.OUTCOME_INIT;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VIEW_POINT;

import org.cristalise.kernel.utils.CastorHashMap;

/**
 * 
 */
public class WfCastorHashMap extends CastorHashMap {

    private static final long serialVersionUID = 8700678607957394346L;

    public WfCastorHashMap() {
        setBuiltInProperty(DESCRIPTION,  "");
        setBuiltInProperty(AGENT_NAME,   "");
        setBuiltInProperty(AGENT_ROLE,   "");
        setBuiltInProperty(BREAKPOINT,   false);
        setBuiltInProperty(VIEW_POINT,   "");
        setBuiltInProperty(OUTCOME_INIT, "");
    }
}
