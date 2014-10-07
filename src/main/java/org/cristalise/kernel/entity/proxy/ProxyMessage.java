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
import java.net.DatagramPacket;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;



/**************************************************************************
 *
 * $Revision: 1.11 $
 * $Date: 2005/05/10 11:40:09 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class ProxyMessage {

    // special server message paths
    public static final String BYEPATH = "bye";
    public static final String ADDPATH = "add";
    public static final String DELPATH = "del";
    public static final String PINGPATH = "ping";
    public static final boolean ADDED = false;
    public static final boolean DELETED = true;

    static ProxyMessage byeMessage = new ProxyMessage(null, BYEPATH, ADDED);
    static ProxyMessage pingMessage = new ProxyMessage(null, PINGPATH, ADDED);

    private ItemPath itemPath = null;
    private String path = "";
    private String server = null;
    private boolean state = ADDED;

    public ProxyMessage() {
        super();
    }
    public ProxyMessage(ItemPath itemPath, String path, boolean state) {
        this();
        setItemPath(itemPath);
        setPath(path);
        setState(state);
    }

    public ProxyMessage(String line) throws InvalidDataException, IOException {
        if (line == null)
            throw new IOException("Null proxy message");
        String[] tok = line.split(":");
        if (tok.length != 2)
            throw new InvalidDataException("String '"+line+"' does not constitute a valid proxy message.");
        if (tok[0].length() > 0 && !tok[0].equals("tree")) {
        	try {
				itemPath = new ItemPath(tok[0]);
			} catch (InvalidItemPathException e) {
				throw new InvalidDataException("Item in proxy message "+line+" was not valid");
			}
        }
        path = tok[1];
        if (path.startsWith("-")) {
            state = DELETED;
            path = path.substring(1);
        }
    }

    public ProxyMessage(DatagramPacket packet) throws InvalidDataException, IOException {
        this(new String(packet.getData()));
    }

    public ItemPath getItemPath() {
        return itemPath;
    }

    public void setItemPath(ItemPath itemPath) {
        this.itemPath = itemPath;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String newPath) {
        this.path = newPath;
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    @Override
	public String toString() {
        return (itemPath==null?"tree":itemPath.getUUID())+":"+(state?"-":"")+path;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }
}
