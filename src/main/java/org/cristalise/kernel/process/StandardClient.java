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

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

abstract public class StandardClient extends AbstractMain {
    protected AgentProxy agent = null;

    /**
     * 
     * @param agentName
     * @param agentPass
     * @param resource
     * @throws InvalidDataException
     */
    protected void login(String agentName, String agentPass, String resource) throws InvalidDataException {
        // login - try for a while in case server hasn't imported our agent yet
        for (int i=1; i < 6; i++) {
            try {
                Logger.msg("Login attempt "+i+" of 5");
                agent = Gateway.connect(agentName, agentPass, resource);
                break;
            }
            catch (Exception ex) {
                Logger.error("Could not log in.");
                Logger.error(ex);

                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException ex2) { }
            }
        }

        if(agent == null) throw new InvalidDataException("Could not login agent:"+agentName);
    }

    /**
     * CRISTAL-iSE clients should use the StateMachine information to implement application logic. This method loads the
     * required StateMachine using different cristal-ise configuration. 
     * 
     * @param propPrefix the Property Name prefix to find client specific configuration
     * @param namesSpaceDefault default value to load bootstrap file if no configuration was provided
     * @param bootfileDefault default value to load bootstrap file if no configuration was provided
     * @return the initialised StateMachine object
     * @throws InvalidDataException Missing/Incorrect configuration data
     */
    protected static StateMachine getRequiredStateMachine(String propPrefix, String namesSpaceDefault, String bootfileDefault) throws InvalidDataException  {
        if(StringUtils.isBlank(propPrefix)) throw new InvalidDataException("propertyPrefix must contain a value");

        String smName    = Gateway.getProperties().getString(propPrefix + ".StateMachine.name");
        int    smVersion = Gateway.getProperties().getInt(   propPrefix + ".StateMachine.version");

        try {
            if (StringUtils.isNotBlank(smName) && smVersion != -1) {
                return LocalObjectLoader.getStateMachine(smName, smVersion);
            }
            else {
                Logger.warning("StandardClient.getRequiredStateMachine() - SM Name and/or Version was not specified, trying to load from bootsrap resource.");

                String stateMachineNS   = Gateway.getProperties().getString(propPrefix + ".StateMachine.namespace", namesSpaceDefault);
                String stateMachinePath = Gateway.getProperties().getString(propPrefix + ".StateMachine.bootfile",  bootfileDefault);

                return (StateMachine) Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(stateMachineNS, stateMachinePath));
            }
        }
        catch(Exception e) {
            Logger.error(e);
            throw new InvalidDataException(e.getMessage());
        }
    }
}
