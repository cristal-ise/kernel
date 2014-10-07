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
package org.cristalise.kernel.process.module;

import java.util.ArrayList;

import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.imports.ImportItem;
import org.cristalise.kernel.entity.imports.ImportRole;
import org.cristalise.kernel.utils.CastorArrayList;


public class ModuleImports extends CastorArrayList<ModuleImport> {

    public ModuleImports()
    {
        super();
    }

    public ModuleImports(ArrayList<ModuleImport> aList)
    {
        super(aList);
    }
    
    public ArrayList<ModuleResource> getResources() {
    	ArrayList<ModuleResource> subset = new ArrayList<ModuleResource>();
    	for (ModuleImport imp : list) {
			if (imp instanceof ModuleResource)
				subset.add((ModuleResource)imp);
		}
    	return subset;
    }
    
    public ArrayList<ImportItem> getItems() {
    	ArrayList<ImportItem> subset = new ArrayList<ImportItem>();
    	for (ModuleImport imp : list) {
			if (imp instanceof ImportItem)
				subset.add((ImportItem)imp);
		}
    	return subset;
    }
    
    public ArrayList<ImportAgent> getAgents() {
    	ArrayList<ImportAgent> subset = new ArrayList<ImportAgent>();
    	for (ModuleImport imp : list) {
			if (imp instanceof ImportAgent)
				subset.add((ImportAgent)imp);
		}
    	return subset;
    }
    
    public ArrayList<ImportRole> getRoles() {
    	ArrayList<ImportRole> subset = new ArrayList<ImportRole>();
    	for (ModuleImport imp : list) {
			if (imp instanceof ImportRole)
				subset.add((ImportRole)imp);
		}
    	return subset;
    }
}
