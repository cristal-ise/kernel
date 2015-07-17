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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;



/**
* Extends Path to enforce SystemKey structure and support int form
*
* @version $Revision: 1.14 $ $Date: 2006/03/03 13:52:21 $
* @author  $Author: abranson $
**/
public class ItemPath extends Path
{
    
    public ItemPath() {
    	setSysKey(UUID.randomUUID());
    }

    protected ItemPath(UUID uuid) {
       	setSysKey(uuid);
    }

    public ItemPath(SystemKey syskey) {
        setSysKey(syskey);
    }

    /*
    */
    public ItemPath(String[] path) throws InvalidItemPathException
    {
        super(path, Path.CONTEXT);
        getSysKeyFromPath();
    }

    /*
    */
    public ItemPath(String path) throws InvalidItemPathException
    {
        super(path, Path.CONTEXT);
        if (path == null) throw new InvalidItemPathException("Path cannot be null");
        getSysKeyFromPath();
    }

    private void getSysKeyFromPath() throws InvalidItemPathException {
        if (mPath.length == 1) {
        	try {
        		setSysKey(UUID.fromString(mPath[0]));
        		mType = Path.ITEM;
        	} catch (IllegalArgumentException ex) {
        		throw new InvalidItemPathException(mPath[0]+" is not a valid UUID");
        	}
        }
        else
        	throw new InvalidItemPathException("Not a valid item path: "+Arrays.toString(mPath));
    }
    
    // EntityPaths root in /entity
    @Override
	public String getRoot() {
        return "item";
    }

    @Override
	public ItemPath getItemPath() throws ObjectNotFoundException {
        return this;
    }

    public byte[] getOID() {
        if (mType == Path.CONTEXT) return null;
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(mSysKey.msb);
        bb.putLong(mSysKey.lsb);
        return bb.array();
    }
    
    protected void setSysKey(UUID uuid) {
		mUUID = uuid;
		mSysKey = new SystemKey(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
		setPathFromUUID(mUUID.toString());
    }
    
    protected void setSysKey(SystemKey sysKey) {
		mSysKey = sysKey;
		mUUID = new UUID(sysKey.msb, sysKey.lsb);
		setPathFromUUID(mUUID.toString());
    }
    
    private void setPathFromUUID(String uuid) {
    	mPath = new String[1];
    	mPath[0] = uuid;
    	mType = Path.ITEM;
    }
}

