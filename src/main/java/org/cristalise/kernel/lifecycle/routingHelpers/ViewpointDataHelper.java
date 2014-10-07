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

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.XmlElementParser;


public class ViewpointDataHelper
{
    static Object[] errArr = { "" };
    /**
     * Method get.
     * @param value
     * @return String[]
     * @throws Exception
     */
    /**@param value : /UUID (or . if current) /SchemaName/Viewname/Path:XPathInOutcome */
    public static Object [] get(String value) throws Exception
    {
        //Syntax of search : <EntityPath>/<ViewpointPath>:<XPathinOutcome>
        String entityPath;
        String viewpoint;
        String xpath;
        Object[] retArr;

        // find syskey, viewname, xpath
        int firstSlash = value.indexOf("/");
        if (firstSlash > 0) {
            entityPath = value.substring(0, firstSlash);
            int startXPath = value.indexOf(":");
            if (startXPath==-1) {
                viewpoint = value.substring(firstSlash + 1);
                xpath = null;
            } else {
                viewpoint = value.substring(firstSlash + 1, startXPath);
                xpath = value.substring(startXPath+1);
            }
        }
        else return errArr;

        // find entity
        ItemPath sourcePath = new ItemPath(entityPath);

        try {
            // load viewpoint
            ItemProxy dataSource = Gateway.getProxyManager().getProxy(sourcePath);
            Viewpoint view = (Viewpoint)dataSource.getObject(ClusterStorage.VIEWPOINT + "/" + viewpoint);
            Outcome outcome = view.getOutcome();
            if (xpath == null) {
                retArr = new Object[1];
                retArr[0] = outcome;
            }
            else
                retArr = XmlElementParser.parse(outcome.getData(), xpath);
            return retArr;

        } catch (ObjectNotFoundException e) {
            return errArr;
        }
    }
}
