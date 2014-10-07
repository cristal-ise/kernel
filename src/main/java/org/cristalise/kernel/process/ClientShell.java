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
package org.cristalise.kernel.process;

import java.util.Scanner;

import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.process.auth.ProxyLogin;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptParsingException;


public class ClientShell extends StandardClient {

	AgentProxy user;
	Script console;
	
	public ClientShell(AgentProxy user) throws Exception {
		this.user = user;
		console = new Script("javascript", user, System.out);
	}
	
	public void run() {
		Scanner scan = new Scanner(System.in);
		System.out.print("> ");
		while (scan.hasNextLine()) {
			String command = scan.nextLine();
			try {
				console.setScript(command);
	        	Object response = console.execute();
	        	if (response == null)
	        		System.out.println("Ok");
	        	else
	        		System.out.println(response);
			} catch (ScriptParsingException e) {
				System.err.println("Syntax error: "+e.getMessage());
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
			System.out.print("> ");
		}
		shutdown(0);
	}

	public static void main(String[] args) throws Exception {
		Gateway.init(readC2KArgs(args));
		ProxyLogin auth = (ProxyLogin)Gateway.getProperties().getInstance("cli.auth");
		AgentProxy user = auth.authenticate(Gateway.getProperties().getProperty("Name"));
		ClientShell shell = new ClientShell(user);
		shell.run();
	}

}
