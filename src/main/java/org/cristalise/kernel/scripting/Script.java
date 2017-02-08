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
package org.cristalise.kernel.scripting;

import static org.cristalise.kernel.collection.BuiltInCollections.INCLUDE;
import static org.cristalise.kernel.process.resource.BuiltInResources.SCRIPT_RESOURCE;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
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

/**
 * 
 */
@Accessors(prefix = "m") @Getter @Setter
public class Script implements DescriptionObject {

    String         mScript     = "";
    CompiledScript mCompScript = null;

    String         mName;
    Integer        mVersion;
    ItemPath       mItemPath;
    String         mLanguage;

    HashMap<String, Parameter> mInputParams    = new HashMap<String, Parameter>();
    HashMap<String, Parameter> mOutputParams   = new HashMap<String, Parameter>();

    /**
     * All declared parameters, including those of imported scripts
     */
    HashMap<String, Parameter> mAllInputParams = new HashMap<String, Parameter>();

    ArrayList<Script> mIncludes = new ArrayList<Script>();

    @Setter(AccessLevel.NONE) @Getter(AccessLevel.NONE)
    ScriptEngine  engine;

    @Setter(AccessLevel.NONE) @Getter(AccessLevel.NONE)
    ScriptContext context;

    /**
     * Constructor for castor unmarshall
     */
    public Script() {}

    /**
     * For testing. Parses a given script xml, instead of loading it from Items.
     */
    public Script(String name, Integer version, ItemPath path, String xml) throws ScriptParsingException, ParameterException {
        mName = name; mVersion = version; mItemPath = path;
        parseScriptXML(xml);
    }

    /**
     * Creates a script executor for the supplied expression, bypassing the xml parsing bit
     * Output class is forced to an object.
     * 
     * @param lang - script language
     * @param expr - the script to run
     * @param returnType Class of the return of the Script
     */
    public Script(String lang, String expr, Class<?> returnType) throws ScriptingEngineException {
        mName = "<expr>";
        setScriptEngine(lang);
        mVersion = null;
        addOutput(null, returnType);
        setScriptData(expr);
    }

    /**
     * Creates a script executor requiring an agent to be set. Used by module event scripts.
     * 
     * @param lang - script language
     * @param name - script name for debugging
     * @param expr - the script to run
     * @param agent - the agentproxy to pass into the script as 'agent'
     */
    public Script(String lang, String name, String expr, AgentProxy agent) throws ScriptingEngineException {
        this(lang, expr, Object.class);
        mName = name;
        addInputParam("agent", AgentProxy.class);
        setInputParamValue("agent", agent);
    }

    public Script(String lang, String expr) throws ScriptingEngineException {
        this(lang, expr, Object.class);
    }

    /**
     * For consoles
     * 
     * @param lang - script language
     * @param agent - AgentProxy of the console user
     * @param out - the output PrintStream for reporting results that don't go to the log
     */
    public Script(String lang, AgentProxy agent, PrintStream out) throws Exception {
        setScriptEngine(lang);
        Bindings beans = context.getBindings(ScriptContext.ENGINE_SCOPE);
        beans.put("storage", Gateway.getStorage());
        beans.put("db", Gateway.getStorage().getDb());
        beans.put("proxy", Gateway.getProxyManager());
        beans.put("lookup", Gateway.getLookup());
        beans.put("orb", Gateway.getORB());
        beans.put("agent", agent);
        beans.put("output", out);
        PrintWriter output = new PrintWriter(out);
        context.setWriter(output);
        context.setErrorWriter(output);

        String scriptText = Gateway.getResource().getTextResource(null, "textFiles/consoleScript."+lang+".txt");

        try {
            Logger.msg(8, "Script() - Loaded consoleScript");
            Logger.msg(8, scriptText);
            engine.put(ScriptEngine.FILENAME, "consoleScript init script");
            engine.eval(scriptText);
        }
        catch (ScriptException ex) {
            //out.println("Exception parsing console script for " + (ns == null ? "kernel" : ns + " module"));
            ex.printStackTrace(out);
        }

        addOutput(null, Object.class);
    }

    /**
     * Adds ItemProxy (object), AgentProxy (subject) and Job to the script input parameters and errors to the output parameters
     * even if these are not defined in the Script XML
     * 
     * @param object ItemProxy representing the Item for the Job
     * @param subject AgentProxy representing executing Agent
     * @param job Job to be executed
     */
    public void setActExecEnvironment(ItemProxy object, AgentProxy subject, Job job) 
            throws ScriptingEngineException, InvalidDataException
    {
        // set environment - this needs to be well documented for script developers
        if (!mInputParams.containsKey("item")) {
            Logger.warning("Item param not declared in Script "+getName()+" v"+getVersion());
            addInputParam("item", ItemProxy.class);
        }
        setInputParamValue("item", object);

        if (!mInputParams.containsKey("agent")) {
            Logger.warning("Agent param not declared in Script "+getName()+" v"+getVersion());
            addInputParam("agent", AgentProxy.class);
        }
        setInputParamValue("agent", subject);

        if (!mInputParams.containsKey("job")) {
            Logger.warning("Job param not declared in Script "+getName()+" v"+getVersion());
            addInputParam("job", Job.class);
        }
        setInputParamValue("job", job);

        if (!mOutputParams.containsKey("errors")) {
            Logger.warning("Errors output not declared in Script "+getName()+" v"+getVersion());
            addOutput("errors", ErrorInfo.class);
        }
    }

    /**
     * Sets the language
     * 
     * @param requestedLang the language
     */
    public void setScriptEngine(String requestedLang) throws ScriptingEngineException {
        String lang = Gateway.getProperties().getString("OverrideScriptLang."+requestedLang, requestedLang);

        ScriptEngineManager sem = (ScriptEngineManager)Gateway.getProperties().getObject("Script.EngineManager");

        if (sem == null) sem = new ScriptEngineManager(getClass().getClassLoader());
        engine = sem.getEngineByName(lang);

        if (engine == null) throw new ScriptingEngineException("No script engine for '"+lang+"' found.");

        mLanguage = requestedLang;

        context = new SimpleScriptContext();
        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        engine.setContext(context);
    }

    /**
     * 
     * @param context  {@link ScriptContext}
     */
    public void setContext(ScriptContext context) {
        this.context = context;
        if (engine != null) engine.setContext(context);
    }

    /**
     * 
     * @return {@link ScriptContext}
     */
    public ScriptContext getContext() {
        return context;
    }

    /**
     * Extracts script data from script xml.
     * 
     * TODO: implement XML marshall/unmarshall with CASTOR 
     *
     * @param scriptXML
     * @throws ScriptParsingException - when script is invalid
     */
    private void parseScriptXML(String scriptXML) throws ScriptParsingException, ParameterException {
        if (StringUtils.isBlank(scriptXML)) {
            Logger.warning("Script.parseScriptXML - scriptXML was NULL!" );
            return;
        }
        
        Document scriptDoc = null;

        // get the DOM document from the XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder domBuilder = factory.newDocumentBuilder();
            scriptDoc = domBuilder.parse(new InputSource(new StringReader(scriptXML)));
        }
        catch (Exception ex) {
            throw new ScriptParsingException("Error parsing Script XML : " + ex.toString());
        }

        parseScriptTag (scriptDoc.getElementsByTagName("script"));
        parseIncludeTag(scriptDoc.getElementsByTagName("include"));
        parseParamTag  (scriptDoc.getElementsByTagName("param"));
        parseOutputTag (scriptDoc.getElementsByTagName("output"));
    }

    private void parseOutputTag(NodeList outputList) throws ScriptParsingException, ParameterException {
        for (int i=0; i<outputList.getLength(); i++) {
            Element output = (Element)outputList.item(i);

            if (!output.hasAttribute("type")) {
                throw new ScriptParsingException("Script Output declaration incomplete, must have type");
            }

            addOutput(output.getAttribute("name"), output.getAttribute("type"));
        }
    }

    private void parseParamTag(NodeList paramList) throws ScriptParsingException, ParameterException {
        for (int i=0; i<paramList.getLength(); i++) {
            Element param = (Element)paramList.item(i);

            if (!(param.hasAttribute("name") && param.hasAttribute("type"))) {
                throw new ScriptParsingException("Script Input Param incomplete, must have name and type");
            }

            addInputParam(param.getAttribute("name"), param.getAttribute("type"));
        }
    }

    private void parseIncludeTag(NodeList includeList) throws ScriptParsingException {
        for (int i=0; i<includeList.getLength(); i++) {
            Element include = (Element)includeList.item(i);

            if (!(include.hasAttribute("name") && include.hasAttribute("version")))
                throw new ScriptParsingException("Script include declaration incomplete, must have name and version");

            String includeName = include.getAttribute("name");
            String includeVersion =  include.getAttribute("version");

            try {
                Script includedScript = LocalObjectLoader.getScript(includeName, Integer.parseInt(includeVersion));
                includedScript.setContext(context);
                mIncludes.add(includedScript);

                for (Parameter includeParam : includedScript.getInputParams().values()) {
                    addIncludedInputParam(includeParam.getName(), includeParam.getType());
                }
            }
            catch (NumberFormatException e) {
                throw new ScriptParsingException("Invalid version in imported script name:'"+includeName+"', version:'"+includeVersion+"'");
            }
            catch (ScriptingEngineException e) {
                Logger.error(e);
                throw new ScriptParsingException("Error parsing imported script "+includeName+" v"+includeVersion+": "+e.getMessage());
            }
            catch (ObjectNotFoundException e) {
                Logger.error(e);
                throw new ScriptParsingException("Error parsing imported script "+includeName+" v"+includeVersion+" not found.");
            }
            catch (InvalidDataException e) {
                Logger.error(e);
                throw new ScriptParsingException("Error parsing imported script "+includeName+" v"+includeVersion+" was invalid: "+e.getMessage());
            }
        }
    }

    private void parseScriptTag(NodeList scriptList) throws ScriptParsingException {
        Element scriptElem = (Element)scriptList.item(0);

        if (!scriptElem.hasAttribute("language")) throw new ScriptParsingException("Script data incomplete, must specify scripting language");

        Logger.msg(6, "Script.parseScriptTag() - Script Language: " + scriptElem.getAttribute("language"));

        try {
            setScriptEngine(scriptElem.getAttribute("language"));
        }
        catch (ScriptingEngineException ex) {
            throw new ScriptParsingException(ex.getMessage());
        }

        // get script source from CDATA
        NodeList scriptChildNodes = scriptElem.getChildNodes();

        if (scriptChildNodes.getLength() != 1)
            throw new ScriptParsingException("More than one child element found under script tag. Script characters may need escaping - suggest convert to CDATA section");
        
        if (scriptChildNodes.item(0) instanceof Text)
            setScriptData(((Text) scriptChildNodes.item(0)).getData());
        else
            throw new ScriptParsingException("Child element of script tag was not text");

        Logger.msg(6, "Script.parseScriptTag() - script:" + mScript);
    }

    /**
     * 
     * @param name
     * @param type
     * @throws ParameterException
     */
    protected void addInputParam(String name, String type) throws ParameterException {
        try {
            addInputParam(name, Gateway.getResource().getClassForName(type));
        }
        catch (ClassNotFoundException ex) {
            throw new ParameterException("Input parameter " + name + " specifies class " + type + " which was not found.");
        }
    }

    protected void addInputParam(String name, Class<?> type) throws ParameterException {
        Parameter inputParam = new Parameter(name, type);

        Logger.msg(6, "ScriptExecutor.addInputParam() - declared parameter " + name + " (" + type + ")");
        //add parameter to hashtable
        mInputParams.put(inputParam.getName(), inputParam);
        mAllInputParams.put(inputParam.getName(), inputParam);

    }

    /**
     * 
     * @param name
     * @param type
     * @throws ParameterException
     */
    protected void addIncludedInputParam(String name, Class<?> type) throws ParameterException {
        // check if we already have it
        if (mAllInputParams.containsKey(name)) {
            Parameter existingParam = mAllInputParams.get(name);
            // check the types match
            if (existingParam.getType() == type)
                return; // matches
            else // error
                throw new ParameterException("Parameter conflict. Parameter'"+name+"' is declared as  "
                        +existingParam.getType().getName()+" is declared in another script as "+type.getName());
        }

        Parameter inputParam = new Parameter(name);
        inputParam.setType(type);

        //add parameter to hashtable
        mAllInputParams.put(inputParam.getName(), inputParam);
    }

    /**
     * 
     * @param name
     * @param type
     * @throws ParameterException
     */
    protected void addOutput(String name, String type) throws ParameterException {
        try {
            addOutput(name, Gateway.getResource().getClassForName(type));
        }
        catch (ClassNotFoundException ex) {
            throw new ParameterException("Output parameter " + name + " specifies class " + type + " which was not found.");
        }
    }

    /**
     * 
     * @param name
     * @param type
     * @throws ParameterException
     */
    protected void addOutput(String name, Class<?> type) throws ParameterException {
        if (mOutputParams.containsKey(name)) {
            throw new ParameterException("Output parameter '"+name+"' declared more than once.");
        }

        mOutputParams.put(name, new Parameter(name, type));
    }

    /**
     * Submits an input parameter to the script. Must be declared by name and type in the script XML.
     *
     * @param name - input parameter name from the script xml
     * @param value - object to use for this parameter
     * @return if the input parameter was used or not
     * @throws ParameterException - name not found or wrong type
     */
    public boolean setInputParamValue(String name, Object value) throws ParameterException {
        Parameter param = mInputParams.get(name);
        boolean wasUsed = false;
        
        if (!mAllInputParams.containsKey(name)) return false;

        if (param != null) { // param is in this script
            if (!param.getType().isInstance(value)) {
                throw new ParameterException( "Parameter "+name+" in script "+mName+" v"+mVersion+" is wrong type \n"+
                        "Required: "+param.getType().toString()+"\n"+"Supplied: "+value.getClass().toString());
            }
            context.getBindings(ScriptContext.ENGINE_SCOPE).put(name, value);
            Logger.msg(7, "Script.setInputParamValue() - " + name + ": " + value);
            param.setInitialised(true);
            wasUsed = true;
        }

        // pass param down to child scripts
        for (Script importScript : mIncludes) wasUsed |= importScript.setInputParamValue(name, value);

        return wasUsed;
    }

    /**
     * Reads and evaluates input properties, set input parameters from those properties and executes the Script
     * 
     * @param itemPath the Item context
     * @param inputProps imput properties
     * @param actContext activity path
     * @param locker transaction locker
     * @return the values returned by the Script
     */
    public Object evaluate(ItemPath itemPath, CastorHashMap inputProps, String actContext, Object locker) throws ScriptingEngineException {
        try {
            for (String inputParamName: getAllInputParams().keySet()) {
                if (inputProps.containsKey(inputParamName)) {
                    setInputParamValue(inputParamName, inputProps.evaluateProperty(itemPath, inputParamName, actContext, locker));
                }
            }

            if (getAllInputParams().containsKey("item"))
                setInputParamValue("item", Gateway.getProxyManager().getProxy(itemPath));

            if (getAllInputParams().containsKey("agent"))
                setInputParamValue("agent", Gateway.getProxyManager().getProxy(Gateway.getLookup().getAgentPath("system")));

            Object retVal = execute();

            if (retVal == null) retVal = "";

            Logger.msg(2, "Script.evaluate("+getName()+") - Script returned:'"+retVal+"'");

            return retVal;
        }
        catch (Exception e) {
            Logger.error("Script.evaluate() - Script:" + getName());
            Logger.error(e);
            throw new ScriptingEngineException(e.getMessage());
        }
    }


   /**
     * Executes the script with the submitted parameters. All declared input parametes should have been set first.
     *
     * @return The return value depends on the way the output type was declared in the script xml.
     * <ul><li>If there was no output class declared then null is returned
     * <li>If a class was declared, but not named, then the object returned by the script is checked
     * to be of that type, then returned.
     * <li>If the output value was named and typed, then an object of that class is created and
     * passed to the script as an input parameter. The script should set this before it returns.
     * </ul>
     * @throws ScriptingEngineException - input parameters weren't set, there was an error executing the script, or the output was invalid
     */
    public Object execute() throws ScriptingEngineException {
        //TODO: Split Script.execute() to several logically independent methods to make it easier to maintain
        executeIncludedScripts();

        StringBuffer missingParams = new StringBuffer();

        //check input params
        for (Parameter thisParam : mInputParams.values()) {
            if (!thisParam.getInitialised()) missingParams.append(thisParam.getName()).append("\n");
        }

        // croak if any missing
        if (missingParams.length() > 0) {
            throw new ScriptingEngineException("Execution aborted, the following declared parameters were not set: \n" + missingParams.toString());
        }

        initOutputParams();

        // run the script
        Object returnValue = null;
        try {
            Logger.msg(7, "Script.execute() - Executing script:"+getName());
            if (Logger.doLog(8)) Logger.msg("Script:\n"+mScript);

            if (engine == null) {
                throw new ScriptingEngineException("Script engine not set. Cannot execute scripts.");
            }

            engine.put(ScriptEngine.FILENAME, mName);

            if (mCompScript != null) returnValue = mCompScript.eval(context);
            else                     returnValue = engine.eval(mScript);

            Logger.msg(7, "Script.execute("+getName()+") - script returned '" + returnValue + "'");
        }
        catch (Throwable ex) {
            Logger.error(ex);
            throw new ScriptingEngineException("Error executing script "+getName()+": " + ex.getMessage());
        }

        // if no outputs are defined, return null
        if (mOutputParams.size() == 0) {
            Logger.msg(4, "Script.execute() - No output params. Returning null.");
            return null;
        }

        return packScriptReturnValue(returnValue);
    }

    /**
     * Executes included script and use output parameters as inputs parameters if their name match
     * 
     * @throws ScriptingEngineException execute() thrown exception
     */
    @SuppressWarnings("unchecked")
    private void executeIncludedScripts() throws ScriptingEngineException {
        for (Script importScript : mIncludes) {
            Logger.msg(5, "Script.executeIncludedScripts() - name:"+importScript.getName()+" version:"+importScript.getVersion());

            // execute the icluded scripts first, they might set input parameters
            Object output = importScript.execute();

            if (output == null || !(output instanceof Map)) return;

            ((Map<String, Object>)output).forEach((outputKey, outputValue) -> {
                if (mInputParams.containsKey(outputKey)) {
                    try {
                        Logger.msg(5, "Script.executeIncludedScripts() - setting inputs for parameter:"+outputKey);
                        setInputParamValue(outputKey, outputValue);
                    }
                    catch (ParameterException e) {
                        Logger.error(e);
                    }
                }
            });
        }
    }

    /**
     * Initialise the output parameters before execution
     */
    private void initOutputParams() {
        for (Parameter outputParam : mOutputParams.values()) {
            //If the name is null then it's the return type. don't pre-register it
            if (outputParam.getName() == null || outputParam.getName().length() == 0) continue; 

            Logger.msg(8, "Script.initOutputParams() - Initialising output bean '" + outputParam.getName() + "'");

            Object emptyObject;
            try {
                emptyObject = outputParam.getType().newInstance();
            }
            catch (Exception e) {
                emptyObject = null;
            }
            context.getBindings(ScriptContext.ENGINE_SCOPE).put(outputParam.getName(), emptyObject);
        }
    }

    /**
     * Packs the outputs of the Script to a return value
     * 
     * @param returnValue value is returned by engine.eval()
     * @return returns the returnValue when a single output was defined with no name 
     *         or a HashMap with data taken from the bindings using the output name
     * @throws ScriptingEngineException
     */
    private Object packScriptReturnValue(Object returnValue) throws ScriptingEngineException {
        HashMap<String, Object> outputs = new HashMap<String, Object>();

        for (Parameter outputParam : mOutputParams.values()) {
            String outputName = outputParam.getName();

            //return the value when a single output was defined with no name
            if (mOutputParams.size() == 1 && StringUtils.isBlank(outputName)) return returnValue;

            if (StringUtils.isBlank(outputName)) throw new ScriptingEngineException("Script "+getName()+" - All outputs must have a name.");

            //otherwise take data from the bindings using the output name
            Object outputValue = context.getBindings(ScriptContext.ENGINE_SCOPE).get(outputParam.getName());

            Logger.msg(4, "Script.packScriptReturnValue() - Output "+ outputName+"= "+(outputValue==null ? "null" : outputValue.toString()));

            // check the class
            if (outputValue != null && !(outputParam.getType().isInstance(outputValue)))  {
                throw new ScriptingEngineException("Script "+getName()+" - output "+outputName+" was not null and it was not instance of " + outputParam.getType().getName() + ", it was a " + outputValue.getClass().getName());    
            }

            outputs.put(outputParam.getName(), outputValue);
        }
        return outputs;
    }

    public void setScriptData(String script) throws ScriptParsingException {
        mScript = script;
        if (engine instanceof Compilable) {
            try {
                Logger.msg(1, "Script.setScriptData() - Compiling script "+mName);
                engine.put(ScriptEngine.FILENAME, mName);
                mCompScript = ((Compilable)engine).compile(mScript);
            }
            catch (ScriptException e) {
                Logger.error(e);
                throw new ScriptParsingException(e.getMessage());
            }
        }
    }

    public String getScriptData() {
        return mScript;
    }

    @Override
    public String getItemID() {
        return mItemPath.getUUID().toString();
    }

    /**
     * Resolves the Script object using its name and version. If Version is null tries to interpret the name 
     * as an expression
     * 
     * @see BuiltInVertexProperties#ROUTING_EXPR
     * 
     * @param name the name of the Script Item or an expression
     * @param version the version of the Script. If set to null
     * @return {@link Script}
     */
    public static Script getScript(String name, Integer version) 
            throws ScriptingEngineException, ObjectNotFoundException, InvalidDataException
    {
        if (name == null || name.length() == 0) throw new ScriptingEngineException("Script name is empty");

        if (version != null) {
            return LocalObjectLoader.getScript(name, version);
        }
        else {
            // empty version: try expression
            String[] tokens = name.split(":");

            if(tokens.length == 2) return new Script(tokens[0], tokens[1]);
            else                   throw new InvalidDataException("Data '"+name+"' cannot be interpreted as expression");
        }
    }

    /**
     * Method for castor marshall
     * 
     * @return list of Include objects
     */
    public ArrayList<Include> getIncludes() {
        //FIXME: CASTOR MARSHALLIN DOES NOT WORK YET
        ArrayList<Include> returnList = new ArrayList<Include>();
        for(Script s: mIncludes) {
            returnList.add(new Include(s.getName(), s.getVersion()));
        }
        return returnList;
    }

    /**
     * Method for castor unmarshall
     * 
     * @param includes included Scripts
     */
    public void setIncludes(ArrayList<Include> includes) throws ObjectNotFoundException, InvalidDataException, ParameterException, ScriptParsingException {
        //FIXME: CASTOR MARSHALLIN DOES NOT WORK YET
        for(Include i: includes) {
//            Script includedScript = LocalObjectLoader.getScript(i.name, i.version);
            Script includedScript = new Script(i.name, i.version, null, null);
            includedScript.setContext(context);
            mIncludes.add(includedScript);

            for (Parameter includeParam : includedScript.getInputParams().values()) {
                addIncludedInputParam(includeParam.getName(), includeParam.getType());
            }
        }
    }

    /**
     * 
     */
    @Override
    public CollectionArrayList makeDescCollections() throws InvalidDataException, ObjectNotFoundException {
        CollectionArrayList retArr = new CollectionArrayList();
        Dependency includeColl = new Dependency(INCLUDE);
        for (Script script : mIncludes) {
            try {
                includeColl.addMember(script.getItemPath());
            } catch (InvalidCollectionModification e) {
                throw new InvalidDataException("Could not add "+script.getName()+" to description collection. "+e.getMessage());
            } catch (ObjectAlreadyExistsException e) {	
                throw new InvalidDataException("Script "+script.getName()+" included more than once.");
            } // 
        }
        retArr.put(includeColl);
        return retArr;
    }

    /**
     * 
     */
    @Override
    public void export(Writer imports, File dir) throws IOException {
        String tc = SCRIPT_RESOURCE.getTypeCode();

        FileStringUtility.string2File(new File(new File(dir, tc), getName()+(getVersion()==null?"":"_"+getVersion())+".xml"), getScriptData());

        if (imports!=null) imports.write("<Resource name='"+getName()+"' "
                +(getItemPath()==null?"":"id='"+getItemID()+"' ")
                +(getVersion()==null?"":"version='"+getVersion()+"' ")
                +"type='"+tc+"'>boot/"+tc+"/"+getName()
                +(getVersion()==null?"":"_"+getVersion())+".xml</Resource>\n");
    }

    static public void main(String[] args) {
        for(ScriptEngineFactory sef: new ScriptEngineManager().getEngineFactories()) {
            System.out.println(sef.getEngineName()+" v"+sef.getEngineVersion()+" using "+sef.getLanguageName()+" v"+sef.getLanguageVersion()+" "+sef.getNames());
        }
        System.out.println("Preferred javascript engine: "+new ScriptEngineManager().getEngineByName("javascript").getClass().getName());
    }
}
