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
package org.cristalise.kernel.process.auth;

import java.util.Properties;
import java.util.Scanner;

import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.process.Gateway;

public class ConsoleAuth implements ProxyLogin {

    @Override
    public void initialize(Properties props) throws Exception {
    }

    @Override
    public AgentProxy authenticate(String resource) throws Exception {
        AgentProxy user = null;

        if (resource != null) System.out.println("Please log in" + (resource.length() > 0 ? "to " + resource : ""));

        Scanner scan = new Scanner(System.in);

        int loginAttempts = 0;

        while (user == null && loginAttempts++ < 3) {
            System.out.print("User:");
            String username = scan.nextLine();

            System.out.print("Password:");
            String pass = scan.nextLine();

            try {
                user = Gateway.connect(username, pass, resource);
            }
            catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
        scan.close();

        if (user == null) {
            System.err.println("Bye");
            System.exit(0);
        }
        return user;
    }
}
