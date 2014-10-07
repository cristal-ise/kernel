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
package org.cristalise.kernel.persistency.outcome;

import java.io.IOException;
import java.io.StringReader;

import org.exolab.castor.xml.schema.reader.SchemaReader;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

/**
 * @author Andrew Branson
 *
 * $Revision: 1.3 $
 * $Date: 2006/09/14 14:13:26 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 */

public class Schema {
    public String docType;
	public int docVersion;
	public String schema;
	public org.exolab.castor.xml.schema.Schema som;
	
	/**
	 * @param docType
	 * @param docVersion
	 * @param schema
	 */
	public Schema(String docType, int docVersion, String schema) {
		super();
		this.docType = docType;
		this.docVersion = docVersion;
		this.schema = schema;
	}
	
	public Schema(String schema) {
		this.schema = schema;
	}
	
	public org.exolab.castor.xml.schema.Schema parse(ErrorHandler errorHandler) throws IOException {
		InputSource schemaSource = new InputSource(new StringReader(schema));
        SchemaReader mySchemaReader = new SchemaReader(schemaSource);
        if (errorHandler!= null) {
        	mySchemaReader.setErrorHandler(errorHandler);
        	mySchemaReader.setValidation(true);
        }
        som = mySchemaReader.read();
        return som;
	}
	
}
