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
package org.cristalise.kernel.entity.proxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.utils.CastorXMLUtility;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;


/******************************************************************************
 * It is a wrapper for the connection and communication with Item
 * It caches data loaded from the Item to reduce communication
 *
 * @version $Revision: 1.25 $ $Date: 2005/05/10 11:40:09 $
 * @author  $Author: abranson $
 ******************************************************************************/
public class ItemProxy
{

    protected Item    				mItem = null;
    protected ItemPath				mItemPath;
    protected org.omg.CORBA.Object  mIOR;
    private final HashMap<MemberSubscription<?>, ProxyObserver<?>>
    								mSubscriptions;
    
   /**************************************************************************
    *  
    **************************************************************************/
    protected ItemProxy( org.omg.CORBA.Object  ior,
                         ItemPath              itemPath)
    {
        Logger.msg(8, "ItemProxy::initialise() - Initialising item proxy " +itemPath);

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

	public Item narrow() throws ObjectNotFoundException
     {
         try {
             return ItemHelper.narrow(mIOR);
         } catch (org.omg.CORBA.BAD_PARAM ex) { }
         throw new ObjectNotFoundException("CORBA Object was not an Item, or the server is down.");
     }
	
	public void initialise( AgentPath agentId,
							PropertyArrayList  itemProps,
                            CompositeActivity  workflow,
                            CollectionArrayList colls
                            )
        throws AccessRightsException, InvalidDataException, PersistencyException, ObjectNotFoundException, MarshalException, ValidationException, IOException, MappingException, InvalidCollectionModification
    {
        Logger.msg(7, "ItemProxy::initialise - started");
        CastorXMLUtility xml = Gateway.getMarshaller();
        if (itemProps == null) throw new InvalidDataException("No initial properties supplied");
        String propString = xml.marshall(itemProps);
        String wfString = "";
        if (workflow != null) wfString = xml.marshall(workflow);
        String collString = "";
        if (colls != null) collString = xml.marshall(colls);
        
        getItem().initialise( agentId.getSystemKey(), propString, wfString, collString);
    }

    public void setProperty(AgentProxy agent, String name, String value)
        throws AccessRightsException,
        PersistencyException, InvalidDataException
    {
        String[] params = new String[2];
        params[0] = name;
        params[1] = value;
        try {
            agent.execute(this, "WriteProperty", params);
        } catch (AccessRightsException e) {
            throw (e);
        } catch (PersistencyException e) {
            throw (e);
        } catch (InvalidDataException e) {
            throw (e);
        } catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException("Could not store property");
        }
    }
    
   /**
    * @throws InvalidCollectionModification 
    *
    **************************************************************************/
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
		if (outcome==null)
			if (thisJob.isOutcomeRequired())
				throw new InvalidDataException("Outcome is required.");
			else
				outcome="";

        if (thisJob.getAgentPath() == null)
            throw new InvalidDataException("No Agent specified.");

        Logger.msg(7, "ItemProxy - executing "+thisJob.getStepPath()+" for "+thisJob.getAgentName());
        return getItem().requestAction (thisJob.getAgentPath().getSystemKey(), thisJob.getStepPath(),
            thisJob.getTransition().getId(), outcome);
    }

   /**************************************************************************
    *
    **************************************************************************/
    private ArrayList<Job> getJobList(AgentPath agentPath, boolean filter)
        throws AccessRightsException,
               ObjectNotFoundException,
               PersistencyException
    {
        JobArrayList thisJobList;
        try {
            String jobs =  getItem().queryLifeCycle(agentPath.getSystemKey(), filter);
            thisJobList = (JobArrayList)Gateway.getMarshaller().unmarshall(jobs);
            }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException("Exception::ItemProxy::getJobList() - Cannot unmarshall the jobs");
        }
        return thisJobList.list;
    }

    public ArrayList<Job> getJobList(AgentProxy agent)
    throws AccessRightsException,
           ObjectNotFoundException,
           PersistencyException
    {
        return getJobList(agent.getPath(), true);
    }

    private Job getJobByName(String actName, AgentPath agent)
    throws AccessRightsException,
           ObjectNotFoundException,
           PersistencyException {

    	ArrayList<Job> jobList = getJobList(agent, true);
    	for (Job job : jobList) {
			if (job.getStepName().equals(actName) && job.hasOutcome())
					return job;
    	}
    	return null;

    }
    
    public Collection<?> getCollection(String collName) throws ObjectNotFoundException {
    	return (Collection<?>)getObject(ClusterStorage.COLLECTION+"/"+collName+"/last");
    }
    
    public Workflow getWorkflow() throws ObjectNotFoundException {
    	return (Workflow)getObject(ClusterStorage.LIFECYCLE+"/workflow");
    }
    
    public Viewpoint getViewpoint(String schemaName, String viewName) throws ObjectNotFoundException {
    	return (Viewpoint)getObject(ClusterStorage.VIEWPOINT+"/"+schemaName+"/"+viewName);
    }
    
    public Job getJobByName(String actName, AgentProxy agent)
    throws AccessRightsException,
           ObjectNotFoundException,
           PersistencyException {
    	return getJobByName(actName, agent.getPath());
    }
    
    /**
     * If this is reaped, clear out the cache for it too.
     */
    @Override
	protected void finalize() throws Throwable {
        Logger.msg(7, "Proxy "+mItemPath+" reaped");
        Gateway.getStorage().clearCache(mItemPath, null);
        Gateway.getProxyManager().removeProxy(mItemPath);
        super.finalize();
    }

   /**************************************************************************
    *
    **************************************************************************/
	public String queryData( String path )
        throws ObjectNotFoundException
    {

    	try {
			Logger.msg(7, "EntityProxy.queryData() - "+mItemPath+"/"+path);
			if (path.endsWith("all")) {
				Logger.msg(7, "EntityProxy.queryData() - listing contents");
				String[] result = Gateway.getStorage().getClusterContents(mItemPath, path.substring(0, path.length()-3));
				StringBuffer retString = new StringBuffer();
				for (int i = 0; i < result.length; i++) {
					retString.append(result[i]);
					if (i<result.length-1) retString.append(",");
				}
				Logger.msg(7, "EntityProxy.queryData() - "+retString.toString());
				return retString.toString();
			}
			C2KLocalObject target = Gateway.getStorage().get(mItemPath, path, null);
			return Gateway.getMarshaller().marshall(target);
        } catch (ObjectNotFoundException e) {
            throw e;
		} catch (Exception e) {
			Logger.error(e);
			return "<ERROR>"+e.getMessage()+"</ERROR>";
		}
    }

    public String[] getContents( String path ) throws ObjectNotFoundException {
        try {
			return Gateway.getStorage().getClusterContents(mItemPath, path.substring(0, path.length()));
		} catch (PersistencyException e) {
			throw new ObjectNotFoundException(e.toString());
		}
    }


   /**************************************************************************
    *
    **************************************************************************/
    public C2KLocalObject getObject( String xpath )
        throws ObjectNotFoundException
    {
        // load from storage, falling back to proxy loader if not found in others
        try
        {
           return Gateway.getStorage().get( mItemPath, xpath , null);
        }
        catch( PersistencyException ex )
        {
        	Logger.msg(4, "Exception loading object :"+mItemPath+"/"+xpath);
            throw new ObjectNotFoundException( ex.toString() );
        }
    }



    public String getProperty( String name )
        throws ObjectNotFoundException
    {
        Logger.msg(5, "Get property "+name+" from item "+mItemPath);
    	Property prop = (Property)getObject("Property/"+name);
    	try
        {
    		return prop.getValue();
    	}
        catch (NullPointerException ex)
        {
    		throw new ObjectNotFoundException();
    	}
    }

    public String getName()
    {
        try {
            return getProperty("Name");
        } catch (ObjectNotFoundException ex) {
            return null;
        }
    }



    
    /**************************************************************************
     * Subscription methods
     **************************************************************************/

    public void subscribe(MemberSubscription<?> newSub) {
    	
    	newSub.setSubject(this);
    	synchronized (this){
            mSubscriptions.put( newSub, newSub.getObserver() );
        }
        new Thread(newSub).start();
        Logger.msg(7, "Subscribed "+newSub.getObserver().getClass().getName()+" for "+newSub.interest);
    }

    public void unsubscribe(ProxyObserver<?> observer)
    {
        synchronized (this){
            for (Iterator<MemberSubscription<?>> e = mSubscriptions.keySet().iterator(); e.hasNext();) {
                MemberSubscription<?> thisSub = e.next();
                if (mSubscriptions.get( thisSub ) == observer) {
                    e.remove();
                    Logger.msg(7, "Unsubscribed "+observer.getClass().getName());
                }
            }
        }
    }

    public void dumpSubscriptions(int logLevel) {
        if (mSubscriptions.size() == 0) return;
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
        Logger.msg(4, "EntityProxy.notify() - Received change notification for "+message.getPath()+" on "+mItemPath);
        synchronized (this){
            if (Gateway.getProxyServer()== null || !message.getServer().equals(Gateway.getProxyServer().getServerName()))
                Gateway.getStorage().clearCache(mItemPath, message.getPath());
            for (Iterator<MemberSubscription<?>> e = mSubscriptions.keySet().iterator(); e.hasNext();) {
                MemberSubscription<?> newSub = e.next();
                if (newSub.getObserver() == null) { // phantom
                    Logger.msg(4, "Removing phantom subscription to "+newSub.interest);
                    e.remove();
                }
                else
                    newSub.update(message.getPath(), message.getState());
            }
        }
    }
}
