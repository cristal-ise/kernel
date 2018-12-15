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
package org.cristalise.kernel.security;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

public class SecurityManager {

    public Subject getSubject() {
        return getSubject("system");
    }

    public Subject getSubject(AgentPath agent) {;
        return getSubject(agent.getAgentName());
    }

    public Subject getSubject(String principal) {
        PrincipalCollection principals = new SimplePrincipalCollection(principal, principal);
        return new Subject.Builder().principals(principals).buildSubject();
    }

    public void setupShiro() {
        //TODO: replace this with shiro Environment initialization
        Factory<org.apache.shiro.mgt.SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.ini");
        
        org.apache.shiro.mgt.SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);

        Logger.msg(2, "SecurityManager.setupShiro() - Done");
    }

    public void shiroAuthenticate(String agentName, String agentPassword) throws InvalidDataException {
        Subject agentSubject = getSubject(agentName);

        if ( !agentSubject.isAuthenticated() ) {
            UsernamePasswordToken token = new UsernamePasswordToken(agentName, agentPassword);

            token.setRememberMe(true);

            try {
                agentSubject.login(token);
            }
            catch (Exception ex) {
                //TODO for security reasons remove this log after development is done
                Logger.error(ex);
                throw new InvalidDataException("Authorisation was failed");
            }
        }
    }
    
    public boolean checkPermissions(AgentPath agent, String stepPath, ItemPath itemPath) 
            throws AccessRightsException, ObjectNotFoundException
    {
        String name    = Gateway.getProxyManager().getProxy(itemPath).getName();
        String type    = Gateway.getProxyManager().getProxy(itemPath).getType(); //FIXME Type can be null
        String actName = StringUtils.substringAfterLast(stepPath, "/");

        String permission = type+":"+actName+":"+name;

        Logger.msg(5, "SecurityManager.checkPermissions() - agent:'%s' permission:'%s'", agent.getAgentName(), permission);

        return getSubject(agent).isPermitted(permission);
    }
}
