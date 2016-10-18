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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.FileStringUtility;
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
    
    public Query() {}

    public Query(String n, int v, ItemPath path, String q) throws QueryParsingException {
        name = n;
        version = v;
        itemPath = path;
        query = q;

        parseQuery();
    }

    public Query(String n, int v, String q) throws QueryParsingException {
        name = n;
        version = v;
        query = q;

        parseQuery();
    }

    public Query(String q) throws QueryParsingException {
        query = q;

        parseQuery();
    }

   @Override
    public String getItemID() {
        return itemPath.getUUID().toString();
    }

    public void parseQuery() throws QueryParsingException {
        if (StringUtils.isBlank(query) || "<NULL/>".equals(query)) {
            Logger.warning("Query.parseQuery - query XML was NULL!" );
            return;
        }

        Document queryDoc = null;

        // get the DOM document from the XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder domBuilder = factory.newDocumentBuilder();
            queryDoc = domBuilder.parse(new InputSource(new StringReader(query)));

            if(queryDoc.getDocumentElement().hasAttribute("name") )    name    = queryDoc.getDocumentElement().getAttribute("name");
            if(queryDoc.getDocumentElement().hasAttribute("version") ) version = Integer.valueOf(queryDoc.getDocumentElement().getAttribute("version"));

            parseQueryTag (queryDoc.getElementsByTagName("query"));
        }
        catch (Exception ex) {
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
        
        if (queryChildNodes.item(0) instanceof Text)
            query = ((Text) queryChildNodes.item(0)).getData();
        else
            throw new QueryParsingException("Child element of query tag was not text");

        Logger.msg(6, "Query.parseQueryTag() - query:" + query);
    }

    @Override
    public CollectionArrayList makeDescCollections() throws InvalidDataException, ObjectNotFoundException {
        return new CollectionArrayList();
    }

    @Override
    public void export(Writer imports, File dir) throws InvalidDataException, ObjectNotFoundException, IOException {
        String resType = BuiltInResources.QUERY.getName();
        
        //FIXME: this line only saves the actual query, rather than the full XML
        FileStringUtility.string2File(new File(new File(dir, resType), getName()+(getVersion()==null?"":"_"+getVersion())+".xml"), getQuery());

        if (imports!=null) imports.write("<Resource name=\""+getName()+"\" "
                +(getItemPath()==null?"":"id=\""+getItemID()+"\" ")
                +(getVersion()==null?"":"version=\""+getVersion()+"\" ")
                +"type=\""+resType+"\">boot/"+resType+"/"+getName()
                +(getVersion()==null?"":"_"+getVersion())+".xml</Resource>\n");
    }
}
