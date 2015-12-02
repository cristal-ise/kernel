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
package org.cristalise.kernel.entity.agent;


import java.nio.ByteBuffer;
import java.sql.Timestamp;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;



/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2005/10/05 07:39:36 $
 * @version $Revision: 1.9 $
 **************************************************************************/
public class ActiveLocator extends org.omg.PortableServer.ServantLocatorPOA
{

   /**************************************************************************
    *
    **************************************************************************/
    public ActiveLocator()
    {
    }


   /**************************************************************************
    *
    **************************************************************************/
    @Override
	public org.omg.PortableServer.Servant preinvoke(
              byte[]                                                    oid,
              org.omg.PortableServer.POA                                poa,
              String                                                    operation,
              org.omg.PortableServer.ServantLocatorPackage.CookieHolder cookie )
    {

        try
        {
        	ByteBuffer bb = ByteBuffer.wrap(oid);
        	long msb = bb.getLong();
        	long lsb = bb.getLong();
        	AgentPath syskey = new AgentPath(new SystemKey(msb, lsb));

            Logger.msg(1,"===========================================================");
            Logger.msg(1,"Agent called at "+new Timestamp( System.currentTimeMillis()) +": " + operation +
                    "(" + syskey + ")." );

            return Gateway.getCorbaServer().getAgent(syskey);

        }
        catch (ObjectNotFoundException ex)
        {
            Logger.error("ObjectNotFoundException::ActiveLocator::preinvoke() "+ex.toString());
            throw new org.omg.CORBA.OBJECT_NOT_EXIST();
        } catch (InvalidItemPathException ex) {
        	Logger.error("InvalidItemPathException::ActiveLocator::preinvoke() "+ex.toString());
        	throw new org.omg.CORBA.INV_OBJREF();
		}
    }


   /**************************************************************************
    *
    **************************************************************************/
    @Override
	public void postinvoke(
               byte[]                           oid,
               org.omg.PortableServer.POA       poa,
               String                           operation,
               java.lang.Object                 the_cookie,
               org.omg.PortableServer.Servant   the_servant )
    {
    }
}
