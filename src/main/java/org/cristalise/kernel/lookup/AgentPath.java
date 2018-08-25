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
package org.cristalise.kernel.lookup;

import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;

/**
 * Extends ItemPath with Agent specific codes
 **/
public class AgentPath extends ItemPath {

    private String mAgentName = null;
    private boolean mPasswordTemporary = false;

    public AgentPath() {
        super();
    }

    public AgentPath(UUID uuid, String ior, String agentName) {
        super(uuid, ior);
        mAgentName = agentName;
    }

    public AgentPath(UUID uuid, String ior, String agentName, boolean isPwdTemporary) {
        super(uuid, ior);
        mAgentName = agentName;
        mPasswordTemporary = isPwdTemporary;
    }

    public AgentPath(UUID uuid) throws InvalidAgentPathException {
        super(uuid);

        //This is commented so a AgentPath can be constructed without setting up Lookup
        //if (getAgentName() == null) throw new InvalidAgentPathException();
    }

    public AgentPath(SystemKey syskey) throws InvalidAgentPathException {
        this(new UUID(syskey.msb, syskey.lsb));
    }

    public AgentPath(ItemPath itemPath) throws InvalidAgentPathException {
        this(itemPath.getUUID());
    }

    public AgentPath(String path) throws InvalidItemPathException {
        //remove the '/entity/' string from the beginning if exists
        this(UUID.fromString(path.substring( (path.lastIndexOf("/") == -1 ? 0 : path.lastIndexOf("/")+1) )));
    }

    public AgentPath(ItemPath itemPath, String agentName) {
        super(itemPath.getUUID());
        mAgentName = agentName;
    }

    public AgentPath(UUID uuid, String agentName) {
        super(uuid);
        mAgentName = agentName;
    }

    public void setAgentName(String agentID) {
        mAgentName = agentID;
    }

    public String getAgentName() {
        if (mAgentName == null) {
            try {
                mAgentName = Gateway.getLookup().getAgentName(this);
            }
            catch (ObjectNotFoundException e) {
                return null;
            }
        }
        return mAgentName;
    }

    public RolePath[] getRoles() {
        return Gateway.getLookup().getRoles(this);
    }

    public RolePath getFirstMatchingRole(List<RolePath> roles) {
        for (RolePath role : roles) {
            if (Gateway.getLookup().hasRole(this, role)) return role;
        }
        return null;
    }

    public boolean hasRole(RolePath role) {
        return Gateway.getLookup().hasRole(this, role);
    }

    public boolean hasRole(String role) {
        try {
            return hasRole(Gateway.getLookup().getRolePath(role));
        }
        catch (ObjectNotFoundException ex) {
            return false;
        }
    }

    @Override
    public String getClusterPath() {
        return ClusterType.PATH + "/Agent";
    }

    @Override
    public String dump() {
        return super.dump() + "\n        agentID=" + mAgentName;
    }

    public boolean isPasswordTemporary() {
        return mPasswordTemporary;
    }
}
