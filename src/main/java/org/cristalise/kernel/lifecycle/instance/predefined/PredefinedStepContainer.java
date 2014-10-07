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
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
public abstract class PredefinedStepContainer extends CompositeActivity
{
	protected int num = 0;

	public PredefinedStepContainer()
	{
		super();
		setName("predefined");
		getProperties().put("Description", "Contains all predefined Steps");
		createChildren();
	}
	public void createChildren()
	{
		predInit("AddDomainPath", "Adds a new path to this entity in the LDAP domain tree", new AddDomainPath());
        predInit("RemoveDomainPath", "Removes an existing path to this Entity from the LDAP domain tree", new RemoveDomainPath());
        predInit("ReplaceDomainWorkflow", "Replaces the domain CA with the supplied one. Used by the GUI to save new Wf layout", new ReplaceDomainWorkflow());
        predInit("AddC2KObject", "Adds or overwrites a C2Kernel object for this Item", new AddC2KObject());
        predInit("RemoveC2KObject", "Removes the named C2Kernel object from this Item.", new RemoveC2KObject());
        predInit("WriteProperty", "Writes a property to the Item", new WriteProperty());
        predInit("WriteViewpoint", "Writes a viewpoint to the Item", new WriteViewpoint());
        predInit("AddNewCollectionDescription", "Creates a new collection description in this Item", new AddNewCollectionDescription());
        predInit("CreateNewCollectionVersion", "Creates a new numbered collection version in this Item from the current one.", new CreateNewCollectionVersion());
        predInit("AddNewSlot", "Creates a new slot in the given aggregation, that holds instances of the item description of the given key", new AddNewSlot());
        predInit("AssignItemToSlot", "Assigns the referenced entity to a pre-existing slot in an aggregation", new AssignItemToSlot());
        predInit("ClearSlot", "Clears an aggregation member slot, given a slot no or entity key", new ClearSlot());
        predInit("RemoveSlotFromCollection", "Removed the given slot from the aggregation", new RemoveSlotFromCollection());
        predInit("AddMemberToCollection", "Creates a new member slot for the given item in a dependency, and assigns the item", new AddMemberToCollection());
        predInit("Import", "Imports an outcome into the Item, with a given schema and viewpoint", new Import());

	}

	public void predInit(String alias, String Description, PredefinedStep act)
	{
		act.setName(alias);
		act.setType(alias);
		act.getProperties().put("Description", Description);
		act.setCentrePoint(new GraphPoint());
		act.setIsPredefined(true);
		addChild(act, new GraphPoint(100, 75 * ++num));
	}
	@Override
	public boolean verify()
	{
		return true;
	}
	@Override
	public String getErrors()
	{
		return "predefined";
	}
	@Override
	public boolean getActive()
	{
		return true;
	}
}
