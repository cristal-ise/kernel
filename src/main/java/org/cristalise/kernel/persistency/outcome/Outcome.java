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
package org.cristalise.kernel.persistency.outcome;
import java.io.StringReader;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;


public class Outcome implements C2KLocalObject {
    Integer mID;
    String mData;
    String mSchemaType;
    int mSchemaVersion;
    Document dom;
    static DocumentBuilder parser;
    static DOMImplementationLS impl;
    static XPath xpath;

    static {
    	// Set up parser
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(false);
        try {
            parser = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            Logger.error(e);
            Logger.die("Cannot function without XML parser");
        }

        // Set up serialiser
        try {
			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
	    	impl = (DOMImplementationLS)registry.getDOMImplementation("LS");
		} catch (Exception e) {
            Logger.error(e);
            Logger.die("Cannot function without XML serialiser");
		}
        
        XPathFactory xPathFactory = XPathFactory.newInstance();
        xpath = xPathFactory.newXPath();
    }

    //id is the eventID
    public Outcome(int id, String data, String schemaType, int schemaVersion) {
        mID = id;
        mData = data;
        mSchemaType = schemaType;
        mSchemaVersion = schemaVersion;
    }

    public Outcome(String path, String data) throws PersistencyException {
    // derive all the meta data from the path
        StringTokenizer tok = new StringTokenizer(path,"/");
        if (tok.countTokens() != 3 && !(tok.nextToken().equals(ClusterStorage.OUTCOME)))
            throw new PersistencyException("Outcome() - Outcome path must have three components: "+path);
        mSchemaType = tok.nextToken();
        String verstring = tok.nextToken();
        String objId = tok.nextToken();
        try {
            mSchemaVersion = Integer.parseInt(verstring);
        } catch (NumberFormatException ex) {
            throw new PersistencyException("Outcome() - Outcome version was an invalid number: "+verstring);
        }
        try {
            mID = Integer.valueOf(objId);
        } catch (NumberFormatException ex) {
            mID = null;
        }
        mData = data;
    }

    public void setID(Integer ID) {
        mID = ID;
    }

    public Integer getID() {
        return mID;
    }

    @Override
	public void setName(String name) {
    	try {
    		mID = Integer.valueOf(name);
    	} catch (NumberFormatException e) {
    		Logger.error("Invalid id set on Outcome:"+name);
    	}
    }

    @Override
	public String getName() {
        return mID.toString();
    }

    public void setData(String data) {
        mData = data;
        dom = null;
    }

    public void setData(Document data) {
        dom = data;
        mData = null;
    }
    
    public String getFieldByXPath(String xpath) throws XPathExpressionException, InvalidDataException {
    	Node field = getNodeByXPath(xpath);
    	if (field == null)
    		throw new InvalidDataException(xpath);
    	
    	else if (field.getNodeType()==Node.TEXT_NODE || field.getNodeType()==Node.CDATA_SECTION_NODE)
    		return field.getNodeValue();
    	
    	else if (field.getNodeType()==Node.ELEMENT_NODE) {
    		NodeList fieldChildren = field.getChildNodes();
    		if (fieldChildren.getLength() == 0) 
    			throw new InvalidDataException("No child node for element");
    		
    		else if (fieldChildren.getLength() == 1) {
    			Node child = fieldChildren.item(0);
    			if (child.getNodeType()==Node.TEXT_NODE || child.getNodeType()==Node.CDATA_SECTION_NODE)
    				return child.getNodeValue();
    			else
    				throw new InvalidDataException("Can't get data from child node of type "+child.getNodeName());
    		}
    		else 
    			throw new InvalidDataException("Element "+xpath+" has too many children");
    	}
    	else if (field.getNodeType()==Node.ATTRIBUTE_NODE)		
    		return field.getNodeValue();
    	else
    		throw new InvalidDataException("Don't know what to do with node "+field.getNodeName());
    }
    
    public void setFieldByXPath(String xpath, String data) throws XPathExpressionException, InvalidDataException {
    	Node field = getNodeByXPath(xpath);
    	if (field == null)
    		throw new InvalidDataException(xpath);

    	else if (field.getNodeType()==Node.ELEMENT_NODE) {
    		NodeList fieldChildren = field.getChildNodes();
    		if (fieldChildren.getLength() == 0) {
    			field.appendChild(dom.createTextNode(data));
    		}
    		else if (fieldChildren.getLength() == 1) {
    			Node child = fieldChildren.item(0);
    			switch (child.getNodeType()) {
    			case Node.TEXT_NODE:
    			case Node.CDATA_SECTION_NODE:
    				child.setNodeValue(data);
    				break;
    			default:
    				throw new InvalidDataException("Can't set child node of type "+child.getNodeName());
    			}
    		}
    		else 
    			throw new InvalidDataException("Element "+xpath+" has too many children");
    	}
    	else if (field.getNodeType()==Node.ATTRIBUTE_NODE)		
    		field.setNodeValue(data);
    	else
    		throw new InvalidDataException("Don't know what to do with node "+field.getNodeName());
    }


    public String getData() {
    	if (mData == null && dom != null) {
    		mData = serialize(dom, false);
    	}
        return mData;
    }

    public Schema getSchema() throws ObjectNotFoundException {
    	return LocalObjectLoader.getSchema(mSchemaType, mSchemaVersion);
    }
    
    public void setSchemaType(String schemaType) {
        mSchemaType = schemaType;
    }

    public String getSchemaType() {
        return mSchemaType;
    }

    public int getSchemaVersion() {
        return mSchemaVersion;
    }

    public void setSchemaVersion(int schVer) {
        mSchemaVersion = schVer;
    }

	@Override
	public String getClusterType() {
		return ClusterStorage.OUTCOME;
	}

    // special script API methods

    /**
     * Parses the outcome into a DOM tree
     * @return a DOM Document
     */
    public Document getDOM() {
    	if (dom == null)
        try {
            synchronized (parser) {
            	if (mData!=null)
            		dom = parser.parse(new InputSource(new StringReader(mData)));
            	else
            		dom = parser.newDocument();
            }
        } catch (Exception e) {
            Logger.error(e);
            return null;
        }
    	return dom;
    }
    
    public String getField(String name) {
    	 NodeList elements = getDOM().getDocumentElement().getElementsByTagName(name);
    	 if (elements.getLength() == 1 && elements.item(0).hasChildNodes() && elements.item(0).getFirstChild() instanceof Text)
    		 return ((Text)elements.item(0).getFirstChild()).getData();
    	 else
    		 return null;
    }
    
    public NodeList getNodesByXPath(String xpathExpr) throws XPathExpressionException {
    	
    	XPathExpression expr = xpath.compile(xpathExpr);
    	return (NodeList)expr.evaluate(getDOM(), XPathConstants.NODESET);
    	
    }
    
    public Node getNodeByXPath(String xpathExpr) throws XPathExpressionException {
    	
    	XPathExpression expr = xpath.compile(xpathExpr);
    	return (Node)expr.evaluate(getDOM(), XPathConstants.NODE);
    	
    }

    static public String serialize(Document doc, boolean prettyPrint)
    {
    	LSSerializer writer = impl.createLSSerializer();
    	writer.getDomConfig().setParameter("format-pretty-print", prettyPrint);
    	writer.getDomConfig().setParameter("xml-declaration", false);
    	return writer.writeToString(doc);
    }
}
