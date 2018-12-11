package org.cristalise.kernel.security;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.utils.Logger;

public class SecurityManager {

    public Subject getSubject() {
        return getSubject("System");
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
}
