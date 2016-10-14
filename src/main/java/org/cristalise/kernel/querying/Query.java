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
package org.cristalise.kernel.querying;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.DescriptionObject;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Query implements DescriptionObject{

    private String      name;
    private Integer     version;
    private ItemPath    itemPath;
    private String      language;
    private String      query;

    @Override
    public String getItemID() {
        return itemPath.getUUID().toString();
    }

    @Override
    public CollectionArrayList makeDescCollections() throws InvalidDataException, ObjectNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void export(Writer imports, File dir) throws InvalidDataException, ObjectNotFoundException, IOException {
        // TODO Auto-generated method stub
        
    }
}
