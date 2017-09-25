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

import java.util.Properties;
import java.util.UUID;

import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.resource.Resource;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.CristalMarshaller;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.kernel.utils.MoxyXMLUtility;
import org.junit.Before;
import org.junit.Test;

public class MoxyXMLTest {

    String ior = "IOR:005858580000001549444C3A69646C746573742F746573743A312E3000585858"+
            "0000000100000000000000350001005800000006636F726261009B44000000214F52"+
            "424C696E6B3A3A636F7262613A33393734383A3A736B656C65746F6E202330";

    CristalMarshaller marshaller;

    @Before
    public void setup() throws Exception {
        Logger.addLogStream(System.out, 0);
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());

        marshaller = new MoxyXMLUtility(new Resource(), props);
    }

    @Test
    public void testItemPath() throws Exception {
        ItemPath item      = new ItemPath(UUID.randomUUID(), ior);
        ItemPath itemPrime = (ItemPath) marshaller.unmarshall(marshaller.marshall(item));

        assertEquals( item.getUUID(),      itemPrime.getUUID());
        assertEquals( item.getIORString(), itemPrime.getIORString());

        Logger.msg(marshaller.marshall(itemPrime));
        Logger.msg(marshaller.marshallToJson(itemPrime));
    }

    @Test
    public void testAgentPath() throws Exception {
        AgentPath agent      = new AgentPath(UUID.randomUUID(), ior, "toto");
        AgentPath agentPrime = (AgentPath) marshaller.unmarshall(marshaller.marshall(agent));

        assertEquals( agent.getUUID(),      agentPrime.getUUID());
        assertEquals( agent.getIORString(), agentPrime.getIORString());
        assertEquals( agent.getAgentName(), agentPrime.getAgentName());

        Logger.msg(marshaller.marshall(agentPrime));
        Logger.msg(marshaller.marshallToJson(agentPrime));
    }

    @Test
    public void testDomainPath_Context() throws Exception {
        DomainPath domain      = new DomainPath("/domain/path");
        DomainPath domainPrime = (DomainPath) marshaller.unmarshall(marshaller.marshall(domain));

        assertEquals(domain.getStringPath(), domainPrime.getStringPath());

        Logger.msg(marshaller.marshall(domainPrime));
        Logger.msg(marshaller.marshallToJson(domainPrime));
    }

    @Test
    public void testDomainPath_WithTarget() throws Exception {
        DomainPath domain      = new DomainPath("/domain/path", new ItemPath());
        DomainPath domainPrime = (DomainPath) marshaller.unmarshall(marshaller.marshall(domain));

        assertEquals(domain.getStringPath(), domainPrime.getStringPath());
        assertEquals(domain.getTargetUUID(), domainPrime.getTargetUUID());

        Logger.msg(marshaller.marshall(domainPrime));
        Logger.msg(marshaller.marshallToJson(domainPrime));
    }

    @Test
    public void testRolePath() throws Exception {
        RolePath role      = new RolePath("Minion", false);
        RolePath rolePrime = (RolePath) marshaller.unmarshall(marshaller.marshall(role));

        assertEquals(role.getStringPath(), rolePrime.getStringPath());
        assertEquals(role.hasJobList(), rolePrime.hasJobList());

        Logger.msg(marshaller.marshall(rolePrime));
        Logger.msg(marshaller.marshallToJson(rolePrime));
    }
}
