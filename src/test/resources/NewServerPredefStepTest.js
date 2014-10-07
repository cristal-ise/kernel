/*
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
var serverPath=new org.cristalise.kernel.lookup.DomainPath("/servers/pcuwe04.cern.ch");
var serverItem=proxy.getProxy(serverPath);

var predef = "AddDomainContext"; var params = new Array(1); params[0] = "/test/context"; agent.execute(serverItem, predef, params);
var predef = "RemoveDomainContext"; agent.execute(serverItem, predef, params);
params[0] = "/test"; agent.execute(serverItem, predef, params);
var predef = "SetAgentPassword"; params = Array(2); params[0] = "dev"; params[1] = "hunter2"; agent.execute(serverItem, predef, params); 
org.cristalise.kernel.process.Gateway.login("dev", "hunter2");
var predef = "SetAgentRoles"; agent.execute(serverItem, predef, params); //Role shouldn't exist
params = Array(3); params[0] = "dev"; params[1] = "Admin"; params[2] = "UserCode"; agent.execute(serverItem, predef, params);
params = Array(2); params[0] = "dev"; params[1] = "Admin"; agent.execute(serverItem, predef, params);
var predef = "RemoveAgent"; var params = new Array(1); params[0] = "dev"; agent.execute(serverItem, predef, params);
