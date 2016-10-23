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
package org.cristalise.storage;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.AgentHelper;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.Item;
import org.cristalise.kernel.entity.ItemHelper;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.Logger;


/**
 * Used by proxies to load clusters by queryData from the Entity.
 * Last client storage - only used if not cached elsewhere
 */
public class ProxyLoader extends ClusterStorage {
    HashMap<ItemPath, Item> entities = new HashMap<ItemPath, Item>();
    Lookup lookup;

    @Override
    public void open(Authenticator auth) throws PersistencyException {
        lookup = Gateway.getLookup();
    }

    @Override
    public void close() throws PersistencyException {
    }

    @Override
    public boolean checkQuerySupport(String language) {
        Logger.warning("ProxyLoader DOES NOT Support any query");
        return false;
    }

    @Override
    public short queryClusterSupport(String clusterType) {
        return READ;
    }

    @Override
    public String getName() {
        return "Proxy Cluster Loader";
    }

    @Override
    public String getId() {
        return "CORBA";
    }

    @Override
    public String executeQuery(Query query) throws PersistencyException {
        throw new PersistencyException("UNIMPLEMENTED funnction");
    }

    /**
     * retrieve object by path
     */
    @Override
    public C2KLocalObject get(ItemPath thisItem, String path) throws PersistencyException {
        try {
            Item thisEntity = getIOR(thisItem);
            String type = getClusterType(path);

            // fetch the xml from the item
            String queryData = thisEntity.queryData(path);

            if (Logger.doLog(8)) Logger.msg("ProxyLoader.get() - "+thisItem+" : "+path+" = "+queryData);

            if (queryData != null) {
                if (type.equals(OUTCOME)) return new Outcome(path, queryData);
                else                      return (C2KLocalObject)Gateway.getMarshaller().unmarshall(queryData);
            }
        }
        catch (ObjectNotFoundException e) {
            return null;
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
        return null;
    }

    /**
     * store object not supported
     */
    @Override
    public void put(ItemPath thisItem, C2KLocalObject obj) throws PersistencyException {
        throw new PersistencyException("Cannot write to items through the ProxyLoader");
    }

    /**
     * delete cluster not supported
     */
    @Override
    public void delete(ItemPath thisItem, String path) throws PersistencyException {
        throw new PersistencyException("Cannot write to items through the ProxyLoader");
    }

    /**
     * Directory listing
     */
    @Override
    public String[] getClusterContents(ItemPath thisItem, String path) throws PersistencyException {
        try {
            Item thisEntity = getIOR(thisItem);
            String contents = thisEntity.queryData(path+"/all");
            StringTokenizer tok = new StringTokenizer(contents, ",");
            String[] result = new String[tok.countTokens()];

            for (int i=0; i<result.length; i++) result[i] = tok.nextToken();

            return result;
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    private Item getIOR(ItemPath thisPath) throws PersistencyException {
        // check the cache
        if (entities.containsKey(thisPath)) {
            Logger.msg(8, "ProxyLoader.getIOR() - "+thisPath+" cached.");
            return entities.get(thisPath);
        }

        try {
            Logger.msg(8, "ProxyLoader.getIOR() - Resolving "+thisPath+".");
            org.omg.CORBA.Object ior = thisPath.getIOR();

            Item thisItem = null;
            try {
                thisItem = ItemHelper.narrow(ior);
            }
            catch (org.omg.CORBA.BAD_PARAM ex) {
                try {
                    thisItem =  AgentHelper.narrow(ior);
                }
                catch (org.omg.CORBA.BAD_PARAM ex2) {
                    throw new PersistencyException ("Could not narrow "+thisItem+" as a known Entity type");
                }
            }

            Logger.msg(8, "ProxyLoader.getIOR() - Found "+thisItem+".");
            entities.put(thisPath, thisItem);
            return thisItem;
        }
        catch (Exception e) {
            throw new PersistencyException("Error narrowing "+thisPath+": "+e.getMessage());
        }
    }
}
