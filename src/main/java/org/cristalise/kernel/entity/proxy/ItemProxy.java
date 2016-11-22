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
package org.cristalise.kernel.entity.proxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.cristalise.kernel.collection.BuiltInCollections;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.Item;
import org.cristalise.kernel.entity.ItemHelper;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.agent.JobArrayList;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.BuiltInItemProperties;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.CastorXMLUtility;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;


/**
 * It is a wrapper for the connection and communication with Item.
 * It caches data loaded from the Item to reduce communication
 */
public class ItemProxy 
{
    protected Item                  mItem = null;
    protected ItemPath              mItemPath;
    protected org.omg.CORBA.Object  mIOR;

    private final HashMap<MemberSubscription<?>, ProxyObserver<?>> mSubscriptions;

    protected ItemProxy( org.omg.CORBA.Object  ior, ItemPath itemPath) {
        Logger.msg(8, "ItemProxy::initialise() - path:" +itemPath);

        mIOR            = ior;
        mItemPath       = itemPath;
        mSubscriptions  = new HashMap<MemberSubscription<?>, ProxyObserver<?>>();
    }

    public ItemPath getPath() {
        return mItemPath;
    }

    protected Item getItem() throws ObjectNotFoundException {
        if (mItem == null)
            mItem = narrow();
        return mItem;
    }

    public Item narrow() throws ObjectNotFoundException {
        try {
            return ItemHelper.narrow(mIOR);
        }
        catch (org.omg.CORBA.BAD_PARAM ex) {}

        throw new ObjectNotFoundException("CORBA Object was not an Item, or the server is down.");
    }

    public void initialise( AgentPath           agentId,
                            PropertyArrayList   itemProps,
                            CompositeActivity   workflow,
                            CollectionArrayList colls
                          )
                    throws AccessRightsException, InvalidDataException, PersistencyException, ObjectNotFoundException, MarshalException, ValidationException, IOException, MappingException, InvalidCollectionModification
    {
        Logger.msg(7, "ItemProxy.initialise() - started");

        CastorXMLUtility xml = Gateway.getMarshaller();
        if (itemProps == null) throw new InvalidDataException("ItemProxy.initialise() - No initial properties supplied");
        String propString = xml.marshall(itemProps);

        String wfString = "";
        if (workflow != null) wfString = xml.marshall(workflow);

        String collString = "";
        if (colls != null) collString = xml.marshall(colls);

        getItem().initialise( agentId.getSystemKey(), propString, wfString, collString);
    }

    public void setProperty(AgentProxy agent, String name, String value)
            throws AccessRightsException, PersistencyException, InvalidDataException
    {
        String[] params = new String[2];
        params[0] = name;
        params[1] = value;
        try {
            agent.execute(this, "WriteProperty", params);
        }
        catch (AccessRightsException e) {
            throw (e);
        }
        catch (PersistencyException e) {
            throw (e);
        }
        catch (InvalidDataException e) {
            throw (e);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException("Could not store property");
        }
    }

    public String requestAction( Job thisJob )
            throws AccessRightsException,
                   InvalidTransitionException,
                   ObjectNotFoundException,
                   InvalidDataException,
                   PersistencyException,
                   ObjectAlreadyExistsException, 
                   InvalidCollectionModification
    {
        String outcome = thisJob.getOutcomeString();
        // check fields that should have been filled in

        if (outcome == null) {
            if (thisJob.isOutcomeRequired()) throw new InvalidDataException("Outcome is required.");
            else                             outcome = "";
        }

        if (thisJob.getAgentPath() == null) throw new InvalidDataException("No Agent specified.");

        Logger.msg(7, "ItemProxy.requestAction() - executing "+thisJob.getStepPath()+" for "+thisJob.getAgentName());

        if (thisJob.getDelegatePath() == null)
            return getItem().requestAction (thisJob.getAgentPath().getSystemKey(), thisJob.getStepPath(),
                                            thisJob.getTransition().getId(), outcome);
        else
            return getItem().delegatedAction(thisJob.getAgentPath().getSystemKey(), thisJob.getDelegatePath().getSystemKey(), 
                                             thisJob.getStepPath(), thisJob.getTransition().getId(), outcome);
    }

    private ArrayList<Job> getJobList(AgentPath agentPath, boolean filter)
            throws AccessRightsException, ObjectNotFoundException, PersistencyException
    {
        JobArrayList thisJobList;
        String jobs =  getItem().queryLifeCycle(agentPath.getSystemKey(), filter);

        try {
            thisJobList = (JobArrayList)Gateway.getMarshaller().unmarshall(jobs);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException("Exception::ItemProxy::getJobList() - Cannot unmarshall the jobs");
        }
        return thisJobList.list;
    }

    public ArrayList<Job> getJobList(AgentProxy agent) throws AccessRightsException, ObjectNotFoundException, PersistencyException {
        return getJobList(agent.getPath(), true);
    }

    private Job getJobByName(String actName, AgentPath agent) throws AccessRightsException, ObjectNotFoundException, PersistencyException {
        ArrayList<Job> jobList = getJobList(agent, true);
        for (Job job : jobList) {
            if (job.getStepName().equals(actName) && job.getTransition().isFinishing())
                return job;
        }
        return null;
    }

    /** 
     * Gets the current version of the named Collection
     * 
     * @param collection The built-in collection
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(BuiltInCollections collection) throws ObjectNotFoundException {
        return getCollection(collection, null);
    }

    /** 
     * Gets a numbered version (snapshot) of a collection
     * 
     * @param collection The built-in Collection
     * @param version The collection number. Use null to get the 'last' version.
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(BuiltInCollections collection, Integer version) throws ObjectNotFoundException {
        return getCollection(collection.getName(), version);
    }

    /** 
     * Gets the last version of the named collection
     * 
     * @param collName The collection name
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(String collName) throws ObjectNotFoundException {
        return getCollection(collName, null);
    }

    /** 
     * Gets a numbered version (snapshot) of a collection
     * 
     * @param collName The collection name
     * @param version The collection number. Use null to get the 'last' version.
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(String collName, Integer version) throws ObjectNotFoundException {
        String verStr = version==null?"last":String.valueOf(version);
        return (Collection<?>)getObject(ClusterStorage.COLLECTION+"/"+collName+"/"+verStr);
    }

    /** Gets the Workflow object of this Item
     * 
     * @return the Item's Workflow object
     * @throws ObjectNotFoundException objects were not found
     */
    public Workflow getWorkflow() throws ObjectNotFoundException {
        return (Workflow)getObject(ClusterStorage.LIFECYCLE+"/workflow");
    }

    /** 
     * Gets the named viewpoint
     * 
     * @param schemaName Outcome schema name
     * @param viewName Viewpoint name
     * @return a Viewpoint object
     * @throws ObjectNotFoundException objects were not found
     */
    public Viewpoint getViewpoint(String schemaName, String viewName) throws ObjectNotFoundException {
        return (Viewpoint)getObject(ClusterStorage.VIEWPOINT+"/"+schemaName+"/"+viewName);
    }

    /** 
     * Finds the first finishing job with the given name for the given Agent in the workflow.
     * 
     * @param actName the name of the Activity to look for
     * @param agent The agent to fetch jobs for
     * @return the JOB object or null if nothing was found
     * @throws AccessRightsException Agent has not rights
     * @throws ObjectNotFoundException objects were not found
     * @throws PersistencyException Error loading the relevant objects
     */
    public Job getJobByName(String actName, AgentProxy agent) throws AccessRightsException, ObjectNotFoundException,PersistencyException {
        return getJobByName(actName, agent.getPath());
    }

    /**
     * Finds the Job with the given Activity and Transition name for the Agent in the Items Workflow
     * 
     * @param actName the name of the Activity to look for
     * @param transName the name of the Transition to look for
     * @param agentPath The agent to fetch jobs for
     * @return the JOB object or null if nothing was found
     * @throws AccessRightsException Agent has not rights
     * @throws ObjectNotFoundException objects were not found
     * @throws PersistencyException Error loading the relevant objects
     */
    public Job getJobByTransitionName(String actName, String transName, AgentPath agentPath) throws AccessRightsException, ObjectNotFoundException,PersistencyException {
        for (Job job : getJobList(agentPath, true)) {
            if (job.getStepName().equals(actName) && job.getTransition().getName().equals(transName))
                return job;
        }
        return null;
    }

    /**
     * If this is reaped, clear out the cache for it too.
     */
    @Override
    protected void finalize() throws Throwable {
        Logger.msg(7, "ItemProxy.finalize() - caches are reaped for item:"+mItemPath);
        Gateway.getStorage().clearCache(mItemPath, null);
        Gateway.getProxyManager().removeProxy(mItemPath);
        super.finalize();
    }

    public String queryData( String path ) throws ObjectNotFoundException {
        try {
            Logger.msg(7, "ItemProxy.queryData() - "+mItemPath+"/"+path);

            if (path.endsWith("all")) {
                Logger.msg(7, "ItemProxy.queryData() - listing contents");

                String[] result = Gateway.getStorage().getClusterContents(mItemPath, path.substring(0, path.length()-3));
                StringBuffer retString = new StringBuffer();

                for (int i = 0; i < result.length; i++) {
                    retString.append(result[i]);
                    if (i<result.length-1) retString.append(",");
                }
                Logger.msg(7, "ItemProxy.queryData() - "+retString.toString());
                return retString.toString();
            }
            C2KLocalObject target = Gateway.getStorage().get(mItemPath, path, null);
            return Gateway.getMarshaller().marshall(target);
        }
        catch (ObjectNotFoundException e) {
            throw e;
        }
        catch (Exception e) {
            Logger.error(e);
            return "<ERROR>"+e.getMessage()+"</ERROR>";
        }
    }

    public String[] getContents( String path ) throws ObjectNotFoundException {
        try {
            return Gateway.getStorage().getClusterContents(mItemPath, path);
        }
        catch (PersistencyException e) {
            throw new ObjectNotFoundException(e.toString());
        }
    }

    /**
     * Executes the Query in the target database. The query can be any of these type: SQL/OQL/XQuery/XPath/etc. 
     * 
     * @param query the query to be executed
     * @return the xml result of the query
     * @throws PersistencyException there was a fundamental DB issue
     */
    public String executeQuery(Query query) throws PersistencyException {
        return Gateway.getStorage().executeQuery(query);
    }

    public C2KLocalObject getObject( String xpath ) throws ObjectNotFoundException {
        // load from storage, falling back to proxy loader if not found in others
        try {
            return Gateway.getStorage().get( mItemPath, xpath , null);
        }
        catch( PersistencyException ex ) {
            Logger.error("ItemProxy.getObject() - Exception loading object:"+mItemPath+"/"+xpath);
            Logger.error(ex);
            throw new ObjectNotFoundException( ex.toString() );
        }
    }

    /**
     * Retrieves the values of a BuiltInItemProperty
     * 
     * @param prop one of the Built-In Item Property
     * @return the value of the property
     * @throws ObjectNotFoundException property was not found
     */
    public String getProperty( BuiltInItemProperties prop ) throws ObjectNotFoundException {
        return getProperty(prop.getName());
    }

    /**
     * Retrieves the values of a named property
     * 
     * @param name of the Item Property
     * @return the value of the property
     * @throws ObjectNotFoundException property was not found
     */
    public String getProperty( String name ) throws ObjectNotFoundException {
        Logger.msg(5, "ItemProxy.getProperty() - "+name+" from item "+mItemPath);
        Property prop = (Property)getObject("Property/"+name);

        if(prop != null) return prop.getValue();
        else             throw new ObjectNotFoundException("ItemProxy.getProperty() - COULD not find property "+name+" from item "+mItemPath);
    }

    public String getName() {
        try {
            return getProperty("Name");
        }
        catch (ObjectNotFoundException ex) {
            return null;
        }
    }


    //**************************************************************************
    // Subscription methods
    //**************************************************************************/


    public void subscribe(MemberSubscription<?> newSub) {
        newSub.setSubject(this);
        synchronized (this){
            mSubscriptions.put( newSub, newSub.getObserver() );
        }
        new Thread(newSub).start();
        Logger.msg(7, "ItemProxy.subscribe() - "+newSub.getObserver().getClass().getName()+" for "+newSub.interest);
    }

    public void unsubscribe(ProxyObserver<?> observer) {
        synchronized (this){
            for (Iterator<MemberSubscription<?>> e = mSubscriptions.keySet().iterator(); e.hasNext();) {
                MemberSubscription<?> thisSub = e.next();
                if (mSubscriptions.get( thisSub ) == observer) {
                    e.remove();
                    Logger.msg(7, "ItemProxy.unsubscribed() - "+observer.getClass().getName());
                }
            }
        }
    }

    public void dumpSubscriptions(int logLevel) {
        if(!Logger.doLog(logLevel) || mSubscriptions.size() == 0) return;

        Logger.msg(logLevel, "Subscriptions to proxy "+mItemPath+":");
        synchronized(this) {
            for (MemberSubscription<?> element : mSubscriptions.keySet()) {
                ProxyObserver<?> obs = element.getObserver();
                if (obs != null)
                    Logger.msg(logLevel, "    "+element.getObserver().getClass().getName()+" subscribed to "+element.interest);
                else
                    Logger.msg(logLevel, "    Phantom subscription to "+element.interest);
            }
        }
    }

    public void notify(ProxyMessage message) {
        Logger.msg(4, "ItemProxy.notify() - Received change notification for "+message.getPath()+" on "+mItemPath);
        synchronized (this){
            if (Gateway.getProxyServer()== null || !message.getServer().equals(Gateway.getProxyServer().getServerName())) {
                Gateway.getStorage().clearCache(mItemPath, message.getPath());
            }
            for (Iterator<MemberSubscription<?>> e = mSubscriptions.keySet().iterator(); e.hasNext();) {
                MemberSubscription<?> newSub = e.next();
                if (newSub.getObserver() == null) { // phantom
                    Logger.msg(4, "ItemProxy.notify() - Removing phantom subscription to "+newSub.interest);
                    e.remove();
                }
                else
                    newSub.update(message.getPath(), message.getState());
            }
        }
    }
}
