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

import java.util.Vector;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.traversal.GraphTraversal;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.Logger;


/**
 * @version $Revision: 1.47 $ $Date: 2006/05/29 13:17:45 $
 * @author $Author: abranson $
 */
public abstract class Split extends WfVertex
{
    public Vector<String> mErrors;

    /**
     * @see java.lang.Object#Object()
     */
    public Split()
    {
        mErrors = new Vector<String>(0, 1);
        getProperties().put("RoutingScriptName", "");
        getProperties().put("RoutingScriptVersion", "");
    }

    private boolean loopTested;

    /**
     * @throws InvalidDataException 
     * @throws ObjectNotFoundException 
     * @throws AccessRightsException 
     * @throws InvalidTransitionException 
     * @throws PersistencyException 
     * @throws ObjectAlreadyExistsException 
     * @throws ObjectCannotBeUpdated 
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#runNext()
     */
    @Override
	public abstract void runNext(AgentPath agent, ItemPath itemPath) throws InvalidDataException;

    /**
     * Method addNext.
     *
     * @param idNext
     */
    void addNext(String idNext)
    {
        new Next(this, (WfVertex) getParent().search(idNext));
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#addNext(org.cristalise.kernel.lifecycle.instance.WfVertex)
     */
    @Override
	public Next addNext(WfVertex vertex)
    {
        Next nxt = new Next(this, vertex);
        int num = getOutGraphables().length;

        try {
            num = Integer.parseInt((String) getProperties().get("LastNum"));
        }
        catch (Exception e) {
            Logger.debug(8, "Split::addNext() - Exception:"+e.getMessage());
        }

        nxt.getProperties().put("Alias", String.valueOf(num));
        getProperties().put("LastNum", String.valueOf(num + 1));
        return nxt;
    }

    @Override
	public void reinit(int idLoop) throws InvalidDataException
    {
        Vertex[] outVertices = getOutGraphables();
        for (Vertex outVertice : outVertices)
			((WfVertex) outVertice).reinit(idLoop);
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#verify()
     */
    @Override
	public boolean verify()
    {
        mErrors.removeAllElements();
        int nbInEdgres = getParent().getChildrenGraphModel().getInEdges(this).length;
        if (nbInEdgres == 0 && this.getID() != getParent().getChildrenGraphModel().getStartVertexId())
        {
            mErrors.add("not enough previous");
            return false;
        }
        if (nbInEdgres > 1)
        {
            mErrors.add("Bad nb of previous");
            return false;
        }
        if (getOutEdges().length <= 1 && !(this instanceof Loop))
        {
            mErrors.add("not enough next");
            return false;
        }
        if (!(this instanceof Loop))
        {
            Vertex[] outV = getOutGraphables();
            Vertex[] anteVertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, false);
            boolean loop = false;
            boolean errInLoop = true;
            for (int i = 0; i < outV.length; i++)
            {
                for (int j = 0; j < anteVertices.length; j++)
                    if (!loop && outV[i].getID() == anteVertices[j].getID())
                    {
                        if (outV[i] instanceof Loop)
                        {
                            loop = true;
                            j = anteVertices.length;
                            i = outV.length;
                        } else
                        {
                            errInLoop = false;
                        }
                    }
            }
            if (errInLoop && loop)
            {
                mErrors.add("Problem in Loop");
                return false;
            }
        }
        return true;
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#getErrors()
     */
    @Override
	public String getErrors()
    {
        if (mErrors.size() == 0)
            return "No error";
        else
            return mErrors.elementAt(0);
    }

    /**
     * @throws InvalidDataException 
     * @throws ObjectNotFoundException 
     * @throws AccessRightsException 
     * @throws InvalidTransitionException 
     * @throws PersistencyException 
     * @throws ObjectAlreadyExistsException 
     * @throws ObjectCannotBeUpdated 
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#run()
     */
    @Override
	public void run(AgentPath agent, ItemPath itemPath) throws InvalidDataException
    {
        runNext(agent, itemPath);
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#loop()
     */
    @Override
	public boolean loop()
    {
        boolean loop2 = false;
        if (!loopTested)
        {
            loopTested = true;
            if (getOutGraphables().length != 0)
            {
                Vertex[] outVertices = getOutGraphables();
                for (int i = 0; i < outVertices.length; i++)
                {
                    WfVertex tmp = (WfVertex) getOutGraphables()[i];
                    loop2 = loop2 || tmp.loop();
                }
            }
        }
        loopTested = false;
        return loop2;
    }

    public String[] nextNames()
    {
        Vertex[] vs = getOutGraphables();
        String[] result = new String[vs.length];
        for (int i = 0; i < vs.length; i++)
            result[i] = vs[i].getName();
        return result;
    }

    protected boolean isInTable(String test, String[] list)
    {
        if (test == null)
            return false;
        for (String element : list)
			if (test.equals(element))
                return true;
        return false;
    }

    @Override
	public void runFirst(AgentPath agent, ItemPath itemPath) throws InvalidDataException
    {
        runNext(agent, itemPath);
    }

}