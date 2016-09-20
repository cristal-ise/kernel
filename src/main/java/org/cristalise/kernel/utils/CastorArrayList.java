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
package org.cristalise.kernel.utils;

import java.util.ArrayList;

/**
 * Wrapper for a root element to an ArrayList. Castor marshalls arraylists as multiple 
 * elements, so this class is needed to provide a root element to stop it crashing.
 *
 * @param <E>
 */
abstract public class CastorArrayList<E> {
    public ArrayList<E> list;

    public CastorArrayList() {
        super();
        list = new ArrayList<E>();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((list == null) ? 0 : list.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)                  return true;
        if (obj == null)                  return false;
        if (getClass() != obj.getClass()) return false;

        CastorArrayList<?> other = (CastorArrayList<?>) obj;

        if (list == null)  return other.list == null;
        else               return list.equals(other.list);
    }

    public CastorArrayList(ArrayList<E> list) {
        this();
        this.list = list;
    }
}
