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
package org.cristalise.kernel.entity.transfer;

import java.io.File;
import java.util.ArrayList;

import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;

public class TransferSet {

    public ArrayList<TransferItem> items;

    public TransferSet() {}

    public TransferSet(ItemPath[] itemPaths) {
        items = new ArrayList<TransferItem>();
        for (ItemPath item : itemPaths) {
            try {
                items.add(new TransferItem(item));
            }
            catch (Exception ex) {
                Logger.error("Could not add item " + item);
                Logger.error(ex);
            }
        }
    }

    public void exportPackage(File dir) throws Exception {
        if (items == null || items.size() == 0)
            throw new Exception("Nothing to dump");
        FileStringUtility.createNewDir(dir.getAbsolutePath());
        for (TransferItem element : items) {
            try {
                element.exportItem(new File(dir, element.itemPath.getUUID().toString()), "/");
            }
            catch (Exception ex) {
                Logger.error("Error dumping item " + element.itemPath);
                Logger.error(ex);
            }
        }

        try {
            String self = Gateway.getMarshaller().marshall(this);
            FileStringUtility.string2File(new File(dir, "transferSet.xml"), self);
        }
        catch (Exception ex) {
            Logger.error("Error writing header file");
            Logger.error(ex);
        }
    }

    public void importPackage(File rootDir) {
        for (TransferItem element : items) {
            Logger.msg(5, "Importing " + element.itemPath);
            try {
                element.importItem(new File(rootDir, element.itemPath.getUUID().toString()));
            }
            catch (Exception ex) {
                Logger.error("Import of item " + element.itemPath + " failed. Rolling back");
                Logger.error(ex);
                Gateway.getStorage().abort(element);
            }
        }
    }
}
