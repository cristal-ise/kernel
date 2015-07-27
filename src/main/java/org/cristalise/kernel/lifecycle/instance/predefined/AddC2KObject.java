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
package org.cristalise.kernel.lifecycle.instance.predefined;

import java.util.Arrays;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2004/10/21 08:02:19 $
 * @version $Revision: 1.31 $
 **************************************************************************/
public class AddC2KObject extends PredefinedStep
{
   /**************************************************************************
    * Constructor for Castror
    **************************************************************************/
    public AddC2KObject()
    {
        super();
        getProperties().put("Agent Role", "Admin");
    }

	//requestdata is xmlstring
    @Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData, Object locker) throws InvalidDataException, PersistencyException {

    	String[] params = getDataList(requestData);
        if (Logger.doLog(3)) Logger.msg(3, "AddC2KObject: called by "+agent+" on "+item+" with parameters "+Arrays.toString(params));
        if (params.length != 1) throw new InvalidDataException("AddC2KObject: Invalid parameters "+Arrays.toString(params));
		C2KLocalObject obj;
		try {
			obj = (C2KLocalObject)Gateway.getMarshaller().unmarshall(params[0]);
		} catch (Exception e) {
			throw new InvalidDataException("AddC2KObject: Could not unmarshall new object: "+params[0]);
		}
		Gateway.getStorage().put(item, obj, locker);
		return requestData;
    }
}