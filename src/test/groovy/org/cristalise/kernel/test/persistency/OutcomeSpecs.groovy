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
package org.cristalise.kernel.test.persistency

import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.process.MainTest
import org.cristalise.kernel.utils.FileStringUtility
import org.cristalise.kernel.utils.Logger
import org.w3c.dom.Document

import spock.lang.Specification


/**
 *
 */
class OutcomeSpecs extends Specification {

    def setup() {
        Logger.addLogStream(System.out, 1);
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
    }

    def 'Outcome C2KLocalObject can be constructed from path - /Script/0/7'() {
        when:
        Outcome o = new Outcome("/Script/0/7", (Document)null)

        then:
        o.getClusterType() == "Outcome"
        o.getName() == "7";
        o.getID() == 7;
        o.getSchemaType() == "Script";
        o.getSchemaVersion() == 0;
    }

    def 'Outcome C2KLocalObject can be constructed from path - /Outcome/Script/0/7'() {
        when:
        Outcome o = new Outcome("/Outcome/Script/0/7", (Document)null)

        then:
        o.getClusterType() == "Outcome"
        o.getName() == "7";
        o.getID() == 7;
        o.getSchemaType() == "Script";
        o.getSchemaVersion() == 0;
    }
    
    def 'Outcome can remove a Node using XPath'() {
        given:
        String ocData = FileStringUtility.url2String(OutcomeTest.class.getResource("/outcomeTest.xml"));
        Outcome o = new Outcome("/Outcome/Script/0/7", ocData)
        assert o.getField("Field1") == "Field1contents"

        when:
        o.removeNodeByXPath("/TestOutcome/Field1")

        then:
        o.getField("Field1") == null
    }
}
