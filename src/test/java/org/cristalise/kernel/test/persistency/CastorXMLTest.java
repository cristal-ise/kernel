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
package org.cristalise.kernel.test.persistency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Properties;

import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

public class CastorXMLTest {

    @Before
    public void setup() throws Exception {
        Logger.addLogStream(System.out, 6);
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
    }

    /**
     * Compares 2 XML string
     *
     * @param expected the reference XML
     * @param actual the xml under test
     * @return whether the two XMLs are identical or not
     * @throws SAXException
     * @throws IOException
     */
    public static boolean compareXML(String expected, String actual) throws SAXException, IOException {
        Diff diffIdentical = DiffBuilder.compare(expected).withTest(actual)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                .ignoreComments()
                .ignoreWhitespace()
                .checkForSimilar()
                .build();

        if(diffIdentical.hasDifferences()){
            Logger.msg(0, actual);
            Logger.warning(diffIdentical.toString());
        }

        return !diffIdentical.hasDifferences();
    }

    @Test @Ignore("Castor XML mapping is not done for Script")
    public void testScriptCDATAHandling() throws Exception {
        String origScriptXML = FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/SC/CreateNewNumberedVersionFromLast.xml"));
        String marshalledScriptXML = Gateway.getMarshaller().marshall(Gateway.getMarshaller().unmarshall(origScriptXML));

        assertTrue(compareXML(origScriptXML, marshalledScriptXML));
    }

    @Test @Ignore("Castor XML mapping is not done for Query")
    public void testQueryCDATAHandling() throws Exception {
        String origQueryXML = FileStringUtility.url2String(CastorXMLTest.class.getResource("/testQuery.xml"));
        String marshalledQueryXML = Gateway.getMarshaller().marshall(Gateway.getMarshaller().unmarshall(origQueryXML));

        assertTrue(compareXML(origQueryXML, marshalledQueryXML));
    }

    @Test 
    public void testQueryParsing() throws Exception {
        Query q = new Query(FileStringUtility.url2String(CastorXMLTest.class.getResource("/testQuery.xml")));

        assertEquals("TestQuery", q.getName());
        assertEquals((int)0, (int)q.getVersion());
        assertEquals("existdb:xquery", q.getLanguage());

        assertEquals(1, q.getParameters().size());
        assertEquals("uuid", q.getParameters().get(0).getName());
        assertEquals("java.lang.String", q.getParameters().get(0).getType().getName());

        assertTrue(q.getQuery().startsWith("\n<TRList>"));
        assertTrue(q.getQuery().endsWith("</TRList>\n    "));
    }
}
