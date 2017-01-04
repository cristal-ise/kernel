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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.UUID;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.process.Gateway;

public abstract class Path {
    public static final String delim = "/";

    protected enum Type {UNKNOWN, CONTEXT, ITEM;};

    /*
    public static final short UNKNOWN = 0;
    public static final short CONTEXT = 1;
    public static final short ITEM    = 2;
    */

    protected String[] mPath = new String[0];

    /**
     *  slash delimited path
     */
    protected String mStringPath = null;
    /**
     *  entity or context
     */
    protected Type mType = Type.UNKNOWN;

    public Path() {}

    /**
     * Creates an empty path
     */
    public Path(Type type) {
        mType = type;
    }

    /**
     * Creates a path with an arraylist of the path (big endian)
     */
    public Path(String[] path, Type type) {
        this(type);
        setPath(path);
    }

    /**
     * Creates a path from a slash separated string (big endian)
     */
    public Path(String path, Type type) {
        this(type);
        setPath(path);
    }

    /**
     * Create a path by appending a child string to an existing path
     */
    public Path(Path parent, String child, Type type) {
        this(type);
        mPath = Arrays.copyOf(parent.getPath(), parent.getPath().length + 1);
        mPath[mPath.length - 1] = child;
    }

    /**
     * Create a path by appending a child
     */
    public Path(Path parent, String child) {
        this(parent, child, Type.UNKNOWN);
    }

    /**
     * string array path e.g. { "Product", "Crystal", "Barrel", "2L", "331013013348" } system/domain node ABSENT
     */
    public void setPath(String[] path) {
        mStringPath = null;
        mPath = path.clone();
    }

    /**
     * string path e.g. /system/d000/d000/d001 system/domain node PRESENT
     */
    public void setPath(String path) {
        /*
        ArrayList<String> newPath = new ArrayList<String>();
        if (path != null) {
            StringTokenizer tok = new StringTokenizer(path, delim);
            if (tok.hasMoreTokens()) {
                String first = tok.nextToken();
                if (!first.equals(getRoot())) newPath.add(first);

                while (tok.hasMoreTokens()) newPath.add(tok.nextToken());
            }
        }
        mPath = (newPath.toArray(mPath));
        */
        mPath = path.split(delim);
        mStringPath = null;
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
            StringBuffer stringPathBuffer = new StringBuffer("/").append(getRoot());

            for (String element : mPath) stringPathBuffer.append(delim).append(element);

            mStringPath = stringPathBuffer.toString();
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

    /*
    public Type getType() {
        return mType;
    }
    */

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

        return "Path - dump(): " + comp.toString() + "}\n        string=" + toString() + "\n        uuid=" + getUUID()
                + "\n        type=" + mType;
    }
}
