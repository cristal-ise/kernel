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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class Outcome implements C2KLocalObject {
    Integer mID;
    String mSchemaType;
    int mSchemaVersion;
    Document mDOM;
    static DocumentBuilder parser;
    static XPath xpath;

    static {
    	// Set up parser
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(false);
        try {
            parser = dbf.newDocumentBuilder();
            Logger.msg(1, "DocumentBuilder: "+parser.getClass().getName());
        } catch (ParserConfigurationException e) {
            Logger.error(e);
            Logger.die("Cannot function without XML parser");
        }

        XPathFactory xPathFactory = XPathFactory.newInstance();
        xpath = xPathFactory.newXPath();
    }
    
    public Outcome(int id, String xml, String schemaType, int schemaVersion) throws InvalidDataException {
    	this(id, (Document)null, schemaType, schemaVersion);
    	try {
    		mDOM = parse(xml);
    	} catch (IOException | SAXException ex) {
    		Logger.error(ex);
    		throw new InvalidDataException("XML not valid: "+ex.getMessage());
    	}
    }

    //id is the eventID
    public Outcome(int id, Document dom, String schemaType, int schemaVersion) {
        mID = id;
        mDOM = dom;
        mSchemaType = schemaType;
        mSchemaVersion = schemaVersion;
    }
    
    public Outcome(String path, String xml) throws PersistencyException, InvalidDataException {
    	this(path, (Document)null);
    	try {
    		mDOM = parse(xml);
    	} catch (IOException | SAXException ex) {
    		Logger.error(ex);
    		throw new InvalidDataException("XML not valid: "+ex.getMessage());
    	}
    }

    public Outcome(String path, Document data) throws PersistencyException {
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
        mDOM = data;
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

    public void setData(String xml) throws SAXException, IOException {
        mDOM = parse(xml);
    }

    public void setDOM(Document dom) {
        mDOM = dom;
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
    			field.appendChild(mDOM.createTextNode(data));
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
    	return serialize(mDOM, false);
    }
    
    public Document getDOM() {
    	return mDOM;
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
     * @throws IOException 
     * @throws SAXException 
     */
    public static Document parse(String xml) throws SAXException, IOException {
        synchronized (parser) {
        	if (xml!=null)
        		return parser.parse(new InputSource(new StringReader(xml)));
        	else
        		return parser.newDocument();
        }
    }
    
    public String getField(String name) {
    	 NodeList elements = mDOM.getDocumentElement().getElementsByTagName(name);
    	 if (elements.getLength() == 1 && elements.item(0).hasChildNodes() && elements.item(0).getFirstChild() instanceof Text)
    		 return ((Text)elements.item(0).getFirstChild()).getData();
    	 else
    		 return null;
    }
    
    public NodeList getNodesByXPath(String xpathExpr) throws XPathExpressionException {
    	
    	XPathExpression expr = xpath.compile(xpathExpr);
    	return (NodeList)expr.evaluate(mDOM, XPathConstants.NODESET);
    	
    }
    
    public Node getNodeByXPath(String xpathExpr) throws XPathExpressionException {
    	
    	XPathExpression expr = xpath.compile(xpathExpr);
    	return (Node)expr.evaluate(mDOM, XPathConstants.NODE);
    	
    }

    static public String serialize(Document doc, boolean prettyPrint)
    {
    	TransformerFactory tf = TransformerFactory.newInstance();
    	Transformer transformer;
		try {
			transformer = tf.newTransformer();
		} catch (TransformerConfigurationException ex) {
			Logger.error(ex);
			return "";
		}
    	transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    	transformer.setOutputProperty(OutputKeys.INDENT, prettyPrint?"yes":"no");
    	transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    	
    	Writer out = new StringWriter();
    	try {
			transformer.transform(new DOMSource(doc), new StreamResult(out));
		} catch (TransformerException e) {
			Logger.error(e);
		}
    	return out.toString();
    }
}
