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
package org.cristalise.kernel.collection;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;


/**
 * A collection that can be instantiated. Collection descriptions link Item 
 * descriptions together, which constrains membership of their instance. 
 * 
 */
public interface CollectionDescription<E extends CollectionMember> extends C2KLocalObject {
	
	/**
	 * Create a new instance of this collection.
	 * @return a new collection instance
	 * @throws ObjectNotFoundException When an essential piece of description 
	 * data, such as a child Item PropertyDescription outcome, doesn't exist.
	 */
	public Collection<E> newInstance() throws ObjectNotFoundException;
}
