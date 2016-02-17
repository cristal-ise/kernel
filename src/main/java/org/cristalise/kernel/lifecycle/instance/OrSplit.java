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
package org.cristalise.kernel.lifecycle.instance;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.Logger;

/**
 * @version $Revision: 1.22 $ $Date: 2005/05/10 15:14:54 $
 * @author $Author: abranson $
 */
public class OrSplit extends Split
{
	/**
	 * @see java.lang.Object#Object()
	 */
	public OrSplit()
	{
		super();
	}
	@Override
	public void runNext(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException 
	{
		String[] nextsTab = calculateNexts(itemPath, locker);
		
		int active = 0;
		DirectedEdge[] outEdges = getOutEdges();
		for (String thisNext : nextsTab) {
			Logger.msg(7, "Finding next " + thisNext);
			for (DirectedEdge outEdge : outEdges) {
				Next nextEdge = (Next) outEdge;
				if (thisNext != null && thisNext.equals(nextEdge.getProperties().get("Alias")))
				{
                    WfVertex term = nextEdge.getTerminusVertex();
                    try {
						term.run(agent, itemPath, locker);
					} catch (InvalidDataException e) {
						Logger.error(e);
						throw new InvalidDataException("Error enabling next "+thisNext);
					}
					Logger.msg(7, "Running " + nextEdge.getProperties().get("Alias"));
					active++;
				}
			}
		}

		// if no active nexts throw exception
		if (active == 0)
			throw new InvalidDataException("No nexts were activated!");
	}


}
