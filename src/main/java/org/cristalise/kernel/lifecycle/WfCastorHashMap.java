/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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
import org.cristalise.kernel.utils.CastorHashMap;
/**
 * @version $Revision: 1.29 $ $Date: 2005/05/20 13:07:13 $
 * @author  $Author: abranson $
 */
public class WfCastorHashMap extends CastorHashMap
{
	/**
	 * @see java.lang.Object#Object()
	 */
	public WfCastorHashMap()
	{
		put("Show time", true);
		put("Breakpoint", false);
		put("Description", "");
        put("Agent Role", "");
        put("Agent Name", "");
		put("SchemaType", "");
		put("SchemaVersion", "");
        put("ScriptName", "");
        put("ScriptVersion", "");
        put("StateMachineName", "Default");
        put("StateMachineVersion", 0);
        put("Viewpoint", "");
        put("OutcomeInit", "");
	}
}
