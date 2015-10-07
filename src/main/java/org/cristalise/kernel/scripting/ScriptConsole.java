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
package org.cristalise.kernel.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.kernel.utils.server.SocketHandler;


/**************************************************************************
 *
 * $Revision: 1.16 $
 * $Date: 2005/08/31 07:20:40 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/



public class ScriptConsole implements SocketHandler {
    BufferedReader input;
    PrintStream output;
    Socket socket = null;
    ScriptEngine engine;
    Bindings beans;
    static AgentProxy user;
    static ArrayList<String> securityHosts = new ArrayList<String>();
    public static final short NONE = 0;
    public static final short ALLOW = 1;
    public static final short DENY = 2;
    static short securityMode;

    static {
        securityMode = ALLOW;
        String hosts = Gateway.getProperties().getString("ItemServer.Console.allow");
        if (hosts == null || hosts.equals("")) {
            securityMode = DENY;
            hosts = Gateway.getProperties().getString("ItemServer.Console.deny");
        }
        if (hosts == null || hosts.equals("")) { // by default only allow localhost
            securityMode = ALLOW;
            securityHosts.add("localhost");
            securityHosts.add("127.0.0.1");
            securityHosts.add("0:0:0:0:0:0:0:1");//ipv6
        }
        else {
            StringTokenizer tok = new StringTokenizer(hosts, ",");
            while(tok.hasMoreTokens()) {
            	String wHostName = tok.nextToken();
                try {
                    securityHosts.add(InetAddress.getByName(wHostName).getHostAddress());
                    if ("localhost".equals(wHostName)){
                        securityHosts.add("127.0.0.1");
                        securityHosts.add("0:0:0:0:0:0:0:1");//ipv6
                    }
                } catch (UnknownHostException ex) {
                    Logger.error("Host not found "+ex.getMessage());
                }
            }
        }
    }
    public ScriptConsole() {
    }

    @Override
	public String getName() {
        return "Script Console";
    }

    @Override
	public boolean isBusy() {
        return (socket != null);
    }
    
    public static void setUser(AgentProxy agent) {
    	user = agent;
    }

    @Override
	public void setSocket(Socket newSocket) {
        try {
            input = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
            output = new PrintStream(newSocket.getOutputStream());
            newSocket.setSoTimeout(0);
            socket = newSocket;
        } catch (IOException ex) {
            try {
                newSocket.close();
            } catch (IOException ex2) {
            }
            socket = null;
            return;
        }
    }

    @Override
	public void shutdown() {
        Socket closingSocket = socket;
        socket = null;
        if (closingSocket == null)
            return;
        try {
            Logger.removeLogStream(output);
            closingSocket.shutdownInput();
            closingSocket.shutdownOutput();
            closingSocket.close();
            Logger.msg("Script console to "+closingSocket.getInetAddress()+" closed");
        } catch (IOException e) {
            Logger.error("Script Console to " + closingSocket.getInetAddress() + " - Error closing.");
            Logger.error(e);
        }
    }


    @Override
	public void run() {
        // check permission
        boolean allowed = true;
        if (securityMode!=NONE) {
        	//ogattaz
        	String wHostAddress = socket.getInetAddress().getHostAddress();
            if (securityHosts.contains(wHostAddress)) {
                if (securityMode==DENY)
                    allowed = false;
            }
            else if (securityMode==ALLOW)
                allowed = false;
        }

        if (!allowed) {
            Logger.error("Host "+socket.getInetAddress()+" access denied");
            output.println("Host "+socket.getInetAddress()+" access denied");
            shutdown();
            return;
        }

        // get system objects
        try {
            Logger.addLogStream(output, 0);
            Script context;           
            try {
            	context = new Script("javascript", user, output);
            } catch (Exception ex) {
            	output.println("Error initializing console script context");
            	ex.printStackTrace(output);
            	shutdown();
            	return;
            }
 
            StringBuffer commandBuffer = new StringBuffer();
            while (socket != null) {

                output.println();
                output.print('>');

                String command = null;
                boolean gotCommand = false;
                while (!gotCommand) {
                    try {
                        command = input.readLine();
                        gotCommand = true;
                    } catch (InterruptedIOException ex) {
                    }
                }
                if (command == null) // disconnected
                    shutdown();
                else {
                    if (command.equals("exit")) {
                        shutdown();
                        continue;
                    }
                    try {
                        if (command.endsWith("\\")) {
                            commandBuffer.append(command.substring(0,command.length()-1));
                            continue;
                        }
                        commandBuffer.append(command);
                        command = commandBuffer.toString();
                        commandBuffer = new StringBuffer();
                        Logger.msg("Console command from "+socket.getInetAddress()+": "+command);

                        // process control
                        if (command.equals("shutdown")) {
                        	AbstractMain.shutdown(0);
                        }
                        else {
                        	context.setScript(command);
                        	Object response = context.execute();
                        	if (response == null)
                        		output.println("Ok");
                        	else
                        		output.println(response);
                        }
                    } catch (Throwable ex) {
                       	ex.printStackTrace(output);
                    }
                    output.flush();
                }
            }
        } catch (IOException ex) {
            Logger.error("IO Exception reading from script console socket");
            shutdown();
        }
    }
}
