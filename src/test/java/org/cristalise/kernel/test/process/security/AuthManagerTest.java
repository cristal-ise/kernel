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
package org.cristalise.kernel.test.process.security;

import static org.junit.Assert.assertEquals;

import java.util.Base64;
import java.util.UUID;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.security.AuthManager;
import org.junit.Test;

public class AuthManagerTest {

	@Test
	public void test() throws InvalidItemPathException, AccessRightsException {
		UUID uuid = UUID.randomUUID();
		AgentPath agentPath = new AgentPath(new ItemPath(uuid.toString()), "toto");
		
		AuthManager authManager = new AuthManager();
		String token = authManager.generateToken(agentPath);
		System.out.println("JWT: " + token);
		System.out.println("Payload: " + new String(Base64.getUrlDecoder().decode(token.split("\\.")[1])) );

		AgentPath actualAgentPath = authManager.decodeAgentPath(token);
		assertEquals(agentPath.getAgentName(), actualAgentPath.getAgentName());
		assertEquals(agentPath.getUUID(), actualAgentPath.getUUID());
	}
	
	// TODO test exception cases
}
