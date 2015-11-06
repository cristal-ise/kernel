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
package org.cristalise.kernel.test.persistency;

import java.util.Properties;

import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class OutcomeTest {

    Outcome testOc;

    @Before
    public void setup() throws Exception {
        Logger.addLogStream(System.out, 1);
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
        String ocData = FileStringUtility.url2String(OutcomeTest.class.getResource("/outcomeTest.xml"));
        testOc = new Outcome("/Outcome/Script/0/0", ocData);
    }

    @Test
    public void testDOMAccess() throws Exception {
        assert "Field1contents".equals(testOc.getField("Field1")) : "getField() failed";
    }

    @Test
    public void testXPath() throws Exception {
        Node field1Node = testOc.getNodeByXPath("//Field1/text()");
        assert field1Node != null : "XPath for Element query failed";
        assert field1Node.getNodeValue() != null : "Field1 node was null";
        assert field1Node.getNodeValue().equals("Field1contents") : "Incorrect value for element node through XPath";
        assert "Field1contents".equals(testOc.getFieldByXPath("//Field1")) : "getFieldByXPath failed";
        testOc.setFieldByXPath("//Field2", "NewField2");
        assert "NewField2".equals(testOc.getFieldByXPath("//Field2")) : "getFieldByXPath failed to retrieve updated value";
        assert testOc.getNodeByXPath("//Field2/text()").getNodeValue() != null : "Field2 text node is null";
        assert testOc.getNodeByXPath("//Field2/text()").getNodeValue().equals("NewField2") : "Failed to setFieldByXPath for element";
        Node field2attr = testOc.getNodeByXPath("//Field2/@attr");
        assert field2attr.getNodeValue().equals("attribute") : "Failed to retrieve attribute value via XPath";
        NodeList field3nodes = testOc.getNodesByXPath("//Field3");
        assert field3nodes.getLength() == 2 : "getNodesByXPath returned wrong number of nodes";
    }
    
    @Test
    public void testValidation() throws Exception {
    	String errors = testOc.validate();
    	Logger.debug(errors.toString());
    	assert errors.equals("ERROR: cvc-elt.1: Cannot find the declaration of element 'TestOutcome'.");
    }
}
