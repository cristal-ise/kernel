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
package org.cristalise.kernel.lifecycle.instance.predefined;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_VERSION;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.STATE_MACHINE_NAME;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.AgentPredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.predefined.item.ItemPredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.utils.Logger;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/**
 * PredefinedStep are always Active, and have only one transition. 
 * Subclasses could override this method (if necessary)
 */
public abstract class PredefinedStep extends Activity {

    private boolean         isPredefined = false;
    public static final int DONE         = 0;
    public static final int AVAILABLE    = 0;

    public PredefinedStep() {
        super();
        setBuiltInProperty(STATE_MACHINE_NAME, "PredefinedStep");
        setBuiltInProperty(SCHEMA_NAME, "PredefinedStepOutcome");
        setBuiltInProperty(SCHEMA_VERSION, "0");
    }

    @Override
    public boolean getActive() {
        if (isPredefined)
            return true;
        else
            return super.getActive();
    }

    @Override
    public String getErrors() {
        if (isPredefined)
            return getName();
        else
            return super.getErrors();
    }

    @Override
    public boolean verify() {
        if (isPredefined)
            return true;
        else
            return super.verify();
    }

    /**
     * Returns the isPredefined.
     *
     * @return boolean
     */
    public boolean getIsPredefined() {
        return isPredefined;
    }

    /**
     * Sets the isPredefined.
     *
     * @param isPredefined
     *            The isPredefined to set
     */
    public void setIsPredefined(boolean isPredefined) {
        this.isPredefined = isPredefined;
    }

    @Override
    public String getType() {
        return getName();
    }

    static public String getPredefStepSchemaName(String stepName) {
        PredefinedStepContainer[] allSteps = 
            { new ItemPredefinedStepContainer(), new AgentPredefinedStepContainer(), new ServerPredefinedStepContainer() };

        for (PredefinedStepContainer thisContainer : allSteps) {
            String stepPath = thisContainer.getName() + "/" + stepName;
            Activity step = (Activity) thisContainer.search(stepPath);

            if (step != null) {
                return (String) step.getBuiltInProperty(SCHEMA_NAME);
            }
        }
        return "PredefinedStepOutcome"; // default to standard if not found - server may be a newer version
    }

    /**
     * All predefined steps must override this to implement their action
     */
    @Override
    protected abstract String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, Object locker) 
            throws  InvalidDataException,
                    InvalidCollectionModification,
                    ObjectAlreadyExistsException,
                    ObjectCannotBeUpdated,
                    ObjectNotFoundException,
                    PersistencyException,
                    CannotManageException,
                    AccessRightsException;

    // generic bundling of parameters
    static public String bundleData(String[] data) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.newDocument();
            Element root = dom.createElement("PredefinedStepOutcome");
            dom.appendChild(root);

            for (String element : data) {
                Element param = dom.createElement("param");
                Text t = dom.createTextNode(element);
                param.appendChild(t);
                root.appendChild(param);
            }
            return Outcome.serialize(dom, false);
        }
        catch (Exception e) {
            Logger.error(e);
            StringBuffer xmlData = new StringBuffer().append("<PredefinedStepOutcome>");

            for (String element : data)
                xmlData.append("<param><![CDATA[").append(element).append("]]></param>");

            xmlData.append("</PredefinedStepOutcome>");
            return xmlData.toString();
        }
    }

    // generic bundling of single parameter
    static public String bundleData(String data) {
        return bundleData(new String[] { data });
    }

    public static String[] getDataList(String xmlData) {
        try {
            Document scriptDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xmlData)));

            NodeList nodeList = scriptDoc.getElementsByTagName("param");
            String[] result = new String[nodeList.getLength()];

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node n = nodeList.item(i).getFirstChild();

                if      (n instanceof CDATASection) result[i] = ((CDATASection) n).getData();
                else if (n instanceof Text)         result[i] = ((Text) n).getData();
            }
            return result;
        }
        catch (Exception ex) {
            Logger.error(ex);
        }
        return null;
    }
}
