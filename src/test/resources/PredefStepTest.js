/*
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
println("Initialising predefined step test"); 
var agent = org.cristalise.gui.MainFrame.userAgent;
var serverPath=new org.cristalise.kernel.lookup.DomainPath("/servers/pcuwe04.cern.ch");
var serverItem=proxy.getProxy(serverPath);

println("Testing predef step: AddDomainContext");
var predef = "AddDomainContext"; var params = new Array(1); params[0] = "/test/context/"; agent.execute(serverItem, predef, params);
println("Testing predef step: RemoveDomainContext");
var predef = "RemoveDomainContext"; agent.execute(serverItem, predef, params);
println("Testing predef step: SetAgentPassword");
params[0] = "/test"; agent.execute(serverItem, predef, params);
var predef = "SetAgentPassword"; params = Array(1); params[0] = "hunter2"; agent.execute(agent, predef, params); 
org.cristalise.kernel.process.Gateway.getAuthenticator().authenticate("dev", "hunter2", "");
params[0] = "test"; agent.execute(agent, predef, params);
println("Testing predef step: SetAgentRoles");
try { var predef = "SetAgentRoles"; agent.execute(serverItem, predef, params); throw "Test Role shouldn't exist"; } catch (e) { }
//Role shouldn't exist
params = Array(2); params[0] = "Admin"; params[1] = "User"; agent.execute(agent, predef, params);
params = Array(1); params[0] = "Admin"; agent.execute(agent, predef, params);

var predef = "AddNewCollectionDescription"; var params = new Array(2);
println("Testing predef step: AddNewCollectionDescription - Aggregation");
params[0] = "TestAgg"; params[1] = "Aggregation"; agent.execute(serverItem, predef, params);
println("Testing predef step: AddNewCollectionDescription - Dependency");
params[0] = "TestDep"; params[1] = "Dependency"; agent.execute(serverItem, predef, params);

println("Testing predef step: CreateNewCollectionVersion");
var predef = "CreateNewCollectionVersion"; var params = new Array(1);
params[0] = "TestAgg"; agent.execute(serverItem, predef, params);

println("Testing predef step: AddNewSlot");
var predef = "AddNewSlot"; var params = new Array(2);
params[0] = "TestAgg"; params[1] = "/desc/dev/ScriptFactory"; agent.execute(serverItem, predef, params);

println("Testing predef step: AddMemberToCollection - without slot properties");
var predef = "AddMemberToCollection"; var params = new Array(2);
params[0] = "TestDep"; params[1] = "/desc/dev/ScriptFactory"; agent.execute(serverItem, predef, params);

println("Testing predef step: AddMemberToCollection - with slot properties");
var predef = "AddMemberToCollection"; var params = new Array(3);
params[0] = "TestDep"; params[1] = "/desc/dev/SchemaFactory";
params[2] = new org.cristalise.kernel.utils.CastorHashMap();
params[2].put("TestProperty", true);
agent.execute(serverItem, predef, params);

println("Testing predef step: AssignItemToSlot");
var predef = "AssignItemToSlot"; var params = new Array(3);
params[0] = "TestAgg"; params[1] = "0"; params[2] = "/desc/Script/dev/CreateNewNumberedVersionFromLast"; agent.execute(serverItem, predef, params);

println("Testing predef step: ClearSlot");
var predef = "ClearSlot"; var params = new Array(2);
params[0] = "TestAgg"; params[1] = "0"; agent.execute(serverItem, predef, params);

println("Testing predef step: RemoveSlotFromCollection");
var predef="RemoveSlotFromCollection"; var params = new Array(2);
params[0] = "TestAgg"; params[1] = "0"; agent.execute(serverItem, predef, params);

var params = new Array(2); params[0] = "TestDep"; params[1] = "-1"; params[2] = "/desc/dev/ScriptFactory";
agent.execute(serverItem, predef, params);

println("Testing predef step: WriteProperty");
var predef = "WriteProperty"; var params = new Array(2);
try { params[0] = "WrongProperty"; params[1] = "0"; agent.execute(serverItem, predef, params); throw "Managed to write non-existent property"; } catch (e) { }
try { params[0] = "ProxyPort";  agent.execute(serverItem, predef, params); throw "Managed to write non-mutable property"; } catch (e) { }
params[0] = "ConsolePort"; params[1] = "12345"; agent.execute(serverItem, predef, params);

println("Testing predef step: RemoveAgent");
var predef = "RemoveAgent"; var params = new Array(0); agent.execute(agent, predef, params);

println("Testing predef step: AddC2KObject - NOT IMPLEMENTED YET");
println("Testing predef step: RemoveC2KObject - NOT IMPLEMENTED YET");
println("Testing predef step: WriteViewpoint - NOT IMPLEMENTED YET");
"Predef Step Test successful";