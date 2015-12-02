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
/**
 * The implementations of Items, and their core functionality.
 * 
 * <p>The CORBA IDLs generate the Item and Agent interfaces and their support 
 * classes in this package. In the kernel source tree, the classes 
 * {@link TraceableEntity} and {@link ItemImplementation} provides the 
 * implementing object for the Item on the server side, while the Locator class,
 * plus the {@link CorbaServer} handle instantiation and caching of Items (and 
 * Agents) on the server.
 * 
 * <p>The corresponding implementation for Agents is located in the agent 
 * sub-package.
 * 
 * <p>Also in this package is the {@link C2KLocalObject} interface, which is 
 * implemented by all objects that may be stored in the CRISTAL persistency 
 * mechanism.
 */

package org.cristalise.kernel.entity;