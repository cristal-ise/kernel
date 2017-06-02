package org.cristalise.kernel.process.security;

import java.security.Principal;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SaltedAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.cristalise.kernel.process.Gateway;

import src.main.java.com.github.panchitoboy.shiro.jwt.realm.AuthorizationInfo;
import src.main.java.com.github.panchitoboy.shiro.jwt.realm.Override;
import src.main.java.com.github.panchitoboy.shiro.jwt.realm.SimpleAccount;
import src.main.java.com.github.panchitoboy.shiro.jwt.realm.SimpleAuthorizationInfo;
import src.main.java.com.github.panchitoboy.shiro.jwt.realm.UserDefault;

public class UsernamePasswordRealm extends AuthorizingRealm
{
	private static final String REALM_NAME = "CristaliseJwtRealm";
	
	@Override
	public String getName() {
		return REALM_NAME;
	}

	@Override
	public boolean supports(AuthenticationToken token) {
		return token != null && token instanceof UsernamePasswordToken;
	}

	@Override
	public AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (token instanceof UsernamePasswordToken) {
			return new AuthenticationInfo() {
				
				@Override
				public PrincipalCollection getPrincipals() {
					UsernamePasswordToken userpwd = (UsernamePasswordToken) token;
					if (Gateway.getAuthenticator().authenticate(userpwd.getUsername(), new String(userpwd.getPassword()), null) ) {
						PrincipalCollection principals = new SimplePrincipalCollection(new Principal() {
							
							@Override
							public String getName() {
								return userpwd.getUsername();
							}
						}, REALM_NAME);
					}
					return null;
				}
				
				@Override
				public Object getCredentials() {
					return null;
				}
				
				@Override
				public ByteSource getCredentialsSalt() {
					// TODO Auto-generated method stub
					return null;
				}
			};
		}
		if (token instanceof JwtAuthenticationToken) {
			return true;
		}
	}

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
        UsernamePasswordToken userpass = (UsernamePasswordToken) token;
        if (Gateway.getAuthenticator().authenticate(userpwd.getUsername(), new String(userpwd.getPassword()), null) )) {
        	
        }
        
        UserDefault user = userRepository.findByUserId(userpass.getUsername());
        if (user != null) {
            SimpleAccount account = new SimpleAccount(user, user.getCredentials(), getName());
            account.addRole(user.getRoles());
            return account;
        }

        return null;
    }
    
        @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return new SimpleAuthorizationInfo(((UserDefault) principals.getPrimaryPrincipal()).getRoles());
    }
}
