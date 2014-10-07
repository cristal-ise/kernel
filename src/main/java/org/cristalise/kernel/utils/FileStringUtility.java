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

//Java
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2004/10/20 14:10:21 $
 * @version $Revision: 1.31 $
 **************************************************************************/
public class FileStringUtility
{
	/**************************************************************************
	 * Reads a file and converts it to String
	 **************************************************************************/
	static public String file2String(File file) throws FileNotFoundException, IOException
	{
		FileInputStream fis = new FileInputStream(file);
		byte[] bArray = (byte[]) Array.newInstance(byte.class, (int) file.length());
		Logger.msg(8, "FileStringUtility.file2String() - Reading file '" + file.getAbsolutePath()+"'");

		fis.read(bArray, 0, (int) file.length());
		fis.close();

		Logger.msg(9, "FileStringUtility.file2String() - file '" + file.getAbsolutePath() + "' read.");

		return new String(bArray);
	}

	/**************************************************************************
	 * Reads a file and converts it to String
	 **************************************************************************/
	static public String file2String(String fileName) throws FileNotFoundException, IOException
	{
		return file2String(new File(fileName));
	}

	/**************************************************************************
	 * Reads a file and converts it to String
	 **************************************************************************/
	static public String url2String(java.net.URL location) throws IOException
	{
        BufferedReader in = new BufferedReader(new InputStreamReader(location.openStream(), "UTF-8"));
        StringBuffer strbuf = new StringBuffer();
        String line = in.readLine();
        while (line != null) {
        	strbuf.append(line).append('\n');
        	line = in.readLine();
        }
		return strbuf.toString();
	}

	/**************************************************************************
	 * Reads a file and converts each line to String[]
	 **************************************************************************/
	static public String[] file2StringArray(File file) throws FileNotFoundException, IOException
	{
		FileReader fr = new FileReader(file);
		BufferedReader buf = new BufferedReader(fr);
		Vector<String> lines = new Vector<String>();
		String thisLine = null;
		while ((thisLine = buf.readLine()) != null)
			lines.addElement(thisLine);
		buf.close();
		String[] lineArray = new String[lines.size()];
		for (int i = 0; i < lines.size(); i++)
			lineArray[i] = lines.get(i);
		return lineArray;
	}

	/**************************************************************************
	 * Reads a file and converts it to String[]
	 **************************************************************************/
	static public String[] file2StringArray(String fileName) throws FileNotFoundException, IOException
	{
		return file2StringArray(new File(fileName));
	}

	/**************************************************************************
	 * Saves a string to a text file
	 **************************************************************************/
	static public void string2File(File file, String data) throws FileNotFoundException, IOException
	{
		FileWriter thisFile = new FileWriter(file);
		BufferedWriter thisFileBuffer = new BufferedWriter(thisFile);

		Logger.msg(9, "FileStringUtility.string2File() - writing file '" + file.getAbsolutePath()+"'");

		thisFileBuffer.write(data);
		thisFileBuffer.close();

		Logger.msg(9, "FileStringUtility.string2File() - file '" + file.getAbsolutePath() + "' complete.");
	}

	/**************************************************************************
	 * Saves a string to a text file
	 **************************************************************************/
	static public void string2File(String fileName, String data) throws FileNotFoundException, IOException
	{
		string2File(new File(fileName), data);
	}

	/**************************************************************************
	 * checks for existing directory
	 **************************************************************************/
	static public boolean checkDir(String dirPath)
	{
		File dir = new File(dirPath);

		if (dir.isFile())
		{
			Logger.error("FileStringUtility.checkDir() - '" + dir.getAbsolutePath() + "' is a file.");
			return false;
		}
		else if (!dir.exists())
		{
			Logger.msg(9, "FileStringUtility.checkDir() - directory '" + dir.getAbsolutePath() + "' does not exist.");
			return false;
		}

		return true;
	}

	/**************************************************************************
	 * creating a new directory
	 **************************************************************************/
	static public boolean createNewDir(String dirPath)
	{
		File dir = new File(dirPath);

		if (dir.isFile())
		{
			Logger.error("FileStringUtility.createNewDir() - '" + dir.getAbsolutePath() + "' is a file.");
			return false;
		}
		else if (dir.exists())
		{
			Logger.msg(8, "FileStringUtility.createNewDir() - '" + dir.getAbsolutePath() + "' already exists.");
			return false;
		}
		else
		{
			if (!dir.mkdirs())
			{
				Logger.error("FileStringUtility - Could not create new directory '" + dir.getAbsolutePath() + "'");
				return false;
			}
		}
		return true;
	}

	/**************************************************************************
	 * deleting a existing directory
	 **************************************************************************/
	static public boolean deleteDir(String dirPath)
	{
		File dir = new File(dirPath);

		if (!checkDir(dirPath))
		{
			Logger.msg(8, "FileStringUtility.deleteDir() - directory '" + dir.getAbsolutePath() + "' does not exist.");
			return false;
		}

		if (!dir.delete())
		{
			//prints the possible reason
			if (dir.list().length != 0)
			{
				Logger.error("FileStringUtility.deleteDir() - cannot delete non-empty directory '" + dir.getAbsolutePath() + "'");
			}
			else
			{
				Logger.error("FileStringUtility.deleteDir() - directory '" + dir.getAbsolutePath() + "' could not be deleted.");
			}
			return false;
		}

		return true;
	}

	/**************************************************************************
	 * deleting a existing directory with its structure
	 *
	 * @param dirPath the directory which should be deleted
	 * @param force if true forces to delete the entry (ie. the dirPath) even if
	 * it is a file
	 * @param recursive if true deletes the complete directory structure
	 **************************************************************************/
	static public boolean deleteDir(String dirPath, boolean force, boolean recursive)
	{
		File dir = new File(dirPath);
		File files[];

		if (!dir.exists())
		{
			Logger.error("FileStringUtility.deleteDir() - directory '" + dir.getAbsolutePath() + "' does not exist.");
			return false;
		}

		if (dir.isFile())
		{

			if (force)
			{ //delete the entry even if it is a file
				dir.delete();
				return true;
			}
			else
			{
				Logger.error("FileStringUtility.deleteDir() - '" + dir.getAbsolutePath() + "' was a file.");
				return false;
			}
		}

		if (recursive)
		{
			files = dir.listFiles();

			for (File file : files)
				deleteDir(file.getAbsolutePath(), true, true);
		}

		return deleteDir(dirPath);
	}

	/**************************************************************************
	 * List all file names in the directory recursively, relative to the
	 * starting directory.
	 *
	 * @param dirPath starting directory
	 * @param recursive goes into the subdirectories
	 **************************************************************************/
	static public ArrayList<String> listDir(String dirPath, boolean withDirs, boolean recursive)
	{
		ArrayList<String> fileNames = new ArrayList<String>();
		File dir = new File(dirPath);
		File files[];
		String fileName;

		if (!checkDir(dirPath))
		{
			Logger.msg(8, "FileStringUtility.listDir() - directory '" + dir.getAbsolutePath() + "' does not exist.");
			return null;
		}

		files = dir.listFiles();

		for (File file : files) {
			fileName = file.getName();

			if (file.isFile())
			{
				fileNames.add(dirPath + "/" + fileName);
			}
			else
			{
				if (recursive)
					fileNames.addAll(listDir(dirPath + "/" + fileName, withDirs, recursive));

				if (withDirs)
					fileNames.add(dirPath + "/" + fileName);
			}
		}

		return fileNames;
	}

	/**************************************************************************
	 * Open a URL or File as an InputStream
	 **************************************************************************/
	static public InputStream openTextStream(String source)
	{
		java.io.InputStream in = null;
		java.net.URL url = null;

		// Try to open URL connection first
		try
		{
			try
			{
				url = new URL(source);
				in = url.openStream();
			}
			catch (MalformedURLException e)
			{
				// Try to open plain file, if `configFile' is not a
				// URL specification
				in = new FileInputStream(source);
			}
		}
		catch (java.io.IOException ex)
		{
			Logger.error("FileStringUtility.openTextStream() - could not load text stream:" + source);
		}
		return in;
	}

	/**************************************************************************
	 * Load the contents of the configuration file
	 **************************************************************************/
	static public java.util.Properties loadConfigFile(String configFile)
	{
		java.io.BufferedInputStream bin = null;
		java.io.InputStream in = openTextStream(configFile);
		java.util.Properties props = new java.util.Properties();

		if (in != null)
		{
			try
			{
				bin = new java.io.BufferedInputStream(in);
				props.load(bin);
				in.close();
			}
			catch (IOException ex)
			{
				Logger.error("FileStringUtility.loadConfigFile() - could not load configuration file '" + configFile+"'");
			}
		}
		return props;
	}

	/**************************************************************************
	 * Load the contents of the language file
	 * *************************************************************************/
	static public Hashtable<String, String> loadLanguageFile(String configFile)
	{
		try
		{
			String language = FileStringUtility.file2String(configFile);
			Hashtable<String, String> props = new Hashtable<String, String>();
			StringTokenizer tok = new StringTokenizer(language, "\n");
			while (tok.hasMoreTokens())
			{
				String t = tok.nextToken();
				int sep = t.indexOf("=");
				if (sep >0)props.put(t.substring(0,sep),t.substring(sep+1));
			}
			return props;
		}
		catch (Exception e)
		{
			Logger.error("FileStringUtility.loadLanguageFile() - could not load language file '" + configFile+"'");
			Logger.error(e);
			return new Hashtable<String, String>();
		}

	}

	/**************************************************************************
	 * Load the contents of the configuration file
	 **************************************************************************/
	static public void appendConfigFile(java.util.Properties props, String configFile)
	{
		java.io.BufferedInputStream bin = null;
		java.io.InputStream in = openTextStream(configFile);

		if (in != null)
		{
			try
			{
				bin = new java.io.BufferedInputStream(in);
				props.load(bin);
				in.close();
			}
			catch (java.io.IOException ex)
			{
    			Logger.error("FileStringUtility.appendConfigFile() - could not append configuration file '" + configFile+"'");
			}
		}
	}
    public static String convert(String init)
    {
        if (init==null) return null;
        return init
            .replace('\'', '_')
            .replace('\\', '_')
            .replace('/', '_')
            .replace('\"', '_')
            .replace(':', '_')
            .replace('*', '_')
            .replace('?', '_')
            .replace('<', '_')
            .replace('>', '_')
            .replace('|', '_')
            .replace('(', '[')
            .replace(')', ']')
            .replace(',','_');    }
    public static String replaceall(String src,String from,String to)
    {
        StringBuffer tmp=new StringBuffer(src);
        int length = from.length();
        int index = tmp.toString().indexOf(from);
        while (index>0)
        {
            tmp.replace(index,index+length,to);
            index = tmp.toString().indexOf(from);
        }
        return tmp.toString();
    }
    public static String toAlphaNum(String source)
    {
        byte[] sourceb = source.getBytes();
        for (int i=0;i<sourceb.length;i++)
        {
            if (sourceb[i]<'0'||(sourceb[i]>'9'&&sourceb[i]<'A')||(sourceb[i]>'Z'&&sourceb[i]<'_')||sourceb[i]>'z')
                sourceb[i]='_';
        }
        return new String(sourceb);

    }

}
