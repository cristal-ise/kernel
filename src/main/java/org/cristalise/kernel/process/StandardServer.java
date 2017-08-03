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
package org.cristalise.kernel.process;

import java.util.Iterator;
import java.util.Properties;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.resource.ResourceLoader;
import org.cristalise.kernel.utils.Logger;

/**
 * Base class for all servers i.e. c2k processes that serve Entities
 */
public class StandardServer extends AbstractMain {
    protected static StandardServer server;

    public static void resetItemIORs(DomainPath root) throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException {
        Logger.msg("StandardServer.resetItemIORs() - root:"+root);

        Iterator<Path> pathes = Gateway.getLookup().getChildren(root);

        while (pathes.hasNext()) {
            DomainPath domain = (DomainPath) pathes.next();

            if (domain.isContext()) {
                resetItemIORs(domain);
            }
            else {
                Logger.msg("StandardServer.resetItemIORs() - setting IOR for domain:" + domain + " item:" + domain.getItemPath());

                Gateway.getLookupManager().setIOR(
                        domain.getItemPath(),
                        Gateway.getORB().object_to_string(Gateway.getCorbaServer().getItemIOR(domain.getItemPath())));
            }
        }
    }

    public static void resetAgentIORs(RolePath root) throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException {
        Logger.msg("StandardServer.resetAgentIORs() - root:"+root);

        Iterator<Path> roles = Gateway.getLookup().getChildren(root);

        while (roles.hasNext()) {
            RolePath role = (RolePath) roles.next();

            resetAgentIORs(role);

            for (AgentPath agent :  Gateway.getLookup().getAgents(role)) {
                Logger.msg("StandardServer.resetAgentIORs() - setting IOR for role:" + role + " agent:" + agent.getAgentName() + " " + agent.getItemPath());

                Gateway.getLookupManager().setIOR(
                        agent.getItemPath(),
                        Gateway.getORB().object_to_string(Gateway.getCorbaServer().getAgentIOR(agent)));
            }
        }
    }

    /**
     * Initialise the server
     * 
     * @param props initiliased Properties
     * @param res the instantiated ResourceLoader
     * @throws Exception throw whatever happens
     */
    public static void standardInitialisation(Properties props, ResourceLoader res) throws Exception {
        isServer = true;

        // read args and init Gateway
        Gateway.init(props, res);

        // connect to LDAP as root
        Gateway.connect();

        //start console
        Logger.initConsole("ItemServer");

        //initialize the server objects
        Gateway.startServer();

        if (Gateway.getProperties().containsKey(AbstractMain.MAIN_ARG_RESETIOR)) {
            Logger.msg(5, "StandardServer.standardInitialisation() - RESETTING IORs");

            resetItemIORs(new DomainPath(""));
            resetAgentIORs(new RolePath());

            AbstractMain.shutdown(0);
        }
        else {
            //start checking bootstrap & module items
            Bootstrap.run();
        }

        Logger.msg(5, "StandardServer.standardInitialisation() - complete.");
    }

    /**
     * Initialise the server
     * 
     * @param args command line parameters
     * @throws Exception throw whatever happens
     */
    public static void standardInitialisation(String[] args) throws Exception {
        standardInitialisation(readC2KArgs(args), null);
    }

    /**
     * Main to launch a Standard Server process
     * 
     * @param args command line parameters
     * @throws Exception  throw whatever happens
     */
    public static void main(String[] args) throws Exception {
        //initialise everything
        standardInitialisation( args );

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                AbstractMain.shutdown(0);
            }
        });
    }
}
