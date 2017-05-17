/**
 * This file is part of the CRISTAL-iSE REST API.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.bind.DatatypeConverter;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

public class TokenChipher {

    private Cipher encryptCipher;
    private Cipher decryptCipher;

    public TokenChipher()  throws InvalidDataException {
        try {
            try {
                initChiphers(256);
            }
            catch (InvalidKeyException ex) {
                if (Gateway.getProperties().getBoolean("SECURITY.allowWeakKey", false) == false) {
                    Logger.error(ex);
                    Logger.die("TokenChipher() - Weak crypto not allowed, and unlimited strength crypto not installed.");
                }

                Logger.msg("TokenChipher() - Unlimited crypto not installed. Trying 128-bit key.");
                initChiphers(128);
            }
        }
        catch (Exception e) {
            Logger.error(e);
            throw new InvalidDataException("Error initializing token encryption");
        }
    }

    /**
     * 
     * @param keySize
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     */
    private void initChiphers(int keySize)
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException
    {
        KeyGenerator kgen = KeyGenerator.getInstance(Gateway.getProperties().getString("SECURITY.keyGeneratorAlgorithm","AES"));
        kgen.init(keySize);

        SecretKey secretKey = kgen.generateKey();

        //System.out.println("secretKey: "+DatatypeConverter.printBase64Binary(secretKey.getEncoded()));

        encryptCipher = Cipher.getInstance(Gateway.getProperties().getString("SECURITY.chipherAlgorithm", "AES/CBC/PKCS5Padding"));
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);

        decryptCipher = Cipher.getInstance(Gateway.getProperties().getString("SECURITY.chipherAlgorithm", "AES/CBC/PKCS5Padding"));
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(encryptCipher.getIV()));
    }

    /**
     * 
     * @param authData
     * @return
     * @throws InvalidAgentPathException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidDataException
     */
    protected AuthData decryptAuthData(String authData)
            throws InvalidAgentPathException, IllegalBlockSizeException, BadPaddingException, InvalidDataException
    {
        byte[] bytes = DatatypeConverter.parseBase64Binary(authData);
        return new AuthData(decryptCipher.doFinal(bytes));
    }

    /**
     * 
     * @param auth
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    protected String encryptAuthData(AuthData auth) throws IllegalBlockSizeException, BadPaddingException {
        byte[] bytes = encryptCipher.doFinal(auth.getBytes());
        return DatatypeConverter.printBase64Binary(bytes);
    }

    /**
     * Generates the security token from the AgentPath
     * 
     * @param agentPath
     * @return
     * @throws InvalidDataException
     */
    public String generateToken(AgentPath agentPath) throws InvalidDataException {
        try {
            return encryptAuthData(new AuthData(agentPath));
        }
        catch (IllegalBlockSizeException | BadPaddingException e) {
            Logger.error(e);
            throw new InvalidDataException(e.getMessage());
        }
        
    }

    /**
     * Authorization data is decrypted from the input string and the corresponding AgentPath is returned
     * 
     * @param token authorisation data normally taken from token
     * @return AgentPath created from the decrypted autData
     */
    public AgentPath checkToken(String token) throws AccessRightsException {
        if (token == null) {
            throw new AccessRightsException("Missing token");
        }

        try {
            return decryptAuthData(token).agent;
        }
        catch (InvalidAgentPathException | InvalidDataException e) {
            throw new AccessRightsException("Invalid token");
        }
        catch (Exception e) {
            Logger.error(e);
            throw new AccessRightsException("Error reading token");
        }
    }

    /**
     * 
     */
    class AuthData {
        AgentPath agent;
        Date timestamp;

        AuthData(AgentPath agent) {
            this.agent = agent;
            timestamp = new Date();
        }

        AuthData(byte[] bytes) throws InvalidAgentPathException, InvalidDataException {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            SystemKey sysKey = new SystemKey(buf.getLong(), buf.getLong());
            agent = new AgentPath(new ItemPath(sysKey));
            timestamp = new Date(buf.getLong());

            int tokenLife = Gateway.getProperties().getInt("SECURITY.tokenValidityPeriod", 0);

            if (tokenLife > 0 && (new Date().getTime() - timestamp.getTime()) / 1000 > tokenLife) {
                throw new InvalidDataException("Token too old");
            }
        }

        byte[] getBytes() {
            byte[] bytes = new byte[Long.SIZE * 3];
            SystemKey sysKey = agent.getSystemKey();
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            buf.putLong(sysKey.msb);
            buf.putLong(sysKey.lsb);
            buf.putLong(timestamp.getTime());
            return bytes;
        }
    }
}
