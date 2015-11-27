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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;
import org.exolab.castor.xml.schema.reader.SchemaReader;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Andrew Branson
 *
 * $Revision: 1.3 $
 * $Date: 2006/09/14 14:13:26 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 */

public class Schema implements DescriptionObject, ErrorHandler {
    private String name;
    private Integer version;
    private final String schemaData;
    private ItemPath itemPath;
	protected StringBuffer errors = null;
	
	public org.exolab.castor.xml.schema.Schema som;
	
	/**
	 * @param docType
	 * @param docVersion
	 * @param schema
	 */
	public Schema(String name, int version, ItemPath itemPath, String schema) {
		super();
		this.name = name;
		this.version = version;
		this.itemPath = itemPath;
		this.schemaData = schema;
	}
	
	protected Schema(String schema) {
		this.schemaData = schema;
		name = "Schema";
		version = 0;
	}
	
	public synchronized String validate() throws IOException {
		errors = new StringBuffer();
		InputSource schemaSource = new InputSource(new StringReader(schemaData));
        SchemaReader mySchemaReader = new SchemaReader(schemaSource);

        mySchemaReader.setErrorHandler(this);
        mySchemaReader.setValidation(true);

        som = mySchemaReader.read();
        return errors.toString();
	}
	
	@Override
	public String getItemID() {
		return itemPath.getUUID().toString();
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Integer getVersion() {
		return version;
	}
	
	public String getSchemaData() {
		return schemaData;
	}

	@Override
	public ItemPath getItemPath() {
		return itemPath;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setVersion(Integer version) {
		this.version = version;
	}

	@Override
	public void setItemPath(ItemPath path) {
		itemPath = path;
	}

    /**
     * ErrorHandler for validation
     */
    private void appendError(String level, Exception ex) {
        errors.append(level);
        String message = ex.getMessage();
        if (message == null || message.length()==0)
            message = ex.getClass().getName();
        errors.append(message);
        errors.append("\n");
    }
    @Override
	public void error(SAXParseException ex) throws SAXException {
        appendError("ERROR: ", ex);
    }
    @Override
	public void fatalError(SAXParseException ex) throws SAXException {
        appendError("FATAL: ", ex);
    }
    @Override
	public void warning(SAXParseException ex) throws SAXException {
        appendError("WARNING: ", ex);
    }

	@Override
	public CollectionArrayList makeDescCollections() {
		return new CollectionArrayList();
	}
	
	@Override
	public void export(Writer imports, File dir) throws IOException {
		FileStringUtility.string2File(new File(new File(dir, "OD"), getName()+(getVersion()==null?"":"_"+getVersion())+".xml"), schemaData);
		if (imports!=null) imports.write("<Resource name=\""+getName()+"\" "
				+(getItemPath()==null?"":"id=\""+getItemID()+"\"")
				+(getVersion()==null?"":"version=\""+getVersion()+"\" ")
				+"type=\"OD\">boot/OD/"+getName()
				+(getVersion()==null?"":"_"+getVersion())
				+".xsd</Resource>\n");
	}
}
