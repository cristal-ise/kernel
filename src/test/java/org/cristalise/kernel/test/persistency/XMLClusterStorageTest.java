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
package org.cristalise.kernel.test.persistency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.XMLClusterStorage;
import org.junit.BeforeClass;
import org.junit.Test;

public class XMLClusterStorageTest {
    static ItemPath itemPath;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Logger.addLogStream(System.out, 8);

        itemPath = new ItemPath("fcecd4ad-40eb-421c-a648-edc1d74f339b");
    }

    public void checkXMLClusterStorage(XMLClusterStorage importCluster) throws Exception {
        ClusterType[] types = importCluster.getClusters(itemPath);

        assertEquals(6, types.length);

        for (ClusterType type : types) {
            String[] contents = importCluster.getClusterContents(itemPath, type);
            
            Logger.msg(Arrays.toString(contents));

            switch (type) {
                case PATH:
                    assertEquals(2,  contents.length);
                    assertArrayEquals(new String[]{"Domain", "Item"}, contents);
                    break;

                case PROPERTY:  assertEquals(19, contents.length); break;
                case LIFECYCLE: assertEquals(1,  contents.length); break;
                case OUTCOME:   assertEquals(14, contents.length); break;
                case VIEWPOINT: assertEquals(14, contents.length); break;
                case HISTORY:   assertEquals(30, contents.length); break;

                default:
                    fail("Unhandled ClusterType:"+type);
            }
        }
    }

    @Test
    public void checkFileBasedStorage() throws Exception {
        checkXMLClusterStorage(new XMLClusterStorage("src/test/data/xmlstorage/filebased", "", false));
    }

    @Test
    public void checkDirectoryBasedStorage() throws Exception {
        checkXMLClusterStorage(new XMLClusterStorage("src/test/data/xmlstorage/directorybased"));
    }
}
