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
package org.cristalise.kernel.lifecycle.instance;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.Logger;


/**
 * @version $Revision: 1.23 $ $Date: 2006/03/03 13:52:21 $
 * @author  $Author: abranson $
 */
public class XOrSplit extends Split
{
    /**
     * @see java.lang.Object#Object()
     */
    public XOrSplit()
    {
        super();
    }

    @Override
	public void runNext(AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException
    {
        ArrayList<DirectedEdge> nextsToFollow = new ArrayList<DirectedEdge>();
        String nexts;
		String scriptName = (String) getProperties().get("RoutingScriptName");
		Integer scriptVersion = deriveVersionNumber(getProperties().get("RoutingScriptVersion"));
        try {
			nexts = this.evaluateScript(scriptName, scriptVersion, itemPath, locker).toString();
		} catch (ScriptingEngineException e) {
			Logger.error(e);
			throw new InvalidDataException("Error running routing script "+scriptName+" v"+scriptVersion);
		}

        StringTokenizer tok = new StringTokenizer(nexts,",");
        String[] nextsTab = new String[tok.countTokens()];
        for (int i=0;i<nextsTab.length;i++)
            nextsTab[i] = tok.nextToken();

        DirectedEdge[] outEdges = getOutEdges();
        for (DirectedEdge outEdge : outEdges) {
            if (isInTable((String)((Next)outEdge).getProperties().get("Alias"), nextsTab))
                nextsToFollow.add(outEdge);
        }
//        Logger.debug(0, getID()+" following "+nexts);
        if (nextsToFollow.size() != 1)
            throw new InvalidDataException("not good number of active next");

        followNext((Next)nextsToFollow.get(0), agent, itemPath, locker);

    }

    public void followNext(Next activeNext, AgentPath agent, ItemPath itemPath, Object locker) throws InvalidDataException {
        activeNext.getTerminusVertex().run(agent, itemPath, locker);
    }

}
