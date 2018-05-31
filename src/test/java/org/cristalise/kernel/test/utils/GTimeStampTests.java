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
package org.cristalise.kernel.test.utils;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;

import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.kernel.utils.Logger;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 *
 */
public class GTimeStampTests {

    @BeforeClass
    public static void setup() throws Exception {
        Logger.addLogStream(System.out, 8);
    }
    
    @SuppressWarnings("deprecation")
    public void assertSqlTimestamp(int year, int month, int day, int hour, int minute, int second, Timestamp sqlTs) {
        assertEquals(year,   sqlTs.getYear()+1900);
        assertEquals(month,  sqlTs.getMonth()+1);
        assertEquals(day,    sqlTs.getDate());
        assertEquals(hour,   sqlTs.getHours());
        assertEquals(minute, sqlTs.getMinutes());
        assertEquals(second, sqlTs.getSeconds());
    }

    @Test
    public void testGTimeStamp_to_SqlTimestamp() throws Exception {
        GTimeStamp ts1 = new GTimeStamp(2001, 11, 18, 13, 11, 0, 0);

        Timestamp sqlTs1 = DateUtility.toSqlTimestamp(ts1);
        assertSqlTimestamp(2001, 11, 18, 13, 11, 0, sqlTs1); //hour is the same

        GTimeStamp ts1prime = DateUtility.fromSqlTimestamp( sqlTs1 );
        assertEquals(0, DateUtility.diff(ts1, ts1prime) );
    }

    @Test
    public void testGTimeStamp_to_SqlTimestamp_wihtOffset() throws Exception {
        GTimeStamp ts1 = new GTimeStamp(2001, 11, 18, 13, 11, 0, 3600000);

        Timestamp sqlTs1 = DateUtility.toSqlTimestamp(ts1);
        assertSqlTimestamp(2001, 11, 18, 12, 11, 0, sqlTs1); //one hour difference

        GTimeStamp ts1prime = DateUtility.fromSqlTimestamp( sqlTs1 );
        assertEquals(3600, DateUtility.diff(ts1, ts1prime) );
    }

    @Test
    public void testSqlTimeStamp_to_utcString() throws Exception {
        GTimeStamp ts1 = new GTimeStamp(2001, 11, 18, 13, 11, 0, 0);
        assertEquals("2001-11-18T13:11:00Z", DateUtility.timeStampToUtcString(ts1));
    }

    @Test
    public void testSqlTimeStamp_to_utcString_withOffset() throws Exception {
        GTimeStamp ts = new GTimeStamp(2001, 11, 18, 13, 11, 0, 3600000);
        assertEquals("2001-11-18T12:11:00Z", DateUtility.timeStampToUtcString(ts));
    }
}
