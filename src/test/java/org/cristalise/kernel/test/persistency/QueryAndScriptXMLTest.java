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

import java.util.Properties;

import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.CristalMarshaller;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

public class QueryAndScriptXMLTest {

    String ior = "IOR:005858580000001549444C3A69646C746573742F746573743A312E3000585858"+
            "0000000100000000000000350001005800000006636F726261009B44000000214F52"+
            "424C696E6B3A3A636F7262613A33393734383A3A736B656C65746F6E202330";

    CristalMarshaller marshaller;

    @Before
    public void setup() throws Exception {
        Logger.addLogStream(System.out, 6);
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
        marshaller = Gateway.getMarshaller();
    }

    /**
     * Compares 2 XML string
     *
     * @param expected the reference XML
     * @param actual the xml under test
     * @return whether the two XMLs are identical or not
     */
    public static boolean compareXML(String expected, String actual)  {
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

    @Test @Ignore("Castor XML mapping cannot be done for Script")
    public void testScriptCDATAHandling() throws Exception {
        String origScriptXML = FileStringUtility.url2String(Gateway.getResource().getKernelResourceURL("boot/SC/CreateNewNumberedVersionFromLast.xml"));
        String marshalledScriptXML = Gateway.getMarshaller().marshall(Gateway.getMarshaller().unmarshall(origScriptXML));

        assertTrue(compareXML(origScriptXML, marshalledScriptXML));
    }

    @Test @Ignore("Castor XML mapping cannot be done for Query")
    public void testQueryCDATAHandling() throws Exception {
        String origQueryXML = FileStringUtility.url2String(QueryAndScriptXMLTest.class.getResource("/testQuery.xml"));
        String marshalledQueryXML = Gateway.getMarshaller().marshall(Gateway.getMarshaller().unmarshall(origQueryXML));

        assertTrue(compareXML(origQueryXML, marshalledQueryXML));
    }

    @Test
    public void testQueryParsing() throws Exception {
        Query q = new Query(FileStringUtility.url2String(QueryAndScriptXMLTest.class.getResource("/testQuery.xml")));

        assertEquals("TestQuery", q.getName());
        assertEquals(0, (int)q.getVersion());
        assertEquals("existdb:xquery", q.getLanguage());

        assertEquals(1, q.getParameters().size());
        assertEquals("uuid", q.getParameters().get(0).getName());
        assertEquals("java.lang.String", q.getParameters().get(0).getType().getName());

        assertTrue(q.getQuery().startsWith("\n<TRList>"));
        assertTrue(q.getQuery().endsWith("</TRList>\n    "));
    }
}
