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
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.property.BuiltInItemProperties;
import org.cristalise.kernel.utils.Logger;
import lombok.Getter;

public class SecurityManager {
    
    private static final String securityMsgBegin = "[errorMessage]";
    private static final String securityMsgEnd   = "[/errorMessage]";

    @Getter
    private Authenticator auth = null;
    @Getter
    private boolean shiroEnabled = false;

    /**
     * 
     * @throws InvalidDataException
     */
    public SecurityManager() throws InvalidDataException {
        if ("Shiro".equals(Gateway.getProperties().getString("Authenticator", ""))) {
            setupShiro();
        }
        else {
            auth = Gateway.getAuthenticator();
        }
    }

    /**
     * 
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    public void authenticate() throws InvalidDataException, ObjectNotFoundException {
        if (!shiroEnabled) {
            if (!auth.authenticate("system")) throw new InvalidDataException("Server authentication failed");
        }
        //NOTE: the else case is not required because shiro cannot authentcate users without a password, and the current
        //setup does not allow us to create the 'system' Agent with password. Also the original auth.authenticate("system") 
        //code simply sets up the connection to the underlying technology (LDAP/AD/JDBC) to 'authenticate' the system user
    }

    /**
     * 
     * @param agentName
     * @param agentPassword
     * @param resource
     * @return
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    public AgentProxy authenticate(String agentName, String agentPassword, String resource)
            throws InvalidDataException, ObjectNotFoundException
    {
        if (shiroEnabled) {
            if (!shiroAuthenticate(agentName, agentPassword)) throw new InvalidDataException("Login failed");
        }
        else {
            if (!auth.authenticate(agentName, agentPassword, resource)) throw new InvalidDataException("Login failed");
        }

        // It can be invoked before ProxyManager and Lookup is initialised
        if (Gateway.getProxyManager() != null) return Gateway.getProxyManager().getAgentProxy(agentName);
        else                                   return null;
    }

    /**
     * 
     * @param agent
     * @return
     */
    public Subject getSubject(AgentPath agent) {;
        return getSubject(agent.getAgentName());
    }

    /**
     * 
     * @param principal
     * @return
     */
    public Subject getSubject(String principal) {
        PrincipalCollection principals = new SimplePrincipalCollection(principal, principal);
        return new Subject.Builder().principals(principals).buildSubject();
    }

    /**
     * 
     */
    public void setupShiro() {
        //TODO: replace this with shiro Environment initialization
        Factory<org.apache.shiro.mgt.SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro.ini");

        org.apache.shiro.mgt.SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);

        Logger.msg(2, "SecurityManager.setupShiro() - Done");

        shiroEnabled = true;
    }

    /**
     * Reads the message from the exception that can be show to the user.
     * 
     * @param ex the exception to be processed
     * @return returns the message or null if nothing was found
     */
    public static String decodePublicSecurityMessage(Exception ex) {
        return StringUtils.substringBetween(ex.getMessage(), securityMsgBegin, securityMsgEnd);
    }

    /**
     * Wraps the massage with specific tokens indicating the the exception has a message to the user.
     * 
     * @param msg the message to be wrapped
     * @return the wrapped message
     */
    public static String encodePublicSecurityMessage(String msg) {
        return securityMsgBegin + msg + securityMsgEnd;
    }

    
    /**
     * 
     * @param agentName
     * @param agentPassword
     * @return
     */
    public boolean shiroAuthenticate(String agentName, String agentPassword) throws InvalidDataException {
        Subject agentSubject = getSubject(agentName);

        if ( !agentSubject.isAuthenticated() ) {
            UsernamePasswordToken token = new UsernamePasswordToken(agentName, agentPassword);

            token.setRememberMe(true);

            try {
                agentSubject.login(token);
                return true;
            }
            catch (Exception ex) {
              //NOTE: Enable this log for testing security problems only, but always remove it when merged
              //Logger.error(ex);

              String publicMsg = decodePublicSecurityMessage(ex);

              if (StringUtils.isNotBlank(publicMsg)) {
                Logger.msg(5, "SecurityManager.shiroAuthenticate() - Failed with public message:%s", publicMsg);
                throw new InvalidDataException(encodePublicSecurityMessage(publicMsg));
              }
            }
        }

        return false;
    }

    /**
     * 
     * @param agent
     * @param stepPath
     * @param itemPath
     * @return
     * @throws AccessRightsException
     * @throws ObjectNotFoundException Item was not found
     */
    public boolean checkPermissions(AgentPath agent, Activity act, ItemPath itemPath) 
            throws AccessRightsException, ObjectNotFoundException
    {
        String domain = getWildcardPermissionDomain(itemPath);
        String action = getWildcardPermissionAction(act);
        String target = Gateway.getProxyManager().getProxy(itemPath).getName();

        //The Shiro's WildcardPermission string 
        String permission = domain+":"+action+":"+target;

        Logger.msg(5, "SecurityManager.checkPermissions() - agent:'%s' permission:'%s'", agent.getAgentName(), permission);

        return getSubject(agent).isPermitted(permission);
    }
    
    /**
     * 
     * @param itemPath
     * @return
     * @throws ObjectNotFoundException Item was not found 
     * @throws AccessRightsException 
     */
    private String getWildcardPermissionDomain(ItemPath itemPath) throws ObjectNotFoundException, AccessRightsException {
        ItemProxy item = Gateway.getProxyManager().getProxy(itemPath);
        String type = item.getType();

        String domain = item.getProperty(BuiltInItemProperties.SECURITY_DOMAIN, type);

        if (StringUtils.isBlank(domain)) throw new AccessRightsException("Domain was blank - Specify 'SecurityDomain' or 'Type' ItemProperties");

        return domain;
    }

    /**
     * 
     * @param act
     * @return
     * @throws AccessRightsException 
     */
    private String getWildcardPermissionAction(Activity act) throws AccessRightsException {
        String action = (String) act.getBuiltInProperty(BuiltInVertexProperties.SECURITY_ACTION);

        if (StringUtils.isBlank(action)) action = act.getName();
        if (StringUtils.isBlank(action)) throw new AccessRightsException("Action was blank - Specify 'SecurityAction' or 'Name' ActivityProperties");

        return action;
    }
}
