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
package org.cristalise.kernel.lookup;

import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.process.Gateway;

public abstract class Path {
    public static final String delim = "/";

    protected String[] mPath = new String[0];

    /**
     *  slash delimited path
     */
    protected String mStringPath = null;

    public Path() {}

    /**
     * Creates a path with an arraylist of the path (big endian)
     */
    protected Path(String[] path) {
        setPath(path);
    }

    /**
     * Creates a path from a slash separated string (big endian)
     */
    protected Path(String path) {
        setPath(path);
    }

    /**
     * Create a path by appending a child string to an existing path
     */
    protected Path(Path parent, String child) {
        mPath = Arrays.copyOf(parent.getPath(), parent.getPath().length + 1);
        mPath[mPath.length - 1] = child;
    }

    /**
     * String array path e.g. { "Product", "Crystal", "Barrel", "2L", "331013013348" }. 
     * The root node name (i.e. entity, domain or role) is removed from the beginning.
     */
    public void setPath(String[] path) {
        mStringPath = null;
        
        if (path != null && path.length > 0) {
            if(path[0].equals(getRoot())) mPath = Arrays.copyOfRange(path, 1, path.length);
            else                          mPath = path.clone();
        }
        else 
            mPath =  new String[0];
    }

    public void setPath(String path) {
        setPath(StringUtils.split(path, delim));
    }

    /**
     * Returns root as it is defined as 'domain', 'entity' or 'role' in subclasses
     *  
     * @return root as it is defined as 'domain', 'entity' or 'role' in subclasses
     */
    abstract public String getRoot();

    //these methods declared here to provide backward compatibility
    abstract public org.omg.CORBA.Object getIOR();
    abstract public void setIOR(org.omg.CORBA.Object IOR);
    abstract public SystemKey getSystemKey();
    abstract public UUID getUUID();
    abstract public ItemPath getItemPath() throws ObjectNotFoundException;

    /**
     * clones the path object
     */
    public void setPath(Path path) {
        mStringPath = null;
        mPath = (path.getPath().clone());
    }


    public String[] getPath() {
        return mPath;
    }

    public String getStringPath() {
        if (mStringPath == null) {
            if (mPath.length == 0) mStringPath = delim + getRoot();
            else                   mStringPath = delim + getRoot() + delim + StringUtils.join(mPath, delim);
        }
        return mStringPath;
    }

    /**
     * @deprecated bad method name, use getStringPath() instead
     */
    @Deprecated
    public String getString() {
        return getStringPath();
    }

    public boolean exists() {
        if (Gateway.getLookup() == null) return false;
        return Gateway.getLookup().exists(this);
    }

    @Override
    public String toString() {
        return getStringPath();
    }

    @Override
    public boolean equals(Object path) {
        if (path == null) return false;
        return toString().equals(path.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public String dump() {
        StringBuffer comp = new StringBuffer("Components: { ");

        for (String element : mPath) comp.append("'").append(element).append("' ");

        return "Path - dump(): " + comp.toString() + "}\n        string=" + toString() + "\n        uuid=" + getUUID();
    }
}
