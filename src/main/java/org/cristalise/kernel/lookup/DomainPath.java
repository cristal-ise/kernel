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

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

/**
 * Very simple extension to Path. Only copies constructors and defines root
 */
public class DomainPath extends Path {

    private ItemPath target = null;

    public DomainPath() {
        super();
    }

    public DomainPath(String[] path) {
        super(path);
    }

    public DomainPath(String path) {
        super(path);
    }

    public DomainPath(String path, ItemPath entity) {
        super(path);
        setItemPath(entity);
    }

    public DomainPath(DomainPath parent, String child) {
        super(parent, child);
    }

    /*
     * the root of domain paths is /domain clearly
     */
    @Override
    public String getRoot() {
        return "domain";
    }

    public DomainPath getParent() {
        if (mPath.length == 0) return null;

        String[] parentPath = new String[mPath.length - 1];
        System.arraycopy(mPath, 0, parentPath, 0, parentPath.length);
        return new DomainPath(parentPath);
    }

    public void setItemPath(ItemPath newTarget) {
        target = newTarget;
    }

    public ItemPath getTarget() {
        return target;
    }

    @Override
    public ItemPath getItemPath() throws ObjectNotFoundException {
        if (target == null) {
            try {
                setItemPath( Gateway.getLookup().resolvePath(this) );
                if (target == null) throw new ObjectNotFoundException("Path " + toString() + " does not resolve to an Item");
            }
            catch (InvalidItemPathException e) {
                throw new ObjectNotFoundException(e.getMessage());
            } 
        }
        return target;
    }

    public String getTargetUUID() {
        if (target != null) return target.getUUID().toString();
        return null;
    }

    public void setTargetUUID(String uuid) {
        try {
            target = new ItemPath(uuid);
        }
        catch (InvalidItemPathException e) {
            Logger.error(e);
        }
    }

    /**
     * Checks if the DomainPath represents a context node (i.e. its target ItemPath is null).
     * Use this method when target was set already.
     * 
     * @return true if the DomainPath represents a context node
     */
    public boolean isContext() {
        return target == null;
    }

    /**
     * Retrieves the domain name of the path
     * 
     * @return the last path component;
     */
    @Override
    public String getName() {
        if (mPath.length > 0) return mPath[mPath.length - 1];
        else                  return getRoot();
    }

    @Override
    public String getClusterPath() {
        return ClusterType.PATH + "/Domain/" + StringUtils.join(mPath, "");
    }
}
