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
package org.cristalise.kernel.process;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.utils.Logger;


abstract public class StandardClient extends AbstractMain {
    protected AgentProxy agent = null;
    
	protected void login(String agentName, String agentPass, String resource) throws InvalidDataException {
		// login - try for a while in case server hasn't imported our user yet
        for (int i=1; i < 6; i++) {
            try {
                Logger.msg("Login attempt "+i+" of 5");
                agent = Gateway.connect(agentName, agentPass, resource);
                break;
            }
            catch (Exception ex) {
                Logger.error("Could not log in.");
                Logger.error(ex);

                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException ex2) { }
            }
        }
        
        if(agent == null) throw new InvalidDataException("Could not login agent:"+agentName);
	}

    /**
     * This method is only provided as an example
     * 
     * @param args
     * @throws Exception
     */
    static public void main(String[] args) throws Exception {
        Gateway.init(readC2KArgs(args));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                AbstractMain.shutdown(0);
            }
        });
        Gateway.connect("username", "password");
    }
}
