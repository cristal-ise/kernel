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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AgentName;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AgentRole;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.Breakpoint;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.Description;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.OutcomeInit;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.Viewpoint;

import org.cristalise.kernel.utils.CastorHashMap;

/**
 * 
 */
public class WfCastorHashMap extends CastorHashMap {
    
    private static final long serialVersionUID = 8700678607957394346L;

	public WfCastorHashMap()
	{
		put(Description.getAlternativeName(), "");
        put(AgentName.getAlternativeName(), "");
        put(AgentRole.getAlternativeName(), "");
		put(Breakpoint.getAlternativeName(), false);
        put(Viewpoint.getAlternativeName(), "");
        put(OutcomeInit.getAlternativeName(), "");

     /* Deprecated description references
        put("SchemaType", "");
	    put("SchemaVersion", "");
        put("ScriptName", "");
        put("ScriptVersion", "");
        put("StateMachineName", "Default");
        put("StateMachineVersion", 0);
      */
	}
}
