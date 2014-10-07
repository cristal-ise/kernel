/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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

import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.utils.Logger;

/**************************************************************************
 * Base class for all servers i.e. c2k processes that serve Entities
 *
 * @author $Author: abranson $ $Date: 2005/04/28 13:49:43 $
 * @version $Revision: 1.47 $
 **************************************************************************/
public class StandardServer extends AbstractMain
{
    protected static StandardServer server;

   /**************************************************************************
    * void StandardInitalisation( String[] )
    *
    * Set-up calls to ORB, POA and Factorys, both optional and required.
    **************************************************************************/
    protected static void standardInitialisation( String[] args )
        throws Exception
    {
    	isServer = true;
    	
        // read args and init Gateway
    	Gateway.init(readC2KArgs(args));

        // connect to LDAP as root
        Authenticator auth = Gateway.connect();

        //start console
        Logger.initConsole("ItemServer");

        //initialize the server objects
        Gateway.startServer(auth);

        Logger.msg(5, "StandardServer::standardInitialisation - complete.");

    }

	public static void main(String[] args) throws Exception 
    {
		//initialise everything
        standardInitialisation( args );
    }
}
