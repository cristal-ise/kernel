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

import static org.assertj.core.api.Assertions.assertThat;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;

import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.GraphableEdge;
import org.cristalise.kernel.lifecycle.instance.Next;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyDescription;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.CastorXMLUtility;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

public class CastorXMLTest {

    String ior = "IOR:005858580000001549444C3A69646C746573742F746573743A312E3000585858"+
            "0000000100000000000000350001005800000006636F726261009B44000000214F52"+
            "424C696E6B3A3A636F7262613A33393734383A3A736B656C65746F6E202330";

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
        assertEquals(0, (int)q.getVersion());
        assertEquals("existdb:xquery", q.getLanguage());

        assertEquals(1, q.getParameters().size());
        assertEquals("uuid", q.getParameters().get(0).getName());
        assertEquals("java.lang.String", q.getParameters().get(0).getType().getName());

        assertTrue(q.getQuery().startsWith("\n<TRList>"));
        assertTrue(q.getQuery().endsWith("</TRList>\n    "));
    }

    @Test
    public void testCastorItemPath() throws Exception {
        CastorXMLUtility marshaller = Gateway.getMarshaller();

        ItemPath item      = new ItemPath(UUID.randomUUID(), ior);
        ItemPath itemPrime = (ItemPath) marshaller.unmarshall(marshaller.marshall(item));

        assertEquals( item.getUUID(),      itemPrime.getUUID());
        assertEquals( item.getIORString(), itemPrime.getIORString());

        Logger.msg(marshaller.marshall(itemPrime));
    }

    @Test
    public void testCastorAgentPath() throws Exception {
        CastorXMLUtility marshaller = Gateway.getMarshaller();

        AgentPath agent      = new AgentPath(UUID.randomUUID(), ior, "toto");
        AgentPath agentPrime = (AgentPath) marshaller.unmarshall(marshaller.marshall(agent));

        assertEquals( agent.getUUID(),      agentPrime.getUUID());
        assertEquals( agent.getIORString(), agentPrime.getIORString());
        assertEquals( agent.getAgentName(), agentPrime.getAgentName());

        Logger.msg(marshaller.marshall(agentPrime));
    }

    @Test
    public void testCastorDomainPath_Context() throws Exception {
        CastorXMLUtility marshaller = Gateway.getMarshaller();

        DomainPath domain      = new DomainPath("/domain/path");
        DomainPath domainPrime = (DomainPath) marshaller.unmarshall(marshaller.marshall(domain));

        assertEquals( domain.getStringPath(), domainPrime.getStringPath());

        Logger.msg(marshaller.marshall(domainPrime));
    }

    @Test
    public void testCastorDomainPath_WithTarget() throws Exception {
        CastorXMLUtility marshaller = Gateway.getMarshaller();

        DomainPath domain      = new DomainPath("/domain/path", new ItemPath());
        DomainPath domainPrime = (DomainPath) marshaller.unmarshall(marshaller.marshall(domain));

        assertEquals( domain.getStringPath(), domainPrime.getStringPath());
        assertEquals( domain.getTargetUUID(), domainPrime.getTargetUUID());

        Logger.msg(marshaller.marshall(domainPrime));
    }

    @Test
    public void testCastorRolePath() throws Exception {
        CastorXMLUtility marshaller = Gateway.getMarshaller();

        RolePath role      = new RolePath("Minion", false, Arrays.asList("permission1", "permission2")) ;
        RolePath rolePrime = (RolePath) marshaller.unmarshall(marshaller.marshall(role));

        assertEquals(role.getStringPath(), rolePrime.getStringPath());
        assertEquals(role.hasJobList(),    rolePrime.hasJobList());

        assertThat(role.getPermissions(), IsIterableContainingInAnyOrder.containsInAnyOrder(rolePrime.getPermissions().toArray()));

        Logger.msg(marshaller.marshall(rolePrime));
    }

    @Test
    public void testCastorErrorInfo() throws Exception {
        CastorXMLUtility marshaller = Gateway.getMarshaller();

        try {
            //Create exception for testing ErrorInfo
            "".substring(5);
        }
        catch (Exception ex) {
            Transition t = new Transition(1, "Done", 0, 3);
            CastorHashMap actProps = new CastorHashMap();
            actProps.setBuiltInProperty(STATE_MACHINE_NAME, "Default");
            actProps.setBuiltInProperty(STATE_MACHINE_VERSION, 0);
            Job j = new Job(-1, new ItemPath(), "TestStep", "workflow/1", "", t, "Waiting", "Finished", "Admin", new AgentPath(), null, actProps, new GTimeStamp());

            ErrorInfo ei = new ErrorInfo(j, ex);
            ErrorInfo eiPrime = (ErrorInfo) marshaller.unmarshall(marshaller.marshall(ei));

            assertThat(ei).isEqualToComparingFieldByField(eiPrime);

            Outcome errors = new Outcome("/Outcome/Errors/0/0", marshaller.marshall(ei));
            errors.validateAndCheck();
        }
    }

    @Test
    public void testGraphMultiPointEdge() throws Exception {
        CastorXMLUtility marshaller = Gateway.getMarshaller();

        GraphableEdge edge = new Next();

        edge.getMultiPoints().put(1, new GraphPoint(0, 0));
        edge.getMultiPoints().put(2, new GraphPoint(100, 100));
        edge.getMultiPoints().put(3, new GraphPoint(200, 100));

        GraphableEdge edgePrime = (GraphableEdge) marshaller.unmarshall(marshaller.marshall(edge));

        assertThat(edge).isEqualToComparingFieldByField(edgePrime);

        Logger.msg(marshaller.marshall(edge));
        Logger.msg(marshaller.marshall(edgePrime));
    }

    @Test
    public void testPropertyDescriptionList() throws Exception {
        CastorXMLUtility marshaller = Gateway.getMarshaller();

        PropertyDescriptionList pdl = new PropertyDescriptionList();
        pdl.list.add(new PropertyDescription("Name", "", false, true, false));
        pdl.list.add(new PropertyDescription("Type", "Item", true, false, true));

        PropertyDescriptionList pdlPrime = (PropertyDescriptionList) marshaller.unmarshall(marshaller.marshall(pdl));

        assertReflectionEquals(pdl, pdlPrime, LENIENT_ORDER);
    }
}
