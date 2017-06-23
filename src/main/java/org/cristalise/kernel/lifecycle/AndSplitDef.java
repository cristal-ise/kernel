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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_EXPR;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_VERSION;

import java.util.Vector;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.traversal.GraphTraversal;
import org.cristalise.kernel.lifecycle.instance.AndSplit;
import org.cristalise.kernel.lifecycle.instance.WfVertex;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.LocalObjectLoader;

public class AndSplitDef extends WfVertexDef {

    public AndSplitDef() {
        mErrors = new Vector<String>(0, 1);

        setBuiltInProperty(ROUTING_SCRIPT_NAME, "");
        setBuiltInProperty(ROUTING_SCRIPT_VERSION, "");
        setBuiltInProperty(ROUTING_EXPR, "");
    }

    @Override
    public boolean verify() {
        mErrors.removeAllElements();
        boolean err = true;
        int nbInEdges = getInEdges().length;
        if (nbInEdges == 0 && this.getID() != getParent().getChildrenGraphModel().getStartVertexId()) {
            mErrors.add("Unreachable");
            err = false;
        }
        else if (nbInEdges > 1) {
            mErrors.add("Bad nb of previous");
            err = false;
        }
        else {
            if (getOutEdges().length <= 1) {
                mErrors.add("not enough next");
                err = false;
            }
            else if (!(this instanceof LoopDef)) {
                Vertex[] outV = getOutGraphables();
                Vertex[] anteVertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, false);
                boolean loop = false;
                boolean errInLoop = true;

                for (int i = 0; i < outV.length; i++) {
                    for (int j = 0; j < anteVertices.length; j++)
                        if (!loop && outV[i].getID() == anteVertices[j].getID()) {
                            if (outV[i] instanceof LoopDef) {
                                loop = true;
                                j = anteVertices.length;
                                i = outV.length;
                            }
                            else {
                                errInLoop = false;
                            }
                        }
                }
                if (errInLoop && loop) {
                    mErrors.add("Problem in Loop");
                    err = false;
                }
            }
        }
        return err;
    }

    @Override
    public boolean loop() {
        boolean loop2 = false;
        if (!loopTested) {
            loopTested = true;
            if (getOutGraphables().length != 0) {
                Vertex[] outVertices = getOutGraphables();
                for (int i = 0; i < outVertices.length; i++) {
                    WfVertexDef tmp = (WfVertexDef) getOutGraphables()[i];
                    loop2 = loop2 || tmp.loop();
                }
            }
        }
        loopTested = false;
        return loop2;
    }

    @Override
    public WfVertex instantiate() throws InvalidDataException, ObjectNotFoundException {
        AndSplit newSplit = new AndSplit();
        configureInstance(newSplit);
        return newSplit;
    }

    public Script getRoutingScript() throws ObjectNotFoundException, InvalidDataException {
        String scriptName = (String) getBuiltInProperty(ROUTING_SCRIPT_NAME);
        Integer scriptVersion;
        try {
            String scriptVerStr = (String) getBuiltInProperty(ROUTING_SCRIPT_VERSION);
            if (scriptVerStr != null && !scriptVerStr.isEmpty())
                scriptVersion = Integer.valueOf(scriptVerStr.toString());
            else
                throw new ObjectNotFoundException();
        }
        catch (NumberFormatException e) {
            throw new InvalidDataException(e.getMessage());
        }
        return LocalObjectLoader.getScript(scriptName, scriptVersion);
    }

}
