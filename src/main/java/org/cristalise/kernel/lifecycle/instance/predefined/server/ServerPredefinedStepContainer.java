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
package org.cristalise.kernel.lifecycle.instance.predefined.server;

import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStepContainer;

public class ServerPredefinedStepContainer extends PredefinedStepContainer {


    @Override
    public void createChildren() {
        super.createChildren();
        serverPredInit("CreateNewItem", "Creates a new Item in this Server without description.", new CreateNewItem());
        serverPredInit("CreateNewAgent", "Creates a new Agent in this Server without description.", new CreateNewAgent());
        serverPredInit("CreateNewRole", "Creates a new Role in this Server.", new CreateNewRole());
        serverPredInit("RemoveRole", "Removes a Role from this Server.", new RemoveRole());
        serverPredInit("RemoveDomainContext", "Deletes an existing context in the domain tree, but only if empty", new RemoveDomainContext());
        serverPredInit("AddDomainContext", "Creates an empty domain context in the tree", new AddDomainContext());
    }

    public void serverPredInit(String alias, String Description, PredefinedStep act)
    {
        act.setName(alias);
        act.setType(alias);
        act.getProperties().put("Description", Description);
        act.getProperties().put("Agent Role", "Admin");
        act.setCentrePoint(new GraphPoint());
        act.setIsPredefined(true);
        addChild(act, new GraphPoint(100, 75 * ++num));
    }

}
