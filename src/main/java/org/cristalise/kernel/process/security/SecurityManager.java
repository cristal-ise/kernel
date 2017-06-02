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
package org.cristalise.kernel.process.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;

import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class SecurityManager 
{
	private static final String ISSUER = "cristal-ise";
	private static final String UUID_CLAIM = "uuid";

//	PRIVATE kEY KEY;
	private Algorithm algorithm;

    public SecurityManager() 
    {
    	try {
    	    final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    	    generator.initialize(1024);
    	    final KeyPair keyPair = generator.generateKeyPair();
    		
    	    algorithm = Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
		} 
    	catch (IllegalArgumentException | NoSuchAlgorithmException e) {
    		// TODO
			System.out.println(SecurityManager.class.getSimpleName() + " could not initialize the Algorithm: " + e);
		}
    }
    
    public String generateToken(AgentPath agent)
    {
    	String jwt = null;
    	try {
    		jwt = JWT.create()
    				.withIssuer(ISSUER)                                              // who creates the token and signs it (could be a stand-alone authentication server)
//    				.withAudience(audience)                                          // to whom the token is intended to be sent (cristal server hostname)
    				.withJWTId(UUID.randomUUID().toString())                         // a unique identifier for the token
    				.withIssuedAt(new Date())                                        // when the token was issued/created (now)
//    				.withExpiresAt(new Date())                                       // time when the token will expire (1 hour from now?)
    				.withNotBefore(new Date(new  Date().getTime() - 2 * 60 * 1000))  // time before which the token is not yet valid (2 minutes ago)
    				.withSubject(agent.getAgentName())                               // the subject/principal is whom the token is about
    				
    				.withClaim(UUID_CLAIM, agent.getUUID().toString())
    				
    				.sign(algorithm);
    	} 
    	catch (IllegalArgumentException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
		return jwt;
    }


    public void checkReadAccess(String token, String clusterPath)
    {
    		AgentPath agent = decodeAgentPath(token);
			
			// TODO shiro
    }

    public AgentPath decodeAgentPath(String token) 
    {
    	AgentPath agent = null;
    	try {
    		JWTVerifier verifier = JWT.require(algorithm)
    				.withIssuer(ISSUER)                     // expected issuer
//    				.withAudience(audience)                 // expected audience
    				.build();

    		DecodedJWT jwt = verifier.verify(token);
    		
//    		System.out.println("JWT validation succeeded! ");
//    		for (Map.Entry<String, Claim> entry: jwt.getClaims().entrySet()) {
//    			
//    			if (entry.getKey() .equals( "iat" ) ) {
//    				System.out.println("  " + entry.getKey() + ": " + entry.getValue().asDate() );
//    			}
//    			else {
//    				System.out.println("  " + entry.getKey() + ": " + entry.getValue().asString());
//    			}
//			}

    		agent = new AgentPath(new ItemPath(jwt.getClaim(UUID_CLAIM).asString()), jwt.getSubject());
    	} 
    	catch (JWTVerificationException | InvalidItemPathException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    	return agent;
    }
    
}
