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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

/**
 * Extends Path to enforce SystemKey structure and support UUID form
 */
public class ItemPath extends Path {

    protected String mIOR;

    public ItemPath() {
        setSysKey(UUID.randomUUID());
    }

    public ItemPath(UUID uuid, String ior) {
        setSysKey(uuid);
        setIORString(ior);
    }

    public ItemPath(UUID uuid) {
        setSysKey(uuid);
    }

    public ItemPath(SystemKey syskey) {
        setSysKey(syskey);
    }

    public ItemPath(String[] path) throws InvalidItemPathException {
        super(path);
        checkSysKeyFromPath();
    }

    public ItemPath(String path) throws InvalidItemPathException {
        super(path);

        if (path == null) throw new InvalidItemPathException("Path cannot be null");

        checkSysKeyFromPath();
    }

    @Override
    public void setPath(String[] path) {
        super.setPath(path);
    }

    @Override
    public void setPath(String path) {
        super.setPath(path);
    }

    @Override
    public void setPath(Path path) {
        super.setPath(path);
   }

    private void checkSysKeyFromPath() throws InvalidItemPathException {
        if (mPath.length == 1) {
            try {
                getUUID();
            }
            catch (Throwable ex) {
                throw new InvalidItemPathException(mPath[0] + " is not a valid UUID : " + ex.getMessage());
            }
        }
        else
            throw new InvalidItemPathException("Not a valid item path: " + Arrays.toString(mPath));
    }

    /**
     * The root of ItemPath is /entity
     */
    @Override
    public String getRoot() {
        return "entity";
    }

    @Override
    public ItemPath getItemPath() throws ObjectNotFoundException {
        return this;
    }

    @Override
    public org.omg.CORBA.Object getIOR() {
        if (mIOR == null) {
            try {
                mIOR = Gateway.getLookup().getIOR(this);
            }
            catch (ObjectNotFoundException ex) {
                Logger.warning(ex.getMessage());
                return null;
            }
        }
        return Gateway.getORB().string_to_object(mIOR);
    }

    @Override
    public void setIOR(org.omg.CORBA.Object IOR) {
        mIOR = Gateway.getORB().object_to_string(IOR);
    }

    public String getIORString() {
        return mIOR;
    }

    public void setIORString(String ior) {
        mIOR = ior;
    }

    public byte[] getOID() {
        UUID uuid = getUUID();

        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        return bb.array();
    }

    protected void setSysKey(UUID uuid) {
        setPathFromUUID(uuid);
    }

    protected void setSysKey(SystemKey sysKey) {
        setPathFromUUID(new UUID(sysKey.msb, sysKey.lsb));
    }

    private void setPathFromUUID(UUID uuid) {
        mPath = new String[1];
        mPath[0] = uuid.toString();
    }

    @Override
    public SystemKey getSystemKey() {
        UUID uuid = getUUID();
        return new SystemKey(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    @Override
    public UUID getUUID() {
        return UUID.fromString(mPath[0]);
    }

    @Override
    public String getName() {
        return getUUID().toString();
    }

    @Override
    public String getClusterPath() {
        return ClusterType.PATH + "/Item";
    }
    
    public static boolean isUUID(String entityKey) {
        if (entityKey.startsWith("/entity/")) entityKey = entityKey.substring(8);

        return entityKey.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
}
