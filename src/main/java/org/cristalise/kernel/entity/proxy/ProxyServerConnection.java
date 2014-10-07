/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.entity.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.Socket;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.utils.Logger;



public class ProxyServerConnection extends Thread
{

    public boolean serverIsActive = true;
    // proxy client details
    String serverName;
    int serverPort;
    Socket serverConnection;
    ProxyManager manager;
    // for talking to the proxy server
    PrintWriter serverStream;
    boolean listening = false;
    static boolean isServer = false;

    /**
     * Create an entity proxy manager to listen for proxy events and reap unused proxies
     */
    public ProxyServerConnection(String host, int port, ProxyManager manager)
    {
        Logger.msg(5, "ProxyServerConnection - Initialising connection to "+host+":"+port);
        serverName = host;
        serverPort = port;
        this.manager = manager;
        listening = true;
        start();
    }

    @Override
	public void run() {
        Thread.currentThread().setName("Proxy Client Connection Listener to "+serverName+":"+serverPort);
        while (listening) {
            try {
                if (serverConnection == null) connect();
                if (serverConnection != null) {
                    BufferedReader request = new BufferedReader(new InputStreamReader(serverConnection.getInputStream()));
                    String input = null;
                    ProxyMessage thisMessage;
                    while (listening && serverConnection != null) {
                        try {
                            input = request.readLine();
                            thisMessage = new ProxyMessage(input);
                            thisMessage.setServer(serverName);
                            manager.processMessage(thisMessage);
                        } catch (InterruptedIOException ex) { // timeout - send a ping
                            sendMessage(ProxyMessage.pingMessage);
                        } catch (InvalidDataException ex) { // invalid proxy message
                            if (input != null)
                                Logger.error("EntityProxyManager - Invalid proxy message: "+input);
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.error("ProxyServerConnection - Disconnected from "+serverName+":"+serverPort);
                try {
                    serverStream.close();
                    serverConnection.close();
                } catch (IOException e1) { }


                serverStream = null;
                serverConnection = null;
            }
        }

        if (serverStream != null) {
            try {
                Logger.msg(1, "Disconnecting from proxy server on "+serverName+":"+serverPort);
                serverStream.println(ProxyMessage.byeMessage.toString());
                serverStream.close();
                serverConnection.close();
                serverConnection = null;
            } catch (Exception e) {
                Logger.error("Error disconnecting from proxy server.");
            }
        }
    }

    public void connect() {
        Logger.msg(3, "ProxyServerConnection - connecting to proxy server on "+serverName+":"+serverPort);
        try {
            serverConnection = new Socket(serverName, serverPort);
            serverConnection.setKeepAlive(true);
            serverIsActive = true;
            serverConnection.setSoTimeout(5000);
            serverStream = new PrintWriter(serverConnection.getOutputStream(), true);
            Logger.msg("Connected to proxy server on "+serverName+":"+serverPort);
            manager.resubscribe(this);
        } catch (Exception e) {
            Logger.msg(3, "Could not connect to proxy server. Retrying in 5s");
            try { Thread.sleep(5000); } catch (InterruptedException ex) { }
            serverStream = null;
            serverConnection = null;
            serverIsActive = false;
        }
    }

    public void shutdown() {
        Logger.msg("Proxy Client: flagging shutdown.");
        listening = false;
    }

    /**
     * @param sub
     */
    public void sendMessage(ProxyMessage sub) {
        if (serverStream != null)
            serverStream.println(sub);
    }

}

