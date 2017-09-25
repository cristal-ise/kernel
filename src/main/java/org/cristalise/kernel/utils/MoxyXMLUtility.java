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
package org.cristalise.kernel.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.resource.ResourceLoader;
import org.eclipse.persistence.jaxb.JAXBContextProperties;

public class MoxyXMLUtility implements CristalMarshaller {

    private JAXBContext mappingContext;

    public MoxyXMLUtility(final ResourceLoader resourceLoader, final Properties appProperties) throws InvalidDataException {
        try {
            List<String> lines = Files.readAllLines(Paths.get(this.getClass().getResource("/org/cristalise/kernel/utils/resources/moxyMapFiles/index").toURI()));

            Map<String, Object> metadataMap = new HashMap<String, Object>();

            StringBuffer contextPath = new StringBuffer();

            for (String line : lines) {
                String[] values = line.split(",");

                String packageName = values[0];
                String mapFileName = values[1];

                if (contextPath.length() != 0) contextPath.append(":");
                contextPath.append(packageName);

                metadataMap.put(packageName, new InputStreamReader(this.getClass().getResourceAsStream("/org/cristalise/kernel/utils/resources/moxyMapFiles/"+mapFileName)));
            }

            Map<String, Object> jaxbProps = new HashMap<String, Object>();

            jaxbProps.put(JAXBContextProperties.OXM_METADATA_SOURCE, metadataMap);

            mappingContext = JAXBContext.newInstance(contextPath.toString(), resourceLoader.getClassLoader(null), jaxbProps);

            jaxbProps.put("eclipselink.media-type", "application/json");
        }
        catch (JAXBException | IOException | URISyntaxException ex) {
            Logger.error(ex);
            throw new InvalidDataException(ex.getMessage());
        }
    }

    private String marshall(Object obj, boolean toJson) throws PersistencyException {
        if (obj == null)            return "<NULL/>";
        if (obj instanceof Outcome) return ((Outcome) obj).getData();

        try {
            Marshaller marshaller = mappingContext.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            if (toJson) marshaller.setProperty("eclipselink.media-type", "application/json");

            StringWriter sw = new StringWriter();
            marshaller.marshal(obj, sw);
            return sw.toString();
        }
        catch (Exception ex) {
            Logger.error(ex);
            throw new PersistencyException(ex.getMessage());
        }
    }

    private Object unmarshall(String data, boolean fromJson) throws PersistencyException {
        if (data.equals("<NULL/>")) return null;

        try {
            Unmarshaller unmarshaller = mappingContext.createUnmarshaller();

            if (fromJson) unmarshaller.setProperty("eclipselink.media-type", "application/json");

            return unmarshaller.unmarshal(new StringReader(data));
        }
        catch (Exception ex) {
            Logger.error(ex);
            throw new PersistencyException(ex.getMessage());
        }
    }

    @Override
    public String marshall(Object obj) throws PersistencyException {
        return marshall(obj, false);
    }

    @Override
    public Object unmarshall(String data) throws PersistencyException {
        return unmarshall(data, false);
    }

    @Override
    public String marshallToJson(Object obj) throws PersistencyException {
        return marshall(obj, true);
    }

    @Override
    public Object unmarshallFromJson(String data) throws PersistencyException {
        return unmarshall(data, true);
    }
}
