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
package org.cristalise.kernel.entity.imports;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.process.Gateway;


public class ImportOutcome {
	public String schema, viewname, path, data;
	public int version;
	
	public ImportOutcome() {
	}
	
	public ImportOutcome(String schema, int version, String viewname, String path) {
		super();
		this.schema = schema;
		this.version = version;
		this.viewname = viewname;
		this.path = path;
	}
	
	public String getData(String ns) throws ObjectNotFoundException {
		if (data == null)
			data = Gateway.getResource().getTextResource(ns, path);
		return data;
	}

}
