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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class OutcomeTest {

    Outcome testOc;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Logger.addLogStream(System.out, 1);
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Gateway.close();
    }

    @Before
    public void setup() throws Exception {
        String ocData = FileStringUtility.url2String(OutcomeTest.class.getResource("/outcomeTest.xml"));
        testOc = new Outcome("/Outcome/Script/0/0", ocData);
    }

    private Outcome getOutcome(String fileName) throws Exception {
        return new Outcome(
                "/Outcome/Script/0/0",
                FileStringUtility.url2String(OutcomeTest.class.getResource("/"+fileName)));
    }

    @Test
    public void testAttributeAccess() {
        assertEquals("/TestOutcome/@attr0 has value attribute0", "attribute0", testOc.getAttribute("attr0"));
        assertNull  ("/TestOutcome/@attr00 deos not exists", testOc.getAttribute("attr00"));

        assertEquals("/TestOutcome/Field1/@attr1 has value 'attribute1'", "attribute1", testOc.getAttributeOfField("Field1", "attr1"));
        assertNull  ("/TestOutcome/Field1/@attr11 does not exists", testOc.getAttributeOfField("Field1", "attr11"));
    }

    @Test
    public void testFieldAccess() {
        assert "Field1contents".equals(testOc.getField("Field1")) : "getField() failed";
    }

    @Test
    public void testXPathAccess() throws Exception {
        Node field1Node = testOc.getNodeByXPath("//Field1/text()");
        assert field1Node != null : "XPath for Element query failed";
        assert field1Node.getNodeValue() != null : "Field1 node was null";
        assert field1Node.getNodeValue().equals("Field1contents") : "Incorrect value for element node through XPath";
        assert "Field1contents".equals(testOc.getFieldByXPath("//Field1")) : "getFieldByXPath failed";

        Node field1attr = testOc.getNodeByXPath("//Field1/@attr1");
        assert field1attr.getNodeValue().equals("attribute1") : "Invalid value for attribute 'attr1'";

        try {
            testOc.getFieldByXPath("//Field2");
            fail("testOc.getFieldByXPath('//Field2') shall throw InvalidDataException");
        }
        catch (InvalidDataException e) {}

        NodeList field3nodes = testOc.getNodesByXPath("//Field3");
        assert field3nodes.getLength() == 2 : "getNodesByXPath returned wrong number of nodes";
    }

    @Test
    public void testSetFieldByXPath_RemoveNode() throws Exception {
        testOc.setFieldByXPath("//Field2", null);

        assertNotNull(testOc.getNodeByXPath("//Field2"));
        assertEquals("", testOc.getField("Field2"));
        assertNotNull(testOc.getData());

        testOc.setFieldByXPath("//Field2", null, true);
        assertNull(testOc.getNodeByXPath("//Field2"));
        assertNotNull(testOc.getData());
    }

    @Test
    public void testSetFieldByXPath() throws Exception {
        testOc.setFieldByXPath("//Field1/@attr1", "attribute11");
        assertEquals("Invalid value for attribute 'attr1'", "attribute11", testOc.getAttributeOfField("Field1", "attr1"));

        testOc.setFieldByXPath("//Field2", "NewField2");
        assert "NewField2".equals(testOc.getFieldByXPath("//Field2")) : "getFieldByXPath failed to retrieve updated value";
        assert testOc.getNodeByXPath("//Field2/text()").getNodeValue() != null : "Field2 text node is null";
        assert testOc.getNodeByXPath("//Field2/text()").getNodeValue().equals("NewField2") : "Failed to setFieldByXPath for element";
    }

    @Test
    public void testRemoveFieldByXPath() throws Exception {
        testOc.removeNodeByXPath("/TestOutcome/Field1");
        assertNull(testOc.getField("Field1"));

        try {
            testOc.removeNodeByXPath("/TestOutcome/Field10");
            fail("testOc.removeNodeByXPath('/TestOutcome/Field10') shall throw InvalidDataException");
        }
        catch (InvalidDataException e) {}
    }

    @Test
    public void testValidation() throws Exception {
        String errors = testOc.validate();
        assert errors.contains("Cannot find the declaration of element 'TestOutcome'.") : "Validation failed";
    }

    @Test
    public void testComplexXpath() throws Exception {
        Outcome complexTestOc = getOutcome("complexOutcomeTest.xml");

        String slotID = complexTestOc.getNodeByXPath("/Fields/@slotID").getNodeValue();
        assertEquals("1",  slotID);

        NodeList fields = complexTestOc.getNodesByXPath("//Field");

        for(int i = 0; i < fields.getLength(); i++) {
            NodeList children = fields.item(i).getChildNodes();

            //There are actually 5 nodes, becuase of the text nodes
            assertEquals(5,  children.getLength());

            String fieldName  = "";
            String fieldValue = "";

            for(int j = 0; j < children.getLength(); j++) {
                if(children.item(j).getNodeType() == Node.ELEMENT_NODE) {
                    if      (children.item(j).getNodeName().equals("FieldName"))  fieldName  = children.item(j).getTextContent().trim();
                    else if (children.item(j).getNodeName().equals("FieldValue")) fieldValue = children.item(j).getTextContent().trim();
                }
                else  {
                    Logger.msg("testComplexXpath() - SKIPPING nodeName:"+children.item(j).getNodeName()+" nodeType:"+children.item(j).getNodeType());
                }
            }

            assertNotNull( "fieldName shall not be null", fieldName);
            assertNotNull("fieldValue shall not be null", fieldValue);

            Logger.msg("testComplexXpath() - slotID:"+slotID+" fieldName:"+fieldName+" fieldValue:"+fieldValue);
        }
    }

    private void compareRecord(Map<String,String> record) {
        assertEquals("123456789ABC", record.get("InsuranceNumber"));
        assertEquals("12/12/1999",   record.get("DateOfBirth"));
        assertEquals("male",         record.get("Gender"));
        assertEquals("85",           record.get("Weight"));
    }

    @Test
    public void testGetRecord() throws Exception {
        Outcome patient1 = getOutcome("patient1.xml");

        compareRecord(patient1.getRecord());
        compareRecord(patient1.getRecord("/PatientDetails"));
    }

    private void compareListOfRecord(List<Map<String, String>> records) {
        assertEquals(3, records.size());

        assertEquals("aaaaaaaaaaa", records.get(0).get("InsuranceNumber"));
        assertEquals("12/12/1999",  records.get(0).get("DateOfBirth"));
        assertEquals("male",        records.get(0).get("Gender"));
        assertEquals("85",          records.get(0).get("Weight"));

        assertEquals("bbbbbbbbbbbb", records.get(1).get("InsuranceNumber"));
        assertEquals("12/12/1989",   records.get(1).get("DateOfBirth"));
        assertEquals("female",       records.get(1).get("Gender"));
        assertEquals("55",           records.get(1).get("Weight"));

        assertEquals("cccccccccccc", records.get(2).get("InsuranceNumber"));
        assertEquals("12/12/1979",   records.get(2).get("DateOfBirth"));
        assertEquals("female",       records.get(2).get("Gender"));
        assertEquals("95",           records.get(2).get("Weight"));
    }

    @Test
    public void testGetAllRecords() throws Exception {
        Outcome patients = getOutcome("allPatients.xml");

        compareListOfRecord(patients.getAllRecords("/AllPatients/PatientDetails"));
    }
}
