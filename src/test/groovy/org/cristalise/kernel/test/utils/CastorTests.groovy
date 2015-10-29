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
package org.cristalise.kernel.test.utils

import groovy.transform.CompileStatic

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.process.MainTest
import org.cristalise.kernel.utils.CastorXMLUtility
import org.cristalise.kernel.utils.FileStringUtility
import org.cristalise.kernel.utils.Logger
import org.junit.Before
import org.junit.Test


/**
 *
 */
@CompileStatic
class CastorTests {

    @Before
    public void setup() throws InvalidDataException {
        Logger.addLogStream(System.out, 1);
        Gateway.init( FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath()) );
    }

    @Test
    public void testMapFiles() throws Exception {
        //TODO: this test needs to be rewritten
        new CastorXMLUtility(Gateway.getResource(), Gateway.getProperties(), Gateway.getResource().getKernelResourceURL("mapFiles/"));
    }
}
