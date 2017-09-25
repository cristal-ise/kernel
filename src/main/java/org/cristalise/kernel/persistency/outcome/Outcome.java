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
package org.cristalise.kernel.persistency.outcome;

import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * A C2KLocalObject encapsulating management of XML data. It has methods to manipulate and validate the XML,
 * and with a valid ID it can be stored in ClusterStore.
 *
 * It contains lot of utility code to read and set data in the Outcome (xml).
 */
@Accessors(prefix = "m") @Getter @Setter
public class Outcome implements C2KLocalObject {

    private static final int NONE = -1;

    /** ID is the eventID created when the Outcome is stored in History */
    Integer mID;

    /** The Schema object associated with the Outcome */
    Schema mSchema;

    /** The parsed XML document */
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
            Logger.msg(7, "Outcome static class init: "+parser.getClass().getName());
        }
        catch (ParserConfigurationException e) {
            Logger.error(e);
            Logger.die("Cannot function without XML parser");
        }

        XPathFactory xPathFactory = XPathFactory.newInstance();
        xpath = xPathFactory.newXPath();
    }

    /**
     * Use this constructor for XML manipulation only. This Outcome cannot be validate
     * not it can be stored in ClusterStore.
     *
     * @param xml the XML string to be manipulated
     * @throws InvalidDataException there was an error parsing the XML
     */
    public Outcome(String xml) throws InvalidDataException {
        this(NONE, xml, null);
    }

    /**
     * Use this constructor for XML manipulation and validation. This Outcome cannot be stored in ClusterStore.
     *
     * @param xml the XML string to be manipulated
     * @param schema the Schema of the XML
     * @throws InvalidDataException there was an error parsing the XML
     */
    public Outcome(String xml, Schema schema) throws InvalidDataException {
        this(NONE, xml, schema);
    }

    /**
     * Use this constructor to manipulate, validate and store this outcome
     *
     * @param id eventID
     * @param xml the XML string to be manipulated
     * @param schema the Schema of the XML
     * @throws InvalidDataException there was an error parsing the XML
     */
    public Outcome(int id, String xml, Schema schema) throws InvalidDataException {
        this(id, (Document)null, schema);

        try {
            mDOM = parse(xml);
        }
        catch (IOException | SAXException ex) {
            Logger.error("INVALID XML - schema:"+mSchema.getName()+"\n"+xml);
            Logger.error(ex);
            throw new InvalidDataException("XML not valid for schema:"+mSchema+" error:"+ex.getMessage());
        }
    }

    /**
     * Very basic constructor to set all members
     *
     * @param id eventID
     * @param dom parsed XML Document
     * @param schema the Schema instance
     */
    public Outcome(int id, Document dom, Schema schema) {
        mID = id;
        mDOM = dom;
        mSchema = schema;
    }

    /**
     * The constructor derives all the meta data (ID and Schema) from the path
     *
     * @param path the actuals path used by the ClusterStorage
     * @param xml the XML string to parse
     * @throws PersistencyException there was DB error
     * @throws InvalidDataException  Version or EventID was an invalid number
     */
    public Outcome(String path, String xml) throws PersistencyException, InvalidDataException {
        setMetaDataFromPath(path);

        try {
            mDOM = parse(xml);
        }
        catch (IOException | SAXException ex) {
            Logger.error(ex);
            throw new InvalidDataException("XML not valid: "+ex.getMessage());
        }
    }

    /**
     * The constructor derives all the meta data (ID and Schema) from the path
     *
     * @param path the actuals path used by the ClusterStorage
     * @param data the parsed xml Document
     * @throws PersistencyException there was DB error
     * @throws InvalidDataException  Version or EventID was an invalid number
     */
    public Outcome(String path, Document data) throws PersistencyException, InvalidDataException {
        setMetaDataFromPath(path);
        mDOM = data;
    }

    /**
     * Retrieves the SchemaName, Version, EevetnId triplet from the path. Check getClusterPath() implementation
     *
     * @param path the ClusterPath to work with
     * @throws PersistencyException path was incorrect
     * @throws InvalidDataException Schema was not found or the Path has incorrect data
     */
    protected void setMetaDataFromPath(String path) throws PersistencyException, InvalidDataException {
        StringTokenizer tok = new StringTokenizer(path,"/");

        if (tok.countTokens() != 3 && !(tok.nextToken().equals(OUTCOME.getName())))
            throw new PersistencyException("Outcome() - Outcome path must have three components:" + path);

        String schemaName = tok.nextToken();
        String verString  = tok.nextToken();
        String objId      = tok.nextToken();

        try {
            Integer schemaVersion = Integer.valueOf(verString);
            mSchema = LocalObjectLoader.getSchema(schemaName, schemaVersion);
            mID = Integer.valueOf(objId);
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("Outcome() - Version or EventID was an invalid number version:"+verString + " eventID:" + objId);
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw new InvalidDataException("Outcome() - problem loading schema:"+schemaName+" version:"+verString);
        }
    }

    /**
     * Validates the actual XML Document against the provided Schema
     *
     * @return the errors found
     * @throws InvalidDataException Schema was null
     */
    public String validate() throws InvalidDataException {
        if (mSchema == null) {
            mDOM.normalize();
            throw new InvalidDataException("Schema was NOT provided");
        }

        OutcomeValidator validator = OutcomeValidator.getValidator(mSchema);
        return validator.validate(mDOM);
    }

    /**
     * Validates the actual XML Document against the provided Schema
     *
     * @throws InvalidDataException XML document is not valid instance of the Schema
     */
    public void validateAndCheck() throws InvalidDataException {
        String error = validate();

        if (StringUtils.isNotBlank(error)) {
            Logger.error("Outcome.validateAndCheck() - Outcome not valid: " + error);
            Logger.msg("XML: \n"+getData());
            Logger.msg("XSD: \n"+getSchema().getXSD());
            throw new InvalidDataException(error);
        }
    }

    @Override
    public void setName(String name) {
        try {
            mID = Integer.valueOf(name);
        }
        catch (NumberFormatException e) {
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

    /**
     * Retrieves the text, CDATA or attribute value of the Node selected by the XPath
     *
     * @param xpath The path to access the selected Node
     * @return the value of the selected Node
     * @throws XPathExpressionException xpath was not valid (e.g. there is no such node)
     * @throws InvalidDataException xpath result is not text, CDATA or attribute
     */
    public String getFieldByXPath(String xpath) throws XPathExpressionException, InvalidDataException {
        Node field = getNodeByXPath(xpath);

        if (field == null) {
            throw new InvalidDataException("Outcome '"+getSchemaType()+"' cannot resolve xpath:"+xpath);
        }
        else if (field.getNodeType() == Node.TEXT_NODE ||
                field.getNodeType() == Node.CDATA_SECTION_NODE ||
                field.getNodeType() == Node.ATTRIBUTE_NODE)
        {
            return field.getNodeValue();
        }
        else if (field.getNodeType() == Node.ELEMENT_NODE) {
            NodeList fieldChildren = field.getChildNodes();

            if (fieldChildren.getLength() == 0) {
                throw new InvalidDataException("No child/text node for element '"+field.getNodeName()+"'");
            }
            else if (fieldChildren.getLength() == 1) {
                Node child = fieldChildren.item(0);

                if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE)
                    return child.getNodeValue();
                else
                    throw new InvalidDataException("Element '"+field.getNodeName()+"' can't get data from child node of type '"+child.getNodeName()+"'");
            }
            else
                throw new InvalidDataException("Element "+xpath+" has too many children");
        }
        else
            throw new InvalidDataException("Don't know what to do with node '"+field.getNodeName()+"'");
    }

    /**
     * Determines if the NodeList is actually a single field, an element with text data only
     *
     * @param elements NodeList
     * @return if the NodeList has a single field or not
     */
    public boolean isField(NodeList elements) {
        return (elements.getLength() == 1 && elements.item(0).hasChildNodes() && elements.item(0).getFirstChild() instanceof Text);
    }

    /**
     * Sets an Attribute value by name of the given Element. It only updates existing Attributes.
     * If data is null, Element exists and the remove flag is true the node is removed.
     *
     * @param element the Element to search
     * @param name the name of the Attribute
     * @param data the value to set
     * @param remove flag to remove existing node when data is null
     * @throws InvalidDataException the attribute was not found
     */
    public void setAttribute(Element element, String name, String data, boolean remove) throws InvalidDataException {
        if (data == null && remove) {
            Logger.msg(7, "Outcome.setAttribute() - removing name:"+name);

            if (element.hasAttribute(name)) element.removeAttribute(name);
            return;
        }

        if (element.hasAttribute(name)) element.getAttributeNode(name).setValue(data);
        else                            throw new InvalidDataException("Invalid name:'"+name+"'");
    }

    /**
     * Sets an Attribute value by name of the given Element. It only updates existing Attributes.
     *
     * @param element the Element to search
     * @param name the name of the Attribute
     * @param data the value to set
     * @throws InvalidDataException the Attribute was not found
     */
    public void setAttribute(Element element, String name, String data) throws InvalidDataException {
        setAttribute(element, name, data, false);
    }

    /**
     *
     * @param name
     * @param data
     * @param remove
     * @throws InvalidDataException
     */
    public void setAttribute(String name, String data, boolean remove) throws InvalidDataException {
        setAttribute(mDOM.getDocumentElement(), name, data, remove);

    }

    /**
     * Sets an Attribute value by name of the root Element.
     *
     * @param name the name of the Attribute
     * @param data the value to set
     * @throws InvalidDataException the name was not found
     */
    public void setAttribute(String name, String data) throws InvalidDataException {
        setAttribute(name, data, false);
    }

    /**
     *
     * @param field
     * @param name
     * @param data
     * @param remove
     * @throws InvalidDataException
     */
    public void setAttributeOfField(String field, String name, String data, boolean remove) throws InvalidDataException {
        NodeList elements = mDOM.getDocumentElement().getElementsByTagName(field);

        if (isField(elements))
            setAttribute((Element)elements.item(0), name, data, remove);
        else
            throw new InvalidDataException("Invalid name:'"+field+"'");
    }

    /**
     *
     * @param field
     * @param name
     * @param data
     * @throws InvalidDataException
     */
    public void setAttributeOfField(String field, String name, String data) throws InvalidDataException {
        setAttributeOfField(field, name, data, false);
    }

    /**
     *
     * @param element
     * @param name
     * @param data
     * @param remove
     * @throws InvalidDataException
     */
    public void setField(Element element, String name, String data, boolean remove) throws InvalidDataException {
        NodeList elements = element.getElementsByTagName(name);

        if (isField(elements)) {
            if (data == null && remove) {
                Logger.msg(7, "Outcome.setField() - removing name:"+name);
                element.removeChild(elements.item(0));
                return;
            }

            //Setting nodeValue to null could corrupt document
            if (data == null) data = "";

            ((Text)elements.item(0).getFirstChild()).setNodeValue(data);
        }
        else
            throw new InvalidDataException("Invalid name:'"+name+"'");
    }

    /**
     * Sets the textNode value of the named Element of the given Element. It only updates existing Element.
     *
     * @param element Element to use
     * @param name the name of the Element
     * @param data the data to be set
     * @throws InvalidDataException the name was not found or there were more Elements with the given name
     */
    public void setField(Element element, String name, String data) throws InvalidDataException {
        setField(element, name, data, false);
    }

    /**
     *
     * @param name
     * @param data
     * @param remove
     * @throws InvalidDataException
     */
    public void setField(String name, String data, boolean remove) throws InvalidDataException {
        setField(mDOM.getDocumentElement(), name, data, remove);
    }

    /**
     * Sets the textNode value of the named Element of the root Element.
     *
     * @param name the name of the Element
     * @param data the data to be set
     * @throws InvalidDataException the name was not found or there were more Elements with the given name
     */
    public void setField(String name, String data) throws InvalidDataException {
        setField(name, data, false);
    }

    /**
     * Sets the text, CDATA or attribute value of the Node selected by the XPath. It only updates existing Nodes.
     *
     * @param xpath the selected Node to be updated
     * @param data string containing the data
     * @throws XPathExpressionException xpath is invalid
     * @throws InvalidDataException xpath result is not text, CDATA or attribute
     */
    public void setFieldByXPath(String xpath, String data) throws XPathExpressionException, InvalidDataException {
        setFieldByXPath(xpath, data, false);
    }

    /**
     * Sets the text, CDATA or attribute value of the Node selected by the XPath. It only updates existing Nodes.
     * If data is null and the node exists, the node is removed
     *
     * @param xpath the selected Node to be updated
     * @param data string containing the data, it can be null
     * @param remove flag to remove existing node when data is null
     * @throws XPathExpressionException xpath is invalid
     * @throws InvalidDataException xpath result is not text, CDATA or attribute
     */
    public void setFieldByXPath(String xpath, String data, boolean remove) throws XPathExpressionException, InvalidDataException {
        if (StringUtils.isBlank(xpath)) throw new InvalidDataException("Xpath is null or empty string");

        if (data == null && remove) {
            Logger.msg(7, "Outcome.setFieldByXPath() - removing field xpath");

            removeNodeByXPath(xpath);
            return;
        }

        //Setting nodeValue to null could corrupt document
        if (data == null) data = "";

        Node field = getNodeByXPath(xpath);

        if (field == null) {
            Logger.error(getData());
            throw new InvalidDataException("Xpath '"+xpath+"' is invalid");
        }
        else if (field.getNodeType() == Node.ELEMENT_NODE) {
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
                throw new InvalidDataException("Element "+xpath+" must have zero or one children node");
        }
        else if (field.getNodeType() == Node.ATTRIBUTE_NODE)
            field.setNodeValue(data);
        else
            throw new InvalidDataException("Don't know what to do with node "+field.getNodeName());
    }

    /**
     * Append the new Node created from xmlFragment as a child of the Node selected by the XPath
     *
     * @param xpath the selected parent node
     * @param xmlFragment string containing the xml fragment
     * @return the Node just added
     */
    public Node appendXmlFragment(String xpath, String xmlFragment) throws InvalidDataException {
        try {
            Node parentNode = getNodeByXPath(xpath);
            Node newNode = parse(xmlFragment).getDocumentElement();
            return parentNode.appendChild(mDOM.importNode(newNode, true));
        }
        catch (SAXException | IOException | XPathExpressionException e) {
            Logger.error(e);
            throw new InvalidDataException(e.getMessage());
        }
    }

    /**
     * Returns the serialised DOM as a string
     *
     * @return the xml string
     */
    public String getData() {
        try {
            return serialize(mDOM, false);
        }
        catch (InvalidDataException e) {
            Logger.error(e);
            return null;
        }
    }

    @Deprecated
    public String getSchemaType() {
        if (mSchema == null) throw new IllegalArgumentException("Outcome must have valid Schema");

        return mSchema.getName();
    }

    @Deprecated
    public int getSchemaVersion() {
        if (mSchema == null) throw new IllegalArgumentException("Outcome must have valid Schema");

        return mSchema.getVersion();
    }

    /**
     * Returns {@link ClusterType#OUTCOME}
     */
    @Override
    public ClusterType getClusterType() {
        return OUTCOME;
    }

    @Override
    public String getClusterPath() {
        if (mID == null || mID == NONE || mSchema == null) throw new IllegalArgumentException("Outcome must have valid ID and Schema");

        return getClusterType()+"/"+mSchema.getName()+"/"+mSchema.getVersion()+"/"+mID;
    }

    /**
     * Parses the outcome into a DOM tree
     *
     * @param xml string to be parsed
     * @return the parsed Document
     *
     * @throws SAXException error parsing document
     * @throws IOException any IO errors occur
     */
    public static Document parse(String xml) throws SAXException, IOException {
        synchronized (parser) {
            if (xml!=null) return parser.parse(new InputSource(new StringReader(xml)));
            else           return parser.newDocument();
        }
    }

    /**
     * Retrieves an Attribute value by name of the given Element.
     *
     * @param element the Element to query
     * @param name The name of the attribute to retrieve.
     * @return The value as a string, or null if that attribute does not have a specified or default value.
     */
    public String getAttribute(Element element, String name) {
        String value = element.getAttribute(name);

        if (StringUtils.isNotBlank(value)) return value;
        else                               return null;
    }

    /**
     * Retrieves an Attribute value by name of the root Element.
     *
     * @param name The name of the attribute to retrieve.
     * @return The value as a string, or null if that attribute does not have a specified or default value.
     */
    public String getAttribute(String name) {
        return getAttribute(mDOM.getDocumentElement(), name);
    }

    /**
     * Retrieves an Attribute value by name from the named Element.
     *
     * @param field The name of the field.
     * @param attribute The name of the attribute to retrieve.
     * @return The value as a string, or null if that attribute does not have a specified or default value.
     */
    public String getAttributeOfField(String field, String attribute) {
        NodeList elements = mDOM.getDocumentElement().getElementsByTagName(field);

        if (isField(elements)) {
            String value = ((Element)elements.item(0)).getAttribute(attribute);

            if (StringUtils.isNotBlank(value)) return value;
            else                               return null;
        }
        else
            return null;
    }

    /**
     * Retrieves the textNode value of the named Element of the given Element.
     *
     * @param element the Element to query
     * @param name The name of the Element
     * @return The value as a string, or null if that field does not exists
     */
    public String getField(Element element, String name) {
        NodeList elements = element.getElementsByTagName(name);

        if (isField(elements))
            return ((Text)elements.item(0).getFirstChild()).getData();
        else
            return null;
    }

    /**
     * Retrieves the textNode value of the named Element of the root Element.
     *
     * @param name The name of the Element
     * @return The value as a string, or null if that field does not exists
     */
    public String getField(String name) {
        return getField( mDOM.getDocumentElement(), name);
    }

    /**
     * Gets a NodeList selected by the xpath
     *
     * @param xpathExpr the xpath to select the list of Nodes
     * @return NodeList
     * @throws XPathExpressionException invalid xpath
     */
    public NodeList getNodesByXPath(String xpathExpr) throws XPathExpressionException {
        XPathExpression expr = xpath.compile(xpathExpr);
        return (NodeList)expr.evaluate(mDOM, XPathConstants.NODESET);
    }

    /**
     * Gets a List selected by the xpath
     *
     * @param xpathExpr the xpath to select the list of Nodes
     * @return Node
     * @throws XPathExpressionException invalid xpath
     */
    public Node getNodeByXPath(String xpathExpr) throws XPathExpressionException {
        XPathExpression expr = xpath.compile(xpathExpr);
        return (Node)expr.evaluate(mDOM, XPathConstants.NODE);
    }

    /**
     * Removes the node selected by the xpath
     *
     * @param xpathExpr xpath to select the Node
     * @return the Node removed
     * @throws XPathExpressionException invalid xpath
     * @throws InvalidDataException invalid xpath
     */
    public Node removeNodeByXPath(String xpathExpr) throws XPathExpressionException, InvalidDataException {
        if (StringUtils.isBlank(xpathExpr)) throw new InvalidDataException("Xpath is null or empty string");

        Node nodeToTemove = getNodeByXPath(xpathExpr);

        if (nodeToTemove == null) {
            Logger.error("Xpath '"+xpathExpr+"' is invalid\n" + getData());
            throw new InvalidDataException("Xpath '"+xpathExpr+"' is invalid");
        }

        return nodeToTemove.getParentNode().removeChild(nodeToTemove);
    }

    /**
     * Serialize the Given Document
     *
     * @param doc document to be serialized
     * @param prettyPrint if the xml is pretty printed or not
     * @return the xml string
     * @throws InvalidDataException Transformer Exception
     */
    static public String serialize(Document doc, boolean prettyPrint) throws InvalidDataException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
        }
        catch (TransformerConfigurationException ex) {
            Logger.error(ex);
            return "";
        }
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, prettyPrint?"yes":"no");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        Writer out = new StringWriter();
        try {
            transformer.transform(new DOMSource(doc), new StreamResult(out));
        }
        catch (Exception e) {
            Logger.error(e);
            throw new InvalidDataException(e.getMessage());
        }
        return out.toString();
    }

    /**
     * Reads the all Attributes and child Elements of the given Node
     *
     * @param node the node to work with
     * @return a Map as a key/value pairs of Attribute/Element names with their value
     */
    public  Map<String, String> getRecordOfNode(Node node) {
        HashMap<String, String> record = new HashMap<>();
        NodeList elements = node.getChildNodes();

        for (int i = 0; i < elements.getLength(); i++) {
            if (elements.item(i).getNodeType() == Node.ELEMENT_NODE) {
                String name = elements.item(i).getNodeName();
                String value = elements.item(i).getTextContent();

                record.put(name, value);
            }
        }

        NamedNodeMap attrs = node.getAttributes();

        for (int i = 0; i < attrs.getLength(); i++) {
            String name = attrs.item(i).getNodeName();
            String value = attrs.item(i).getTextContent();

            record.put(name, value);
        }

        return record;
    }

    /**
     * Reads the all Attributes and child Elements of the root Element
     *
     * @return a Map as a key/value pairs of Attribute/Element names with their value
     */
    public Map<String, String> getRecord() {
        return getRecordOfNode( mDOM.getDocumentElement() );
    }

    /**
     * Reads the all Attributes and child Elements of the Node selected by the xpath
     *
     * @param xpath the xpath pointing to the Node
     * @return a Map as a key/value pairs of Attribute/Element names with their value
     * @throws XPathExpressionException xpath is invalid
     */
    public Map<String, String> getRecord(String xpath) throws XPathExpressionException {
        return getRecordOfNode( getNodeByXPath(xpath) );
    }

    /**
     * Reads all Attributes and child Elements of the list of Node selected by the xpath
     *
     * @param xpath selecting the list of Nodes
     * @return List of Maps as a key/value pairs of Attribute/Element names with their value
     * @throws XPathExpressionException xpath is invalid
     */
    public List<Map<String, String>> getAllRecords(String xpath) throws XPathExpressionException {
        List< Map<String, String> > records = new ArrayList<>();

        NodeList nodes = getNodesByXPath(xpath);

        for (int i = 0; i < nodes.getLength(); i++) records.add( getRecordOfNode(nodes.item(i)) );

        return records;
    }

    /**
     * Reads list of values of the Attributes and child Elements of the given Element.
     * The values are returned in the order specified in the names parameter. It only return values for the list of names.
     * Null is added to the result if there is no value for the given name.
     *
     * @param element the Element to use
     * @param names the the Attributes and Element names to retrieve
     * @return List of values
     */
    public List<String> getRecordOfElement(Element element, List<String> names) {
        List<String> record = new ArrayList<>();

        for (String name : names) {
            String value = getField(element, name);

            if (value == null)  value = getAttribute(element, name);

            record.add(value);
        }
        return record;
    }

    /**
     * Reads list of values of the Attributes and child Elements of the root Element.
     * The values are returned in the order specified in the names parameter. It only return values for the list of names.
     * Null is added to the result if there is no value for the given name.
     *
     * @param names the the Attributes and Element names to retrieve
     * @return List of values
     */
    public List<String> getRecord(List<String> names) {
        List<String> record = new ArrayList<>();

        for (String name : names) {
            String value = getField(name);

            if (value == null) value = getAttribute(name);

            record.add(value);
        }

        return record;
    }

    /**
     * Reads list of values of the Attributes and child Elements of the Element selected by the xpath.
     * The values are returned in the order specified in the names parameter. It only return values for the list of names.
     * Null is added to the result if there is no value for the given name.
     *
     * @param xpath to select the Element
     * @param names the the Attributes and Element names to retrieve
     * @return List of values
     * @throws XPathExpressionException invalid xpath
     */
    public List<String> getRecord(String xpath, List<String> names) throws XPathExpressionException {
        return getRecordOfElement((Element)getNodeByXPath(xpath), names);
    }

    /**
     * Reads list of list of values of the Attributes and child Elements of the list of Elements selected by the xpath.
     * The values are returned in the order specified in the names parameter. It only return values for the list of names.
     * Null is added to the result if there is no value for the given name.
     *
     * @param xpath to select the list of Element
     * @param names the the Attributes and Element names to retrieve
     * @return List of list of values
     * @throws XPathExpressionException invalid xpath
     */
    public List<List<String>> getAllRecords(String xpath, List<String> names) throws XPathExpressionException {
        List< List<String> > records = new ArrayList<>();
        NodeList nodes = getNodesByXPath(xpath);

        for (int i = 0; i < nodes.getLength(); i++) records.add( getRecordOfElement((Element)nodes.item(i), names) );

        return records;
    }

    /**
     * Sets the values of Attributes and child Element of the root Element. It only updates existing elements.
     *
     * @param record Map with a key/value pairs to fing the fields or attributes to update
     * @throws InvalidDataException the name in the map was invalid
     */
    public void setRecord(Map<String, String> record) throws InvalidDataException {
        for (Entry<String,String> entry : record.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            try {
                setField(name, value);
            }
            catch (InvalidDataException e) {
                setAttribute(name, value);
            }
        }
    }

    public boolean isIdentical(Outcome other) {
        return isIdentical(getDOM(), other.getDOM());
    }

    public static boolean isIdentical(Object orig, Object other) {
        Diff diffIdentical = DiffBuilder.compare(orig).withTest(other)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                .ignoreComments()
                .ignoreWhitespace()
                .checkForIdentical() //.checkForSimilar()
                .build();

        if(diffIdentical.hasDifferences()){
            Logger.warning(diffIdentical.toString());
            return false;
        }
        else {
            return true;
        }
    }
}
