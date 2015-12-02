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
package org.cristalise.kernel.entity.proxy;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.kernel.utils.server.SimpleTCPIPServer;


public class ProxyServer implements Runnable {

    // server objects
    ArrayList<ProxyClientConnection> proxyClients;
    SimpleTCPIPServer proxyListener = null;
    String serverName = null;
    boolean keepRunning = true;
    LinkedBlockingQueue<ProxyMessage> messageQueue;
    
	public ProxyServer(String serverName) {
        Logger.msg(5, "ProxyManager::initServer - Starting.....");
        int port = Gateway.getProperties().getInt("ItemServer.Proxy.port", 0);
        this.serverName = serverName;
        this.proxyClients = new ArrayList<ProxyClientConnection>();
        this.messageQueue = new LinkedBlockingQueue<ProxyMessage>();
        
        if (port == 0) {
            Logger.error("ItemServer.Proxy.port not defined in connect file. Remote proxies will not be informed of changes.");
            return;
        }

        // set up the proxy server
        try {
            Logger.msg(5, "ProxyManager::initServer - Initialising proxy informer on port "+port);
            proxyListener = new SimpleTCPIPServer(port, ProxyClientConnection.class, 200);
            proxyListener.startListening();
        } catch (Exception ex) {
            Logger.error("Error setting up Proxy Server. Remote proxies will not be informed of changes.");
            Logger.error(ex);
        }
        // start the message queue delivery thread
        new Thread(this).start();
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName("Proxy Server");
		while(keepRunning) {
			ProxyMessage message = messageQueue.poll();
			if (message != null) {
	            synchronized(proxyClients) {
	                for (ProxyClientConnection client : proxyClients) {
	                    client.sendMessage(message);
	                }
	            }
			} else
				try {
					synchronized(this) {
						if (messageQueue.isEmpty()) wait(); 
					}
				} catch (InterruptedException e) { }
		}

	}

	public String getServerName() {
		return serverName;
	}

    public void sendProxyEvent(ProxyMessage message) {
		try {
			synchronized(this) { 
				messageQueue.put(message);
				notify(); 
			}
		} catch (InterruptedException e) { }
    }

    public void reportConnections(int logLevel) {
        synchronized(proxyClients) {
            Logger.msg(logLevel, "Currently connected proxy clients:");
            for (ProxyClientConnection client : proxyClients) {
                Logger.msg(logLevel, "   "+client);
            }
        }
    }

    public void shutdownServer() {
        Logger.msg(1, "ProxyManager: Closing Server.");
        proxyListener.stopListening();
        synchronized(this) { 
        	keepRunning = false; 
        	notify();
        }
    }

    public void registerProxyClient(ProxyClientConnection client) {
        synchronized(proxyClients) {
            proxyClients.add(client);
        }
    }

    public void unRegisterProxyClient(ProxyClientConnection client) {
        synchronized(proxyClients) {
            proxyClients.remove(client);
        }
    }

}
