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
package org.cristalise.kernel.lifecycle.instance.predefined.item;



import java.util.Iterator;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;



/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2005/11/15 15:56:38 $
 * @version $Revision: 1.10 $
 **************************************************************************/
public class Erase extends PredefinedStep
{
    public Erase()
    {
        super();
        getProperties().put("Agent Role", "Admin");
    }

	//requestdata is xmlstring
    @Override
	protected String runActivityLogic(AgentPath agent, ItemPath item,
			int transitionID, String requestData, Object locker) throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, PersistencyException {
    	
        Logger.msg(1, "Erase::request() - Starting.");

        Iterator<Path> domPaths = Gateway.getLookup().searchAliases(item);
        while (domPaths.hasNext()) {
            DomainPath path = (DomainPath)domPaths.next();
            // delete them
            if (path.getItemPath().equals(item))
                Gateway.getLookupManager().delete(path);
        }

        //clear out all storages
        Gateway.getStorage().removeCluster(item, "", locker);

        //remove entity path
        Gateway.getLookupManager().delete(item);

        Logger.msg(1, "Erase::request() - DONE.");
        return requestData;
    }

}
