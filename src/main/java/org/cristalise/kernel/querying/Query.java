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
package org.cristalise.kernel.querying;

import static org.cristalise.kernel.process.resource.BuiltInResources.QUERY_RESOURCE;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.OutcomeValidator;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ParameterException;
import org.cristalise.kernel.scripting.ScriptParsingException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

@Getter @Setter
public class Query implements DescriptionObject {

    private String      name = "";
    private Integer     version = null;
    private ItemPath    itemPath;
    private String      language;
    private String      query;

    private ArrayList<Parameter> parameters = new ArrayList<Parameter>();

    public Query() {}

    public Query(String n, int v, ItemPath path, String xml) throws QueryParsingException {
        name = n;
        version = v;
        itemPath = path;

        parseXML(xml);
    }

    public Query(String n, int v, String xml) throws QueryParsingException {
        name = n;
        version = v;

        parseXML(xml);
    }

    public Query(String xml) throws QueryParsingException {
        parseXML(xml);
    }

    @Override
    public String getItemID() {
        if (itemPath == null || itemPath.getUUID() == null) return "";
        return itemPath.getUUID().toString();
    }

    public boolean hasParameters() {
        return parameters != null && parameters.size() > 0; 
    }

    public Parameter getParameter(String name) {
        for(Parameter p: parameters) {
            if (p.getName().equals(name)) return p;
        }
        return null;
    }

    public void setStringParameter(String name, Object value) {
        if (value == null) return; 

        Parameter p = getParameter(name);

        if (p == null) {
            p = new Parameter(name, String.class, value);
            parameters.add(p);
        }
        else p.setValue(value);
    }

    public void setParemeterValues(String itemUUID,  Object schemaName, CastorHashMap actProps) {
        for(Parameter p : parameters) p.setValue(actProps.get(p.getName()));

        setMandatoryParemeters(itemUUID, schemaName);
    }

    public void setMandatoryParemeters(String uuid, Object schemaName) {
        setStringParameter("itemUUID",   uuid);
        setStringParameter("schemaName", schemaName);
    }

    public void validateXML(String xml) throws InvalidDataException, ObjectNotFoundException {
        Schema querySchema;

        if (Gateway.getLookup() == null) querySchema = new Schema("Query", 0, Gateway.getResource().getTextResource(null, "boot/OD/Query.xsd"));
        else                             querySchema = LocalObjectLoader.getSchema("Query", 0);

        OutcomeValidator validator = new OutcomeValidator(querySchema);
        String error = validator.validate(xml);

        if (StringUtils.isBlank(error)) {
            Logger.msg(5, "Query.validateXML() - DONE");
        }
        else {
            Logger.error("Query.validateXML() - $error");
            Logger.error("\n============== XML ==============\n" + xml + "\n=================================\n");
            throw new InvalidDataException(error);
        }
    }

    public void parseXML(String xml) throws QueryParsingException {
        if (StringUtils.isBlank(xml) || "<NULL/>".equals(xml)) {
            Logger.warning("Query.parseXML() - query XML was NULL!" );
            return;
        }

        if(Logger.doLog(8)) Logger.msg("Query.parseXML() - xml:\n"+xml);

        try {
            validateXML(xml);

            DocumentBuilder domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document queryDoc = domBuilder.parse(new InputSource(new StringReader(xml)));

            if(queryDoc.getDocumentElement().hasAttribute("name") )    name    = queryDoc.getDocumentElement().getAttribute("name");
            if(queryDoc.getDocumentElement().hasAttribute("version") ) version = Integer.valueOf(queryDoc.getDocumentElement().getAttribute("version"));

            parseQueryTag(queryDoc.getElementsByTagName("query"));
            parseParameterTag(queryDoc.getElementsByTagName("parameter"));
        }
        catch (Exception ex) {
            Logger.error(ex);
            throw new QueryParsingException("Error parsing Query XML : " + ex.toString());
        }
    }

    private void parseQueryTag(NodeList querytList) throws QueryParsingException {
        Element queryElem = (Element)querytList.item(0);

        if (!queryElem.hasAttribute("language")) throw new QueryParsingException("Query data incomplete, must specify language");
        language = queryElem.getAttribute("language");

        Logger.msg(6, "Query.parseQueryTag() - Query Language:" + language);

        // get source from CDATA
        NodeList queryChildNodes = queryElem.getChildNodes();

        if (queryChildNodes.getLength() != 1)
            throw new QueryParsingException("More than one child element found under query tag. Query characters may need escaping - suggest convert to CDATA section");

        if (queryChildNodes.item(0) instanceof Text) query = ((Text) queryChildNodes.item(0)).getData();
        else                                         throw new QueryParsingException("Child element of query tag was not text");

        Logger.msg(6, "Query.parseQueryTag() - query:" + query);
    }

    private void parseParameterTag(NodeList paramList) throws ScriptParsingException, ParameterException, ClassNotFoundException {
        for (int i = 0; i < paramList.getLength(); i++) {
            Element param = (Element)paramList.item(i);

            if (!(param.hasAttribute("name") && param.hasAttribute("type"))) {
                throw new ScriptParsingException("Incomplete Query Parameter: must have name and type");
            }

            parameters.add( new Parameter(param.getAttribute("name"), param.getAttribute("type")) );
        }
    }

    public String getQueryXML() {
        StringBuffer sb = new StringBuffer("<cristalquery name='" + name + "' version='" + version + "'>");

        for(Parameter p: parameters) {
            sb.append("<parameter name='"+p.getName()+"' type='"+p.getType().getName()+"'/>");
        }

        sb.append("<query language='" + language + "'>"+"<![CDATA[" + query + "]]></query>");
        sb.append("</cristalquery>");

        if(Logger.doLog(8)) Logger.msg("Query.getQueryXML() - xml:\n"+sb);

        return sb.toString();
    }

    @Override
    public CollectionArrayList makeDescCollections() throws InvalidDataException, ObjectNotFoundException {
        return new CollectionArrayList();
    }

    @Override
    public void export(Writer imports, File dir) throws InvalidDataException, ObjectNotFoundException, IOException {
        String resType = QUERY_RESOURCE.getTypeCode();

        FileStringUtility.string2File(new File(new File(dir, resType), getName()+(getVersion()==null?"":"_"+getVersion())+".xml"), getQueryXML());

        if (imports == null) return;

        if (Gateway.getProperties().getBoolean("Resource.useOldImportFormat", false)) {
            imports.write("<Resource name='"+getName()+"' "
                    + (getItemPath()==null?"":"id='"+getItemID()+"' ")
                    + (getVersion()==null?"":"version='"+getVersion()+"' ")
                    + "type='"+resType+"'>boot/"+resType+"/"+getName()
                    + (getVersion()==null?"":"_"+getVersion())+".xml</Resource>\n");
        }
        else { 
            imports.write("<QueryResource name='"+getName()+"' "
                    + (getItemPath() == null ? "" : "id='"      + getItemID()  + "' ")
                    + (getVersion()  == null ? "" : "version='" + getVersion() + "'")
                    + "/>\n");
            
        }
    }
}
