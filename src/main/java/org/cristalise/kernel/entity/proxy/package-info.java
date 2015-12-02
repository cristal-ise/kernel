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
 * The Proxy API is a major part of the client-side functionality of the 
 * CRISTAL API, which provides client-side proxy objects that represent the 
 * Items and Agents on the server. It is the main entry point for many 
 * components, such as Scripts and Job execution. An AgentProxy is returned on
 * login, and should be used as the root for all user-based CRISTAL interactions. 
 * 
 * <p>The Proxy API provides the following functionality:
 * 
 * <ul>
 * <li><b>Transparent storage integration</b> - Combines direct database access
 * with remote calls to data retrieval methods on the Items. This allows client 
 * processes to load Item data directly from databases whenever possible 
 * without bothering the CRISTAL server. For example, the LDAP Lookup 
 * implementation allows client processes to load Item Properties directly from 
 * the LDAP server.</li>
 * 
 * <li><b>Data object browsing and loading</b> - The proxy objects allow client
 * processes to browse through the storage cluster structure beneath the Item, 
 * and access the objects directly without having to unmarshall their XML forms.
 * All object types have their own get methods, so there's no need to construct
 * their paths nor cast.</b>
 *
 * <li><b>Item object and directory change notification</b> - When a proxy
 * object is created, it notifies the CRISTAL server that its Item is located
 * on, and it notified of all additions, deletions and modifications of objects
 * within that Item so it can remain up-to-date. Client applications may use 
 * the {@link ProxyObserver} interface to be notified of changes, using 
 * {@link MemberSubscription} instances to set up push subscriptions to cluster
 * contents. It also provides a mechanism for subscribing to directory paths, 
 * so that domain tree browsers can implement asynchronous loading and update
 * themselves when the tree changes.</li>
 * 
 * <li><b>Job querying</b> - Job objects may be retrieved directly from an 
 * ItemProxy, and may also be filtered by Activity name.</li>
 * 
 * <li><b>Job execution</b> - The {@link AgentProxy} provides the main 
 * execution method for Jobs. This method performs outcome validation and 
 * executes required CRISTAL Scripts in the client process before the execution 
 * is requested on the server. Additional execution methods to call Predefined
 * Steps are also available.
 * 
 * <li><b>Utility methods for resolution and marshalling</b> - The AgentProxy 
 * provides utility methods for finding Items in the directory by name, path, 
 * or system key, and gives access to the Castor XML marshalling system to 
 * transform CRISTAL objects to XML and back again.</li>
 * </ul>
 * <p>The core object of the Proxy API is the ProxyManager, which is initialized 
 * as a static member of the Gateway on initialization. This object can be used
 * to create a Proxy object from a Path from the directory, and maintains a
 * connection to the server called the Proxy Update Notification Channel, 
 * through which it subscribes to Items it holds proxies for so it can be 
 * informed of changes to Item data through {@link ProxyMessage} objects.
 * 
 */
package org.cristalise.kernel.entity.proxy;