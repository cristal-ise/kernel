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

//Java
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.transform.Result;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.resource.ResourceLoader;
import org.cristalise.kernel.querying.Query;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.XMLContext;

/**************************************************************************
 * Loads all castor mapfiles, and wraps marshalling/unmarshalling
 **************************************************************************/
public class CastorXMLUtility {

    public static final String CASTOR_XML_SERIALIZER_FACTORY = "org.exolab.castor.xml.serializer.factory";
    
    private XMLContext mappingContext;

    /**
     * Looks for a file called 'index' at the given URL, and loads every file listed in there by relative path
     * 
     * @param aResourceLoader
     *            the resource loader able to return the right class loader
     * @param aAppProperties
     *            the application properties containing optional castor configuration
     * @param mapURL
     *            the root URL for the mapfiles
     * @throws InvalidDataException
     */
    public CastorXMLUtility(final ResourceLoader aResourceLoader, final Properties aAppProperties, final URL mapURL)
            throws InvalidDataException
    {
        // load index
        Logger.msg(3, String.format("CastorXMLUtility.<init> Loading maps from [%s]", mapURL));
        String index;
        try {
            index = FileStringUtility.url2String(new URL(mapURL, "index"));
        }
        catch (Exception e) {
            throw new InvalidDataException(String.format("Could not load map index from [%s]", mapURL));
        }

        // retrieve the class loader of the class "CastorXMLUtility"
        ClassLoader defaultClassLoader = aResourceLoader.getClassLoader(CastorXMLUtility.class.getName());

        Logger.msg(3, String.format("CastorXMLUtility.<init>: defaultClassLoader=[%s]", defaultClassLoader));

        StringTokenizer sTokenizer = new StringTokenizer(index);
        int wNbMap = sTokenizer.countTokens();

        // init the castor mapping using the classloader of this class
        Mapping thisMapping = new Mapping(defaultClassLoader);
        HashSet<String> loadedMapURLs = new HashSet<String>();
        try {
            int wMapIdx = 0;
            while (sTokenizer.hasMoreTokens()) {
                String thisMap = sTokenizer.nextToken();
                String thisMapURL = new URL(mapURL, thisMap).toString();
                wMapIdx++;
                if (!loadedMapURLs.contains(thisMapURL)) {
                    Logger.msg(3, String.format("CastorXMLUtility.<init>: Adding mapping file (%d/%d):[%s]", wMapIdx, wNbMap, thisMapURL));
                    thisMapping.loadMapping(new URL(thisMapURL));
                    loadedMapURLs.add(thisMapURL);
                }
                else {
                    Logger.msg(3, "Map file already loaded:" + thisMapURL);
                }
            }

            mappingContext = new XMLContext();
            mappingContext.setClassLoader(defaultClassLoader);

            // if the aAppProperties contains castor properties then
            if (aAppProperties != null && aAppProperties.containsKey(CASTOR_XML_SERIALIZER_FACTORY)) {

                mappingContext.setProperty(CASTOR_XML_SERIALIZER_FACTORY, aAppProperties.getProperty(CASTOR_XML_SERIALIZER_FACTORY));

                Logger.msg(3, String.format("CastorXMLUtility.<init>: castor prop: %s=[%s]", CASTOR_XML_SERIALIZER_FACTORY,
                        mappingContext.getProperty(CASTOR_XML_SERIALIZER_FACTORY)));
            }

            mappingContext.addMapping(thisMapping);
        }
        catch (MappingException ex) {
            Logger.error(ex);
            throw new InvalidDataException("XML Mapping files are not valid: " + ex.getMessage());
        }
        catch (MalformedURLException ex) {
            Logger.error(ex);
            throw new InvalidDataException("Mapping file location invalid: " + ex.getMessage());
        }
        catch (IOException ex) {
            Logger.error(ex);
            throw new InvalidDataException("Could not read XML mapping files: " + ex.getMessage());
        }

        Logger.msg(1, String.format("Loaded [%d] maps from [%s]", loadedMapURLs.size(), mapURL));
    }

    /**
     * Marshalls a mapped object to xml string. The mapping must be loaded before. See updateMapping().
     * 
     * @param obj
     * @return the xml string of the marshalled object
     * @throws IOException
     * @throws MappingException
     * @throws MarshalException
     * @throws ValidationException
     */
    public String marshall(Object obj) throws IOException, MappingException, MarshalException, ValidationException {
        if (obj == null) return "<NULL/>";

        if (obj instanceof Outcome) return ((Outcome) obj).getData();

        StringWriter sWriter = new StringWriter();
        Marshaller marshaller = mappingContext.createMarshaller();
        marshaller.setWriter(sWriter);
        marshaller.setMarshalAsDocument(false);

        if (obj instanceof Query) marshaller.addProcessingInstruction(Result.PI_DISABLE_OUTPUT_ESCAPING, "");

        marshaller.marshal(obj);

        return sWriter.toString();
    }

    /**
     * Unmarshalls a mapped object from XML string. The mapping must be loaded before. See updateMapping().
     * 
     * @param data
     * @return the unmarshalled object
     * @throws IOException
     * @throws MappingException
     * @throws MarshalException
     * @throws ValidationException
     */
    public Object unmarshall(String data) throws IOException, MappingException, MarshalException, ValidationException {
        if (data.equals("<NULL/>")) return null;

        StringReader sReader = new StringReader(data);

        return mappingContext.createUnmarshaller().unmarshal(sReader);
    }
}
