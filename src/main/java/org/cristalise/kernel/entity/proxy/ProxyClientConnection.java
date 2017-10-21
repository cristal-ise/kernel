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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.kernel.utils.server.SocketHandler;

/**
 * 
 *
 */
public class ProxyClientConnection implements SocketHandler {
    static int          clientId = -1;
    Socket              clientSocket = null;
    int                 thisClientId;
    ArrayList<ItemPath> subscribedItems;
    PrintWriter         response;
    BufferedReader      request;
    boolean             closing = false;

    public ProxyClientConnection() {
        super();
        thisClientId = ++clientId;
        Gateway.getProxyServer().registerProxyClient(this);
        Logger.msg(1, "ProxyClientConnection() - clientID:"+thisClientId+" READY.");
    }

    @Override
    public String getName() {
        return "Proxy Client Connection";
    }

    @Override
    public boolean isBusy() {
        return clientSocket != null;
    }

    @Override
    public synchronized void setSocket(Socket newSocket) {
        try {
            Logger.msg(1, "ProxyClientConnection.setSocket() - clientID "+thisClientId+" connect from "+newSocket.getInetAddress()+":"+newSocket.getPort());
            newSocket.setSoTimeout(500);
            clientSocket = newSocket;
            response = new PrintWriter(clientSocket.getOutputStream(), true);
            subscribedItems = new ArrayList<ItemPath>();
        } 
        catch (SocketException ex) {
            Logger.error("ProxyClientConnection.setSocket() - Could not set socket timeout:");
            Logger.error(ex);
            closeSocket();
        }
        catch (IOException ex) {
            Logger.error("ProxyClientConnection.setSocket() - Could not setup output stream:");
            Logger.error(ex);
            closeSocket();
        }
    }

    /**
     * Main loop. Reads proxy commands from the client and acts on them.
     */
    @Override
    public void run() {
        Thread.currentThread().setName(getName()+": "+clientSocket.getInetAddress());

        Logger.msg(7, "ProxyClientConnection.run() - clientID:"+thisClientId+" - Setting up proxy client connection with "+clientSocket.getInetAddress());

        try {
            request = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String input = null;
            ProxyMessage thisMessage;
            while (clientSocket != null) {
                try {
                    input = request.readLine();
                    Logger.msg(9, "ProxyClientConnection.run() - "+thisClientId+" - received "+input);
                    thisMessage = new ProxyMessage(input);
                    processMessage(thisMessage);
                } 
                catch (InterruptedIOException ex) { //timeout
                }
                catch (InvalidDataException ex) { // invalid proxy message
                    Logger.error("ProxyClientConnection.run() - clientID:"+thisClientId+" - Invalid proxy message: "+input);
                }
            }
        }
        catch (IOException ex) {
            if (!closing) {
                if (Logger.doLog(8)) Logger.error(ex);
                Logger.error("ProxyClientConnection.run() - clientID:"+thisClientId+" - Error reading from socket.");
            }
        }
        closeSocket();
        Logger.msg(1, "ProxyClientConnection.run() - clientID:"+thisClientId+" closed.");
    }

    /**
     * Processing a single message
     * 
     * @param message the message to be processed
     * @throws InvalidDataException
     */
    private void processMessage(ProxyMessage message) throws InvalidDataException {
        // proxy disconnection
        if (message.getPath().equals(ProxyMessage.BYEPATH)) {
            Logger.msg(7, "ProxyClientConnection.processMessage() - clientID:"+thisClientId+" disconnecting");
            closeSocket();
        }
        else if (message.getPath().equals(ProxyMessage.PINGPATH)) {
            // proxy checking connection
            response.println(ProxyMessage.pingMessage);
        }
        else if (message.getPath().equals(ProxyMessage.ADDPATH)) {
            // new subscription to entity changes
            Logger.msg(7, "ProxyClientConnection.processMessage() - clientID:"+thisClientId+" subscribed to "+message.getItemPath());

            synchronized (subscribedItems) {
                subscribedItems.add(message.getItemPath());
            }
        }
        else if (message.getPath().equals(ProxyMessage.DELPATH)) {
            // remove of subscription to entity changes
            synchronized (subscribedItems) {
                subscribedItems.remove(message.getItemPath());
            }
            Logger.msg(7, "ProxyClientConnection.processMessage() - clientID:"+thisClientId+" unsubscribed from "+message.getItemPath());
        }
        else {
            // unknown message
            Logger.error("ProxyClientConnection.processMessage() - clientID:"+thisClientId+" - Unknown message type: "+message);
        }
    }

    public synchronized void sendMessage(ProxyMessage message) {
        if (clientSocket==null) return; // idle
        boolean relevant = message.getItemPath() == null;

        synchronized (subscribedItems) {
            for (Iterator<ItemPath> iter = subscribedItems.iterator(); iter.hasNext() && !relevant;) {
                ItemPath thisKey = iter.next();

                if (thisKey.equals(message.getItemPath())) relevant = true;
            }
        }

        if (!relevant) return; // not for our client

        response.println(message);
    }

    @Override
    public void shutdown() {
        if (isBusy()) {
            closing = true;
            Logger.msg("ProxyClientConnection.shutdown() - clientID: "+thisClientId+" closing.");
            closeSocket();
        }
    }

    @Override
    public String toString() {
        if (clientSocket == null) return thisClientId+": idle";
        else return thisClientId+": "+clientSocket.getInetAddress();
    }

    private synchronized void closeSocket() {
        if (clientSocket==null) return;

        try {
            request.close();
            response.close();
            clientSocket.close();
        }
        catch (IOException e) {
            Logger.error("ProxyClientConnection.closeSocket() - clientID: "+thisClientId+" - Could not close socket.");
            Logger.error(e);
        }

        synchronized (subscribedItems) {
            subscribedItems = null;
        }

        clientSocket = null;
    }
}
