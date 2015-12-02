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
package org.cristalise.kernel.process.auth;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;

/**
 * This interface is used by the kernel to store an authenticated connection
 * and/or token that will be used by kernel components. The CRISTAL property
 * 'Authenticator' is used to specify the implementation used. It is
 * instantiated by the connect() methods of the Gateway, and will be found in
 * the AgentProxy returned by connect(). Lookup and ClusterStorage instances are
 * initialized with this Authenticator, which is expected to maintain the same
 * user's connection through the process lifetime, reconnecting if the
 * connection is lost.
 * 
 * @since 3.0
 * 
 */
public interface Authenticator {

	/**
	 * Authenticates a CRISTAL agent. If this method returns true, then the
	 * connect method will create and return an AgentProxy for the given
	 * username using the Lookup and ProxyManager.
	 * 
	 * @param agentName
	 *            The username of the Agent to be authenticated. This must be
	 *            already present as an Agent in the CRISTAL directory.
	 * @param password
	 *            The Agent's password
	 * @param resource
	 *            The authentication resource/domain/realm of the agent.
	 *            Included so that domains may include CRISTAL users from
	 *            different realms. This parameter is passed into the connect()
	 *            method if required. May be null.
	 * @return a boolean indicating if the authentication was successful. If so,
	 *         then the Gateway will generate an AgentProxy for the given user.
	 * @throws ObjectNotFoundException
	 *             When the Agent doesn't exist
	 * @throws InvalidDataException
	 *             When authentication fails for another reason
	 */
	public boolean authenticate(String agentName, String password,
			String resource) throws InvalidDataException,
			ObjectNotFoundException;

	/**
	 * Authenticates a superuser connection for the server. It must be able to
	 * act on behalf of any other Agent, as the server needs to do this.
	 * Credentials may be in the CRISTAL properties, or some other mechanism.
	 * 
	 * @param resource
	 * @return
	 * @throws InvalidDataException
	 * @throws ObjectNotFoundException
	 */
	public boolean authenticate(String resource) throws InvalidDataException,
			ObjectNotFoundException;

	/**
	 * Lookup and storage implementations that need to use user or superuser
	 * authentication can retrieve it using this method. This will be highly
	 * implementation specific.
	 * 
	 * @return the connection/token created during authentication
	 */
	public Object getAuthObject();

	/**
	 * Close or expire the connection as the CRISTAL process shuts down.
	 */
	public void disconnect();
}
