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
package org.cristalise.kernel.lifecycle.instance.predefined;



import java.util.Arrays;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;



/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2005/11/15 15:56:38 $
 * @version $Revision: 1.28 $
 **************************************************************************/
public class RemoveC2KObject extends PredefinedStep
{
    public RemoveC2KObject()
    {
        super();
        getProperties().put("Agent Role", "Admin");
    }

	//requestdata is xmlstring
    @Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData, Object locker) throws InvalidDataException, PersistencyException {

    	String[] params = getDataList(requestData);
        if (Logger.doLog(3)) Logger.msg(3, "RemoveC2KObject: called by "+agent+" on "+item+" with parameters "+Arrays.toString(params));
        if (params.length != 1)
        	throw new InvalidDataException("RemoveC2KObject: Invalid parameters "+Arrays.toString(params));
        String path  = params[0];
        
        try
        {
            Gateway.getStorage().remove( item, path, locker );
        }
        catch( PersistencyException ex )
        {
            throw new PersistencyException("RemoveC2KObject: Error removing object '"+path+"': "+ex.getMessage());
        }
        return requestData;
    }
}
