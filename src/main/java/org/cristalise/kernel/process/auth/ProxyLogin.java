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

import java.util.Properties;

import org.cristalise.kernel.entity.proxy.AgentProxy;


/**
 * This interface is used by client processes to implement alternative login
 * mechanisms aside from the standard username and password. Implementations may
 * synchronize Agents with an external user library, such as Active Directory.
 * Implementations are expected to set up the Gateway process and its
 * authenticated components itself.
 */
public interface ProxyLogin {

    /**
     * Intialiase the connection with the system used for authentication
     * 
     * @param props Properties needed for the initialisation
     * @throws Exception
     */
    public void initialize(Properties props) throws Exception;

    /**
     * Authenticate the Agent
     * 
     * @param resource additional data required by the system used for authentication
     * @return the authenticated Agent
     * @throws Exception
     */
    public AgentProxy authenticate(String resource) throws Exception;
}
