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

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.utils.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**************************************************************************
 *
 * $Revision: 1.24 $
 * $Date: 2005/06/09 13:50:10 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/


public class OutcomeValidator implements ErrorHandler {

    static SchemaValidator schemaValid = new SchemaValidator();

    Schema schema;
    javax.xml.validation.Schema xmlSchema;
    protected StringBuffer errors = null;

    public static OutcomeValidator getValidator(Schema schema) throws InvalidDataException {
    	
        if (schema.docType.equals("Schema") &&
        		schema.docVersion==0)
            return schemaValid;

         return new OutcomeValidator(schema);
    }

    protected OutcomeValidator() {
        errors = new StringBuffer();
    }

	public OutcomeValidator(Schema schema) throws InvalidDataException {
		this.schema = schema;

        if (schema.docType.equals("Schema"))
            throw new InvalidDataException("Use SchemaValidator to validate schema");

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(this);
        errors = new StringBuffer();
        Logger.msg(5, "Parsing "+schema.docType+" version "+schema.docVersion+". "+schema.schema.length()+" chars");

        

        try {
        	xmlSchema = schemaFactory.newSchema(new StreamSource(new StringReader(schema.schema)));
        } catch (SAXException ex) {
            throw new InvalidDataException("Error parsing schema: "+ex.getMessage());
        }

		if (errors.length() > 0) {
            throw new InvalidDataException("Schema error: \n"+errors.toString());
		}

	}

	public synchronized String validate(Outcome outcome) {
        if (outcome == null) return "Outcome object was null";
        Logger.msg(5, "Validating outcome no "+outcome.getID()+" as "+schema.docType+" v"+schema.docVersion);
        if (outcome.getSchemaType().equals(schema.docType)
                    && outcome.getSchemaVersion() == schema.docVersion) {
            return validate(outcome.getData());
        }
        else
            return "Outcome type and version did not match schema "+schema.docType;
    }

    public synchronized String validate(String outcome) {
        if (outcome == null) return "Outcome String was null";
        errors = new StringBuffer();
        try {
        	Validator parser = xmlSchema.newValidator();
            parser.setErrorHandler(this);

            parser.validate(new StreamSource(new StringReader(outcome)));
        } catch (Exception e) {
            return e.getMessage();
        }
        return errors.toString();
    }

    private void appendError(String level, Exception ex) {
        errors.append(level);
        String message = ex.getMessage();
        if (message == null || message.length()==0)
            message = ex.getClass().getName();
        errors.append(message);
        errors.append("\n");
    }

    /**
     * ErrorHandler for instances
     */
    @Override
	public void error(SAXParseException ex) throws SAXException {
        appendError("ERROR: ", ex);
    }

    /**
     *
     */
    @Override
	public void fatalError(SAXParseException ex) throws SAXException {
        appendError("FATAL: ", ex);
    }

    /**
     *
     */
    @Override
	public void warning(SAXParseException ex) throws SAXException {
        appendError("WARNING: ", ex);
    }
}
