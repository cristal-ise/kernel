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
package org.cristalise.kernel.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Properties;

import org.cristalise.kernel.process.resource.BadArgumentsException;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2004/10/25 15:27:35 $
 * @version $Revision: 1.67 $
 **************************************************************************/
abstract public class AbstractMain
{
    public static boolean isServer = false;
    private static ShutdownHandler shutdownHandler;

    public static String MAIN_ARG_NONEWLOGSTREAM = "noNewLogStream";
    public static String MAIN_ARG_CONFIG = "config";
    public static String MAIN_ARG_LOGLEVEL = "logLevel";
    public static String MAIN_ARG_LOGFILE = "logFile";
    public static String MAIN_ARG_CONNECT = "connect";



    /**************************************************************************
     * reading and setting input paramaters
     ************************************************************************** 
     * 
     * Known arguments :
     * <ul>
     * <li>logLevel: the log level 0-9 (+10 to have time, +20 to have only one level)</li>
     * <li>logFile: the full path of the target log file. if none, the Logstream is the stdOut</li>
     * <li>noNewLogStream: if present no new Logstream is added to the logger (
     * considers that the Logger is already configured)</li>
     * 
     * <li>config</li>
     * <li>connect</li>
     * <li>LocalCentre</li>
     * </ul>
     *  
     * 
     * @param args
     * @returnn Properties
     * @throws BadArgumentsException
     */
    public static Properties readC2KArgs( String[] args ) throws BadArgumentsException {
        Properties c2kProps;
        Properties argProps = new Properties();
        int logLevel = 0;
        PrintStream logStream = System.out;

        int i = 0;
        while( i < args.length ) {
            if (args[i].startsWith("-") && args[i].length()>1) {
                String key = args[i].substring(1);
                if (argProps.containsKey(key))
                    throw new BadArgumentsException("Argument "+args[i]+" given twice");
                String value = "";
                if (!args[i+1].startsWith("-"))
                    value = args[++i];
                argProps.put(key, value);
                i++;
            }
            else
                throw new BadArgumentsException("Bad argument: "+args[i]);

        }

        if (argProps.containsKey("logFile"))
            try {
                logStream = new PrintStream(new FileOutputStream(argProps.getProperty("logFile"), true));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new BadArgumentsException("Logfile "+argProps.getProperty("logFile")+" cannot be created");
            }


        // if the optional arg "noNewLogStream" isn't present => add a
        // new LogStream
        boolean wMustAddNewLogStream = !argProps.contains(MAIN_ARG_NONEWLOGSTREAM);
        if (wMustAddNewLogStream) {

            // Set up log stream
            if (argProps.containsKey("logLevel"))
                logLevel = Integer.parseInt(argProps.getProperty("logLevel"));

            Logger.addLogStream(logStream, logLevel);
        }
        if (wMustAddNewLogStream) Logger.msg(
                String.format("New logStream added at logLevel %d: %s", logLevel, logStream.getClass().getName()));


        // Dump params if log high enough

        if (Logger.doLog(3)) for (Enumeration<?> e = argProps.propertyNames(); e.hasMoreElements();) {
            String next = (String)e.nextElement();
            System.out.println("AbstractMain: Param "+next+": "+argProps.getProperty(next));
        }

        String configPath = argProps.getProperty("config");
        if (configPath == null)
            throw new BadArgumentsException("Config file not specified");


        // Load config & connect files into c2kprops
        try {
            c2kProps = FileStringUtility.loadConfigFile(argProps.getProperty("config") );
            c2kProps.putAll(argProps); // args overlap config

            String connectFile = c2kProps.getProperty("connect");
            if (connectFile == null)
                throw new BadArgumentsException("Connect file not specified");

            FileStringUtility.appendConfigFile( c2kProps, connectFile);

            if (!c2kProps.containsKey("LocalCentre")) {
                String connectFileName = new File(connectFile).getName();
                String centreId = connectFileName.substring(0, connectFileName.lastIndexOf(".clc"));
                c2kProps.setProperty("LocalCentre", centreId);
            }

            c2kProps.putAll(argProps); // args override connect file too
        }
        catch (IOException e) {
            Logger.error(e);
            throw new BadArgumentsException(e.getMessage());
        }

        Logger.msg(7, "AbstractMain::standardSetUp() - readC2KArgs() DONE.");

        return c2kProps;
    }

    public static void setShutdownHandler(ShutdownHandler handler) {
        shutdownHandler = handler;
    }

    public static void shutdown(int errCode) {
        Bootstrap.abort();
        if (shutdownHandler!= null)
            shutdownHandler.shutdown(errCode, isServer);
        try {
            Gateway.close();
        } catch (Exception ex) {
            Logger.error(ex);
        }
        throw new ThreadDeath(); // if we get here, we get out

    }
}
