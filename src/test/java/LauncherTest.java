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
import java.util.Properties;

import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.resource.BadArgumentsException;
import org.cristalise.kernel.utils.Logger;



public class LauncherTest {

	String[] args;
	Properties props;
	
	public LauncherTest() {
	}
	
	private void standardArgs() {
		args = new String[8];
		args[0] = "-logLevel";
		args[1] = "0";
		args[2] = "-logFile";
		args[3] = "target/testlog.txt";
		args[4] = "-config";
		args[5] = LauncherTest.class.getResource("server.conf").getPath();
		args[6] = "-connect";
		args[7] = LauncherTest.class.getResource("test.clc").getPath();
	}

	public void testValidC2KArgs() throws Exception {

		standardArgs();
		Logger.msg("Testing valid startup args");		
		props = AbstractMain.readC2KArgs(args);
		
		assert "MemoryOnlyClusterStorage".equals(props.get("ClusterStorage")) : "Config file properties not loaded";
		assert "1553".equals(props.get("ItemServer.Proxy.port")) : "Connect file properties not loaded";
	}
	
	public void testWrongConfigFileName() throws Exception {
		standardArgs();
		args[5] = "filenotfound";
		try {
			props = AbstractMain.readC2KArgs(args);
			assert false: "Invalid connect file not detected";
		} catch (BadArgumentsException ex) { }
	}
	
	public void testWrongConnectFileName() throws Exception {
		standardArgs();
		args[7] = "alsonotfound";
		try {
			props = AbstractMain.readC2KArgs(args);
			assert false : "Invalid connect file not detected";
		} catch (BadArgumentsException ex) { }
	}
	
	public void testMissingConnectArg() throws Exception {
		args = new String[2];
		args[0] = "-config";
		args[1] = LauncherTest.class.getResource("server.conf").getPath();
		try {
			props = AbstractMain.readC2KArgs(args);
			assert false: "Missing connect file not detected";
		} catch (BadArgumentsException ex) { }
	}
	
	public void testMissingConfigArg() throws Exception {
		args = new String[2];
		args[0] = "-connect";
		args[1] = LauncherTest.class.getResource("test.clc").getPath();
		try {
			props = AbstractMain.readC2KArgs(args);
			assert false: "Missing config file not detected";
		} catch (BadArgumentsException ex) { }	
	}
}
