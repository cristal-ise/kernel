package org.cristalise.kernel.process.security;

import org.apache.shiro.authc.AuthenticationToken;
import org.cristalise.kernel.lookup.AgentPath;

import lombok.Data;

@Data
public class JwtAuthenticationToken implements AuthenticationToken
{
//	private AgentPath agent = null;
//	
//	public AgentPath getAgent() {
//		if (null == agent) {
//			agent = new SecurityManager().decodeAgentPath(token);
//			// TODO handle null?
//		}
//		return agent;
//	}
	
    private Object userId;
    private String token;

    public JwtAuthenticationToken(Object userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return getUserId();
    }

    @Override
    public Object getCredentials() {
        return getToken();
    }

    public Object getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}