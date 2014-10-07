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
package org.cristalise.kernel.utils.server;

import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.cristalise.kernel.utils.Logger;



public class SimpleTCPIPServer implements Runnable
{
    int                 port            = 0;
    int                 maxConn         = 10;
    Thread              listener        = null;
    Class<?>               handlerClass    = null;
    ServerSocket        serverSocket    = null;
    boolean            keepListening   = true;
    ArrayList<SocketHandler>           currentHandlers = new ArrayList<SocketHandler>();
    static short       noServers = 0;

    public SimpleTCPIPServer(int port, Class<?> handlerClass, int maxConnections)
    {
        this.port         = port;
        this.handlerClass = handlerClass;
        this.maxConn      = maxConnections;
        noServers++;
    }

    public void startListening()
    {
        if(listener != null) return;
        keepListening = true;

        listener = new Thread(this);
        listener.start();
    }

    public void stopListening()
    {
        Logger.msg("SimpleTCPIPServer: Closing server for " + handlerClass.getName() +" on port "+ port);
        keepListening = false;
        for (SocketHandler thisHandler : currentHandlers) {
            thisHandler.shutdown();
        }
    }

    @Override
	public void run()
    {
        Thread.currentThread().setName("TCP/IP Server for "+handlerClass.getName());
        Socket       connectionSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            if (port == 0)
                port = serverSocket.getLocalPort();
            Logger.msg("SimpleTCPIPServer: Created server for " + handlerClass.getName()+" on port "+port);
            serverSocket.setSoTimeout(500);
            SocketHandler freeHandler = null;
            while(keepListening) {
                if (freeHandler == null || freeHandler.isBusy()) {
                    ListIterator<SocketHandler> i = currentHandlers.listIterator();
                    try {
                        do {
                            freeHandler = i.next();
                        } while (freeHandler.isBusy());
                    } catch (NoSuchElementException e) {
                        // create new one
                        if (currentHandlers.size() < maxConn) {
                            freeHandler = (SocketHandler)handlerClass.newInstance();
                            currentHandlers.add(freeHandler);
                        }
                        else { // max handlers are created. wait for a while, then look again
                            Logger.warning("No free handlers left for "+handlerClass.getName()+" on port "+ port + "!");
                            Thread.sleep(2000);
                            continue;
                        }
                    }
                }
                try {
                    connectionSocket = serverSocket.accept();
                    if (keepListening) {
                        Logger.msg("SimpleTCPIPServer: Connection to "+freeHandler.getName()+" from "+
                            connectionSocket.getInetAddress());
                        freeHandler.setSocket(connectionSocket);
                        new Thread(freeHandler).start();
                    }
                } catch (InterruptedIOException ex1) { }// timeout just to check if we've been told to die

            }
            serverSocket.close();
            Logger.msg("SimpleTCPIPServer: Server closed for " + handlerClass.getName() +" on port "+ port);
        } catch(Exception ex) {
            Logger.error(ex);
            Logger.error("SimpleTCPIPServer.run(): Fatal Error. Listener for '"+handlerClass.getName()+"' will stop.");
            listener = null; --noServers;
            return;
        }
        listener = null;
        Logger.msg("SimpleTCPIPServer - Servers still running: "+--noServers);
    }

    public int getPort() {
        return port;
    }
}
