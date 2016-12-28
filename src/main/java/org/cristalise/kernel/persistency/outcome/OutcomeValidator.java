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
package org.cristalise.kernel.persistency.outcome;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.utils.Logger;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class OutcomeValidator implements ErrorHandler {

    static SchemaValidator schemaValid = new SchemaValidator();

    Schema                      schema;
    javax.xml.validation.Schema xmlSchema;
    protected StringBuffer      errors = null;

    public static OutcomeValidator getValidator(Outcome o) throws InvalidDataException, ObjectNotFoundException {
        Schema schema = o.getSchema();
        return getValidator(schema);
    }

    public static OutcomeValidator getValidator(Schema schema) throws InvalidDataException {
        if (schema.getName().equals("Schema") && schema.getVersion() == 0) return schemaValid;

        return new OutcomeValidator(schema);
    }

    protected OutcomeValidator() {
        errors = new StringBuffer();
    }

    public OutcomeValidator(Schema schema) throws InvalidDataException {
        this.schema = schema;

        if (schema.getName().equals("Schema")) throw new InvalidDataException("Use SchemaValidator to validate schema");

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setErrorHandler(this);

        errors = new StringBuffer();

        Logger.msg(5, "OutcomeValidator() - Parsing "+schema.getName()+" version "+schema.getVersion()+". "+schema.getSchemaData().length()+" chars");

        try {
            xmlSchema = schemaFactory.newSchema(new StreamSource(new StringReader(schema.getSchemaData())));
        }
        catch (SAXException ex) {
            throw new InvalidDataException("Error parsing schema: " + ex.getMessage());
        }

        if (errors.length() > 0) {
            throw new InvalidDataException("Schema error: \n" + errors.toString());
        }

    }

    private synchronized String validate(Source outcome) {
        errors = new StringBuffer();
        try {
            Validator parser = xmlSchema.newValidator();
            parser.setErrorHandler(this);
            parser.validate(outcome);
        }
        catch (SAXException | IOException e) {
            return "Couldn't create outcome validator:" + e.getMessage();
        }

        return errors.toString();
    }

    public String validate(String outcome) {
        if (StringUtils.isBlank(outcome)) return "XML string was null or blank";

        return validate(new StreamSource(new StringReader(outcome)));
    }

    public String validate(Document outcome) {
        if (outcome == null) return "XML Document was null";

        return validate(new DOMSource(outcome));
    }

    private void appendError(String level, Exception ex) {
        errors.append("level:" + level);

        if (StringUtils.isNotBlank(ex.getMessage())) errors.append(" msg:" + ex.getMessage());

        errors.append("\n");
    }

    /**
     * ErrorHandler for instances
     */
    @Override
    public void error(SAXParseException ex) throws SAXException {
        appendError("ERROR", ex);
    }

    /**
     *
     */
    @Override
    public void fatalError(SAXParseException ex) throws SAXException {
        appendError("FATAL", ex);
    }

    /**
     *
     */
    @Override
    public void warning(SAXParseException ex) throws SAXException {
        appendError("WARNING", ex);
    }
}
