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
package org.cristalise.kernel.lifecycle.routingHelpers;

import javax.xml.xpath.XPathExpressionException;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;


public class ViewpointDataHelper implements DataHelper
{
    /**
     * Method get.
     * @param value
     * @return String
     * @throws Exception
     */
    /**@param value : /UUID (or . if current) /SchemaName/Viewname/Path:XPathInOutcome 
     * @throws PersistencyException */
    @Override
	public String get(ItemPath item, String dataPath, Object locker) throws InvalidDataException, PersistencyException, ObjectNotFoundException
    {
        //Syntax of search : <ViewpointPath>:<XPathinOutcome>
    	String[] paths = dataPath.split(":");
    	if (paths.length != 2)
    		throw new InvalidDataException("Invalid path: "+dataPath);
        String viewpoint = paths[0];
        String xpath = paths[1];
        
        // load viewpoint
        Viewpoint view = (Viewpoint) Gateway.getStorage().get(item, ClusterStorage.VIEWPOINT + "/" + viewpoint, locker);
        Outcome outcome = (Outcome)Gateway.getStorage().get(item, ClusterStorage.OUTCOME+"/"+view.getSchemaName()+"/"+view.getSchemaVersion()+"/"+view.getEventId(), locker);
       	try {
			return outcome.getFieldByXPath(xpath);
		} catch (XPathExpressionException e) {
			throw new InvalidDataException("Invalid XPath: "+xpath);
		}
    }
}
