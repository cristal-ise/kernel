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

import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.kernel.utils.SoftCache;

/**
 * Manager of pool of Proxies and their subscribers
 *
 */
public class ProxyManager {
    SoftCache<ItemPath, ItemProxy>             proxyPool       = new SoftCache<ItemPath, ItemProxy>(50);
    HashMap<DomainPathSubscriber, DomainPath>  treeSubscribers = new HashMap<DomainPathSubscriber, DomainPath>();
    HashMap<String, ProxyServerConnection>     connections     = new HashMap<String, ProxyServerConnection>();

    /**
     * Create a proxy manager to listen for proxy events and reap unused proxies
     */
    public ProxyManager() {
        Logger.msg(5, "ProxyManager() - Starting.....");

        Iterator<Path> servers = Gateway.getLookup().search(new DomainPath("/servers"), new Property(TYPE, "Server", false));

        while(servers.hasNext()) {
            Path thisServerResult = servers.next();
            try {
                ItemPath thisServerPath = thisServerResult.getItemPath();
                String remoteServer = ((Property)Gateway.getStorage().get(thisServerPath, ClusterStorage.PROPERTY+"/"+NAME, null)).getValue();
                String portStr = ((Property)Gateway.getStorage().get(thisServerPath, ClusterStorage.PROPERTY+"/ProxyPort", null)).getValue();
                int remotePort = Integer.parseInt(portStr);
                connectToProxyServer(remoteServer, remotePort);
            }
            catch (Exception ex) {
                Logger.error("Exception retrieving proxy server connection data for "+thisServerResult);
                Logger.error(ex);
            }
        }
    }

    public void connectToProxyServer(String name, int port) {
        ProxyServerConnection oldConn = connections.get(name);

        if (oldConn != null) oldConn.shutdown();

        connections.put(name, new ProxyServerConnection(name, port, this));
    }

    protected void resubscribe(ProxyServerConnection conn) {
        synchronized (proxyPool) {
            for (ItemPath key : proxyPool.keySet()) {
                ProxyMessage sub = new ProxyMessage(key, ProxyMessage.ADDPATH, false);
                Logger.msg(5, "ProxyManager.resubscribe() - item:"+key);
                conn.sendMessage(sub);
            }
        }
    }

    /**
     * @param sub
     */
    private void sendMessage(ProxyMessage sub) {
        for (ProxyServerConnection element : connections.values()) {
            element.sendMessage(sub);
        }
    }

    public void shutdown() {
        Logger.msg("ProxyManager.shutdown() - flagging shutdown of server connections");
        for (ProxyServerConnection element : connections.values()) {
            element.shutdown();
        }
    }

    protected void processMessage(ProxyMessage thisMessage) throws InvalidDataException {
        if (Logger.doLog(9)) Logger.msg(9, thisMessage.toString());

        if (thisMessage.getPath().equals(ProxyMessage.PINGPATH)) // ping response
            return;

        if (thisMessage.getItemPath() == null) {
            // must be domain path info
            informTreeSubscribers(thisMessage.getState(), thisMessage.getPath());
        }
        else {
            // proper proxy message
            Logger.msg(5, "ProxyManager.processMessage() - Received proxy message: "+thisMessage.toString());
            ItemProxy relevant = proxyPool.get(thisMessage.getItemPath());
            if (relevant == null) {
                Logger.warning("Received proxy message for sysKey "+thisMessage.getItemPath()+" which we don't have a proxy for.");
            }
            else {
                try {
                    relevant.notify(thisMessage);
                }
                catch (Throwable ex) {
                    Logger.error("Error caught notifying proxy listener "+relevant.toString()+" of "+thisMessage.toString());
                    Logger.error(ex);
                }
            }
        }
    }

    private void informTreeSubscribers(boolean state, String path) {
        DomainPath last = new DomainPath(path);
        DomainPath parent; boolean first = true;
        synchronized(treeSubscribers) {
            while((parent = last.getParent()) != null) {
                ArrayList<DomainPathSubscriber> currentKeys = new ArrayList<DomainPathSubscriber>();
                currentKeys.addAll(treeSubscribers.keySet());
                for (DomainPathSubscriber sub : currentKeys) {
                    DomainPath interest = treeSubscribers.get(sub);

                    if (interest!= null && interest.equals(parent)) {
                        if (state == ProxyMessage.ADDED) sub.pathAdded(last);
                        else if (first)                  sub.pathRemoved(last);
                    }
                }
                last = parent;
                first = false;
            }
        }
    }

    public void subscribeTree(DomainPathSubscriber sub, DomainPath interest) {
        synchronized(treeSubscribers) {
            treeSubscribers.put(sub, interest);
        }
    }

    public void unsubscribeTree(DomainPathSubscriber sub) {
        synchronized(treeSubscribers) {
            treeSubscribers.remove(sub);
        }
    }

    private ItemProxy createProxy( org.omg.CORBA.Object ior, ItemPath itemPath) throws ObjectNotFoundException {
        ItemProxy newProxy = null;

        Logger.msg(5, "ProxyManager.createProxy() - Item:" + itemPath);

        if( itemPath instanceof AgentPath ) {
            newProxy = new AgentProxy(ior, (AgentPath)itemPath);
        }
        else {
            newProxy = new ItemProxy(ior, itemPath);
        }

        // subscribe to changes from server
        ProxyMessage sub = new ProxyMessage(itemPath, ProxyMessage.ADDPATH, false);
        sendMessage(sub);
        reportCurrentProxies(9);
        return ( newProxy );
    }

    protected void removeProxy( ItemPath itemPath ) {
        ProxyMessage sub = new ProxyMessage(itemPath, ProxyMessage.DELPATH, true);
        Logger.msg(5,"ProxyManager.removeProxy() - Unsubscribing to proxy informer for "+itemPath);
        sendMessage(sub);
    }


    /**
     * Called by the other GetProxy methods. Fills in either the IOR or the SystemKey
     * 
     * @param ior
     * @param itemPath
     * @return the ItemProx
     * @throws ObjectNotFoundException
     */
    private ItemProxy getProxy( org.omg.CORBA.Object ior, ItemPath itemPath) throws ObjectNotFoundException {
        synchronized(proxyPool) {
            ItemProxy newProxy;
            // return it if it exists
            newProxy = proxyPool.get(itemPath);
            if (newProxy == null) {
                // create a new one
                newProxy = createProxy(ior, itemPath);
                proxyPool.put(itemPath, newProxy);
            }
            return newProxy;
        }
    }

    public ItemProxy getProxy( Path path ) throws ObjectNotFoundException {
        ItemPath itemPath;
        if (path instanceof ItemPath) itemPath = (ItemPath)path;
        else itemPath = path.getItemPath();
        Logger.msg(8,"ProxyManager::getProxy(" + path.toString() + ")");
        return getProxy( itemPath.getIOR(), itemPath );

    }

    public AgentProxy getAgentProxy( AgentPath path ) throws ObjectNotFoundException {
        return (AgentProxy) getProxy(path);
    }

    /**
     * A utility to Dump the current proxies loaded
     * 
     * @param logLevel the selectd log level
     */
    public void reportCurrentProxies(int logLevel) {
        if (!Logger.doLog(logLevel)) return;
        Logger.msg(logLevel, "Current proxies: ");
        try {
            synchronized(proxyPool) {
                Iterator<ItemPath> i = proxyPool.keySet().iterator();

                for( int count=0; i.hasNext(); count++ ) {
                    ItemPath nextProxy = i.next();
                    ItemProxy thisProxy = proxyPool.get(nextProxy);
                    if (thisProxy != null) {
                        Logger.msg(logLevel, ""+count + ": "+proxyPool.get(nextProxy).getClass().getName()+": "+nextProxy);
                    }
                }
            }
        }
        catch (ConcurrentModificationException ex) {
            Logger.msg(logLevel, "Proxy cache modified. Aborting.");
        }
    }
}
