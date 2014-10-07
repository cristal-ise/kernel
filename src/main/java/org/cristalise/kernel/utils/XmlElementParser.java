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

import java.io.StringReader;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlElementParser {
	public static String[] parse(String data, String xpath) {
		Vector<String> returnData = new Vector<String>();
		String[] returnArray;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			Logger.error(e);
			throw new RuntimeException("Could not create XML Document Builder");
		}
		StringReader is = new StringReader(data);
		Document doc;
		try {
			doc = builder.parse(new InputSource(is));
		} catch (Exception e) {
			Logger.error(e);
			throw new RuntimeException("Parser malfunction");
		} 
		StringTokenizer pathTokens = new StringTokenizer(xpath, "/");
		int taille = pathTokens.countTokens();
		String[] pathElements = new String[taille];
		int i = taille;
		while (pathTokens.hasMoreElements())
			pathElements[--i] = pathTokens.nextToken();

		if (Logger.doLog(6)) {
			Logger.msg(6, "Path elements:");
			for (String pathElement : pathElements)
				Logger.debug(6, pathElement);
		}

		Logger.msg(6, "Looking for attribute " + pathElements[0] + " in "
				+ pathElements[1]);
		NodeList nl = doc.getElementsByTagName(pathElements[1]);
		for (int j = 0; j < nl.getLength(); j++) {
			Logger.msg(6, "Found one");
			Element e = (Element) nl.item(j);
			boolean match = true;
			Node child = e;
			for (int k = 2; k < taille && match; k++) {
				Logger.msg(6, "Checking parent " + pathElements[k]);
				child = child.getParentNode();
				if (!child.getNodeName().equals(pathElements[k])) {
					Logger.msg(6, "No match for " + child.getNodeName());
					match = false;
				} else
					Logger.msg(6, "Match");
			}
			if (match && e.hasAttribute(pathElements[0])) {
				Logger.msg(
						6,
						"Matching Attribute " + pathElements[0] + "="
								+ e.getAttribute(pathElements[0]));
				returnData.add(e.getAttribute(pathElements[0]));
			}
		}

		Logger.msg(6, "Looking for element " + pathElements[0]);
		nl = doc.getElementsByTagName(pathElements[0]);
		for (int j = 0; j < nl.getLength(); j++) {
			Logger.msg(6, "Found one");
			Element e = (Element) nl.item(j);
			boolean match = true;
			Node child = e;
			for (int k = 1; k < taille && match; k++) {
				Logger.msg(6, "Checking parent " + pathElements[k]);
				child = child.getParentNode();
				if (!child.getNodeName().equals(pathElements[k])) {
					Logger.msg(6, "No match for " + child.getNodeName());
					match = false;
				} else
					Logger.msg(6, "Match");
			}
			if (match) {
				String s = e.getFirstChild().getNodeValue();
				Logger.msg(6, "Found Element " + pathElements[0] + "=" + s);
				if (s != null)
					returnData.add(s);
			}
		}
		Logger.msg(3, returnData.size() + " values found for " + xpath);
		returnArray = new String[returnData.size()];
		for (int j = 0; j < returnArray.length; j++)
			returnArray[j] = returnData.get(j);
		return returnArray;
	}
}
