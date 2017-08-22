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

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.MemoryOnlyClusterStorage;
import org.junit.Before;
import org.junit.Test;

import static org.cristalise.kernel.persistency.ClusterType.PATH;

public class LookupPathClusterStorageTest {

    String ior = "IOR:005858580000001549444C3A69646C746573742F746573743A312E3000585858"+
                 "0000000100000000000000350001005800000006636F726261009B44000000214F52"+
                 "424C696E6B3A3A636F7262613A33393734383A3A736B656C65746F6E202330";

    ItemPath storingItem = new ItemPath();
    MemoryOnlyClusterStorage inMemoryCluster = new MemoryOnlyClusterStorage();

    @Before
    public void setup() throws Exception {
        Logger.addLogStream(System.out, 9);
        inMemoryCluster.open(null);
    }

    @Test
    public void storeItemPath() throws Exception {
        ItemPath item = new ItemPath(UUID.randomUUID(), ior);

        inMemoryCluster.put(storingItem, item);

        ItemPath itemPrime = (ItemPath) inMemoryCluster.get(storingItem, PATH + "/Item");

        assertNotNull(itemPrime);
        assertEquals(item.getUUID(),      itemPrime.getUUID());
        assertEquals(item.getIORString(), itemPrime.getIORString());
    }

    @Test
    public void storeAgentPath() throws Exception {
        AgentPath agent = new AgentPath(UUID.randomUUID(), ior, "toto");

        inMemoryCluster.put(storingItem, agent);

        AgentPath agentPrime = (AgentPath) inMemoryCluster.get(storingItem, PATH + "/Agent");

        assertNotNull(agentPrime);
        assertEquals(agent.getUUID(),      agentPrime.getUUID());
        assertEquals(agent.getIORString(), agentPrime.getIORString());
        assertEquals(agent.getAgentName(), agentPrime.getAgentName());
    }

    @Test
    public void storeDomainPath() throws Exception {
        DomainPath domain = new DomainPath("/my/path.2", new ItemPath());

        inMemoryCluster.put(storingItem, domain);
        
        String name = StringUtils.remove( StringUtils.join(domain.getPath(), ""), "." );

        DomainPath domainPrime = (DomainPath) inMemoryCluster.get(storingItem, PATH + "/Domain/" + name);

        assertNotNull(domainPrime);
        assertEquals(domain.getStringPath(), domainPrime.getStringPath());
        assertEquals(domain.getTargetUUID(), domainPrime.getTargetUUID());
    }

    @Test
    public void storeRolePath() throws Exception {
        RolePath role      = new RolePath("Minion", false);

        inMemoryCluster.put(storingItem, role);

        RolePath rolePrime = (RolePath) inMemoryCluster.get(storingItem, PATH + "/Role/" + StringUtils.join(role.getPath(), ""));

        assertNotNull(rolePrime);
        assertEquals(role.getStringPath(), rolePrime.getStringPath());
        assertEquals(role.hasJobList(), rolePrime.hasJobList());
    }
}
