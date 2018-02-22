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
package org.cristalise.kernel.graph.model;

import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 *
 */
@Accessors(prefix = "m") @Getter @Setter
public abstract class GraphableEdge extends DirectedEdge {

    private GraphableVertex mParent;
    private CastorHashMap   mProperties = null;

    public GraphableEdge() {
        super();
        mProperties = new CastorHashMap();
    }

    public GraphableEdge(GraphableVertex pre, GraphableVertex nex) {
        mProperties = new CastorHashMap();
        setParent(pre.getParent());
        pre.getParent().getChildrenGraphModel().addEdgeAndCreateId(this, pre, nex);
    }

    public KeyValuePair[] getKeyValuePairs() {
        return mProperties.getKeyValuePairs();
    }

    public void setKeyValuePairs(KeyValuePair[] pairs) {
        mProperties.setKeyValuePairs(pairs);
    }

    public Object getBuiltInProperty(BuiltInEdgeProperties prop) {
        return mProperties.get(prop.getName());
    }

    public void setBuiltInProperty(BuiltInEdgeProperties prop, Object val) {
        mProperties.put(prop.getName(), val);
    }
}
