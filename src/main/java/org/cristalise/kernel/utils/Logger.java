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
package org.cristalise.kernel.utils;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;

import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ScriptConsole;
import org.cristalise.kernel.utils.server.SimpleTCPIPServer;

/**
 * <pre>- message string should always contain the class name and the method name: Logger.msg(1,"ItemFact::createDir() - LifeCycle DB created"); - use meaningfull abbreviation and also use the dash to separate the 'header' from the message! - each method should start with this 'method signature' debug: Logger.msg(1,"ItemFact::createDir() - path:" + path);
*</pre>
 */
public class Logger
{
	/**
	 * logging level 0 (only error & warning) => no logging ; 9 => maximum logging
     * add ten to output time before each message
	 */
	private static int mHighestLogLevel = 0;
	private static long startTime = System.currentTimeMillis();
    private static HashMap<PrintStream, Integer> logStreams = new HashMap<PrintStream, Integer>();
    static protected SimpleTCPIPServer  mConsole       = null;

	static private void printMessage(String message, int msgLogLevel)
	{
        synchronized(logStreams) {
            for (Iterator<PrintStream> iter = logStreams.keySet().iterator(); iter.hasNext();) {
                PrintStream element = iter.next();
                int logLevel = logStreams.get(element);

                if ( (logLevel > 9 && logLevel - 10 < msgLogLevel) || 
                		(msgLogLevel > 9 && logLevel < msgLogLevel - 10) ||
                		(logLevel < 10 && msgLogLevel < 10 && logLevel < msgLogLevel) )
                    continue;

                if (logLevel > 9 || msgLogLevel > 9) {
                	message = reportTime() + " - " + message;
                }
                
                try {
                    element.println(message);
                    element.flush();
                } catch (Exception ex) {
                    iter.remove();
                }
            }
        }
	}
	
	static private String reportTime() {
		long now = System.currentTimeMillis();
		Timestamp ts = new Timestamp(now);
		double since = (now - startTime) / 1000.0;
		return ts.toString() + " ("+since+"s)";
	}

    static private void printMessage(Throwable ex) {
            StringWriter msgString = new StringWriter();
            PrintWriter msg = new PrintWriter(msgString);
            msg.print(ex instanceof Exception ? "EXCEPTION:" : "JVM ERROR:");
            ex.printStackTrace(msg);
            printMessage(msgString.toString(), 0);
    }

	static public boolean doLog(int logLevel)
	{
		if (logLevel > 9) logLevel -= 10;
        return mHighestLogLevel >= logLevel;
	}
	/**
	 * Use this only for temporary messages while developing/debugging. When the code is stable, change calls to debug to
	 * message/warning/error with an appropriate log level. Is is marked deprecated to highlight stray calls. This makes 
	 * it easier to manage debug calls in the source.
	 *
	 * @param msg -
	 *            the string to write to the console, or log file if specified in cmd line
	 * @deprecated
	 */
	@Deprecated
	static public void debug(String msg)
	{
		msg("DEBUG  : " + msg);
	}
	static public void debug(int logLevel, String msg)
	{
		msg(logLevel, "DEBUG  : " + msg);
	}
	/**
	 * Use Logger.message to report information that will be useful for debugging a release
	 *
	 * @param level -
	 *            log level of this message. If the current log level has been on the cmd line to be less that this number, the log message
	 *            will not be displayed
	 * @param msg -
	 *            the string to write to the console, or log file if specified in cmd line
	 */
	static public void msg(int level, String msg)
	{
		printMessage(msg, level);
	}
	static public void msg(String msg)
	{
		printMessage(msg, 0);
	}
	static public void error(String msg)
	{
		printMessage("ERROR  : " + msg, 0);
	}
	static public void error(Throwable ex)
	{
		printMessage(ex);
	}
	static public void warning(String msg)
	{
		printMessage("WARNING: " + msg, 0);
	}
	static public void die(String msg)
	{
		printMessage("FATAL  : " + msg, 0);
		AbstractMain.shutdown(1);
	}
	/**
     * @param console
     */
    public static void addLogStream(PrintStream console, int logLevel) {
        try {
            console.println("***********************************************************");
            console.println("  CRISTAL log started at level "+logLevel+" @"+new Timestamp(System.currentTimeMillis()).toString());
            console.println("***********************************************************");
        } catch (Exception ex) {
            System.out.println("Exception accessing log stream");
            ex.printStackTrace();
        }

        synchronized(logStreams) {
            logStreams.put(console, logLevel);
            if ((logLevel>10?logLevel-10:logLevel) > mHighestLogLevel) mHighestLogLevel = logLevel;
        }

    }
    /**
     * @param console
     */
    public static void removeLogStream(PrintStream console) {
        synchronized(logStreams) {
            Integer logIntObj = logStreams.get(console);
            if (logIntObj == null) return; // not registered
            int logLevel = (logIntObj).intValue();
            logStreams.remove(console);

            // recalculate lowest log level
            if (logLevel == mHighestLogLevel || (logLevel > 9 && logLevel-10 == mHighestLogLevel)) {
                mHighestLogLevel = 0;
                for (Integer element : logStreams.values()) {
                    int thisLogLevel = element>9?element-10:element;
                    if (thisLogLevel > mHighestLogLevel)
                        mHighestLogLevel = thisLogLevel;
                }
            }
        }
    }

    static public void initConsole(String id)
    {
        int port = Gateway.getProperties().getInt(id+".Console.port", 0);
        if (port == 0)
            Logger.msg("No port defined for "+id+" console. Using any port.");

        mConsole = new SimpleTCPIPServer(port, ScriptConsole.class, 5);
        mConsole.startListening();
        Gateway.getProperties().setProperty(id+".Console.port", String.valueOf(mConsole.getPort()));
    }

    static public int getConsolePort() {
        return mConsole.getPort();
    }

    static public void closeConsole()
    {
        if (mConsole != null)
            mConsole.stopListening();
    }
}
