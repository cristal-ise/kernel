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

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.process.Gateway;



/**
* @version $Revision: 1.22 $ $Date: 2005/10/13 08:15:00 $
* @author  $Author: abranson $
**/
public class DomainPath extends Path
{
    private ItemPath target = null;
    protected static String mTypeRoot;

/* Very simple extension to Path. Only copies constructors and defines root */

    public DomainPath()
    {
        super(Path.UNKNOWN);
    }

    public DomainPath(short type)
    {
        super();
        mType = type;
    }

    public DomainPath(String[] path)
    {
        super(path, Path.UNKNOWN);
    }

    public DomainPath(String path)
    {
        super(path, Path.UNKNOWN);
    }

    public DomainPath(String path, ItemPath entity)
    {
        super(path, Path.UNKNOWN);
        setItemPath(entity);
    }

    public DomainPath(DomainPath parent, String child) {
        super(parent, child);
    }

    /* the root of domain paths is /domain
     * clearly
    */
    @Override
	public String getRoot() {
        return "domain";
    }

    public DomainPath getParent() {
        if (mPath.length == 0)
            return null;

        String[] parentPath = new String[mPath.length-1];
        System.arraycopy(mPath, 0, parentPath, 0, parentPath.length);
        return new DomainPath(parentPath);
    }

    public void setItemPath(ItemPath newTarget) {
        if (newTarget == null) { // clear
            target = null;
            mType = Path.CONTEXT;
            return;
        }

        target = newTarget;
        mType = Path.ITEM;
    }

    @Override
	public ItemPath getItemPath() throws ObjectNotFoundException {
        if (mType == UNKNOWN) { // must decide
            checkType();
        }

        if (target == null)
            throw new ObjectNotFoundException("Path "+toString()+" does not resolve to an Item");
        return target;
    }

    @Override
	public short getType() {
        if (mType == UNKNOWN) { // must decide
            checkType();
        }
        return mType;
    }

    protected void checkType() {
        try {
		    setItemPath(Gateway.getLookup().resolvePath(this));
        } catch (InvalidItemPathException ex) {
            mType = CONTEXT;
        } catch (ObjectNotFoundException ex) {
            mType = CONTEXT;
        }

    }

    /**
     * Retrieves the domkey of the path
     * @return the last path component;
     */
    public String getName() {
    	return mPath[mPath.length-1];
    }
}

