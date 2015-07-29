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
package org.cristalise.kernel.lookup;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.UUID;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.process.Gateway;



/**
* 
**/
public abstract class Path
{
    public static final String delim = "/";

    // types
    public static final short UNKNOWN = 0;
    public static final short CONTEXT = 1;
    public static final short ITEM  = 2;

    protected String[] mPath = new String[0];

    // slash delimited path
    protected String   mStringPath = null;
    // entity or context
    protected short    mType      = CONTEXT;
    
    // item UUID (only valid for ItemPaths and DomainPaths that are aliases for Items)
    protected UUID			 mUUID;
    protected SystemKey		 mSysKey;

    // ior is stored in here when it is resolved
    protected org.omg.CORBA.Object   mIOR         = null;

    public Path() {
    }

    /*
    * Creates an empty path
    */
    public Path(short type)
    {
        mType = type;
    }

    /*
    * Creates a path with an arraylist of the path (big endian)
    */
    public Path(String[] path, short type) {
        setPath(path);
        mType = type;
    }

    /*
     * Creates a path from a slash separated string (big endian)
    */
    public Path(String path, short type) {
        setPath(path);
        mType = type;
    }

    /*
     *  Create a path by appending a child string to an existing path
     */
    public Path(Path parent, String child, short type) {
        String[] oldPath = parent.getPath();
        mPath = new String[oldPath.length+1];
        for (int i=0; i<oldPath.length; i++)
            mPath[i] = oldPath[i];
        mPath[oldPath.length] = child;
        mType = type;
    }

     /*
      * Create a path by appending a child and inheriting the type
      */
    public Path(Path parent, String child) {
        this(parent, child, UNKNOWN);
    }
    /*************************************************************************/

    // Setters

    /* string array path e.g. { "Product", "Crystal", "Barrel", "2L", "331013013348" }
     * system/domain node ABSENT
    */
    public void setPath(String[] path)
    {
        mStringPath = null;
        mPath = path.clone();
        mUUID = null;
        mSysKey = null;
    }

    /* string path e.g. /system/d000/d000/d001
     * system/domain node PRESENT
    */
    public void setPath(String path)
    {
        ArrayList<String> newPath = new ArrayList<String>();
        if (path != null) {
        	StringTokenizer tok = new StringTokenizer(path, delim);
	        if (tok.hasMoreTokens()) {
		        String first = tok.nextToken();
		        if (!first.equals(getRoot()))
		        	newPath.add(first);
		        while (tok.hasMoreTokens())
		            newPath.add(tok.nextToken());
	        }
        }

        mPath = (newPath.toArray(mPath));
        mStringPath = null;
        mUUID = null;
        mSysKey = null;
    }

    // lookup sets the IOR
    public void setIOR(org.omg.CORBA.Object IOR) {
        mIOR = IOR;
        if (IOR == null) mType = Path.CONTEXT;
        else mType = Path.ITEM;
    }

    /* clone another path object
    */
    public void setPath(Path path)
    {
        mStringPath = null;
        mPath = (path.getPath().clone());
        mUUID = null;
        mSysKey = null;
    }

    /*************************************************************************/


    /*
     * Getter Methods
     */

     // root is defined as 'domain', 'item' or 'role' in subclasses
    public abstract String getRoot();

    public String[] getPath()
    {
        return mPath;
    }

    public String getString()
    {
        if (mStringPath == null) {
                StringBuffer stringPathBuffer = new StringBuffer("/").append(getRoot());
                for (String element : mPath)
					stringPathBuffer.append(delim).append(element);
                mStringPath = stringPathBuffer.toString();
        }
        return mStringPath;
    }

    public boolean exists() {
        return Gateway.getLookup().exists(this);
    }

    /** Queries the lookup for the IOR
     */

    public org.omg.CORBA.Object getIOR() {
        org.omg.CORBA.Object newIOR = null;
        if (mIOR==null) { // if not cached try to resolve
            Lookup myLookup = Gateway.getLookup();
            try {
                String iorString = myLookup.getIOR(this);
                newIOR = Gateway.getORB().string_to_object(iorString);
            } catch (ObjectNotFoundException ex) {
            }
            setIOR(newIOR);
        }
        return mIOR;
    } 

    @Override
	public String toString() {
        return getString();
    }

    public short getType() {
        return mType;
    }

    public SystemKey getSystemKey() {
        return mSysKey;
    }
    
    public UUID getUUID() {
    	return mUUID;
    }

    public abstract ItemPath getItemPath() throws ObjectNotFoundException;

    @Override
	public boolean equals( Object path )
    {
    	if (path == null) return false;
        return toString().equals(path.toString());
    }

    @Override
	public int hashCode() {
        return toString().hashCode();
    }

    public String dump() {
        StringBuffer comp = new StringBuffer("Components: { ");
        for (String element : mPath)
			comp.append("'").append(element).append("' ");
        return "Path - dump(): "+comp.toString()+"}\n        string="+toString()+"\n        uuid="+getUUID()+"\n        type="+mType;
    }
}

