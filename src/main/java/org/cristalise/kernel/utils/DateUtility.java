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
package org.cristalise.kernel.utils;

import java.util.Calendar;

import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.common.InvalidDataException;


public class DateUtility {

    public static GTimeStamp setToNow(GTimeStamp date) {
        GTimeStamp now = getNow();

        date.mYear       = now.mYear;
        date.mMonth      = now.mMonth;
        date.mDay        = now.mDay;
        date.mHour       = now.mHour;
        date.mMinute     = now.mMinute;
        date.mSecond     = now.mSecond;
        date.mTimeOffset = now.mTimeOffset;

        return date;
    }

    static public GTimeStamp getNow() {
        java.util.Calendar now = Calendar.getInstance();

        return new GTimeStamp( now.get(Calendar.YEAR),
                               now.get(Calendar.MONTH)+1,
                               now.get(Calendar.DAY_OF_MONTH),
                               now.get(Calendar.HOUR_OF_DAY),
                               now.get(Calendar.MINUTE),
                               now.get(Calendar.SECOND),
                               now.get(Calendar.ZONE_OFFSET) );
    }

    public static String getSQLFormat(GTimeStamp timeStamp) {
        StringBuffer time = new StringBuffer().append(timeStamp.mYear).append("-");
        if (timeStamp.mMonth < 10) time.append("0");
        time.append(timeStamp.mMonth).append("-");

        if (timeStamp.mDay < 10) time.append("0");
        time.append(timeStamp.mDay).append(" ");

        if (timeStamp.mHour < 10) time.append("0");
        time.append(timeStamp.mHour).append(":");

        if (timeStamp.mMinute < 10) time.append("0");
        time.append(timeStamp.mMinute).append(":");

        if (timeStamp.mSecond < 10) time.append("0");
        time.append(timeStamp.mSecond);

        return time.toString();
    }

    public static int getNbDayInYear(GTimeStamp date) {
        int centuary = date.mYear / 100;
        int cdivby4 = centuary / 4;
        int ydivby4 = date.mYear / 4;
        if (centuary * 100 - date.mYear == 0) {
            if (centuary == cdivby4 * 4) return 366;
            else return 365;
        }
        else if (date.mYear == ydivby4 * 4) return 366;
        else return 365;
    }

    public static int getNbDayInMonth(GTimeStamp date) {
        switch (date.mMonth) {
            case 2: if (getNbDayInYear(date) == 365) return 28;
                    else                             return 29;
            case 4:  return 30;
            case 6:  return 30;
            case 9:  return 30;
            case 11: return 30;
            default: return 31;
        }
    }

    public static long diff(GTimeStamp date1, GTimeStamp date2) {
        GTimeStamp tmp = new GTimeStamp(date1.mYear, date1.mMonth, date1.mDay, 
                                        date1.mHour, date1.mMinute, date1.mSecond, date1.mTimeOffset);

        while (tmp.mYear - date2.mYear < 0) {
            while (tmp.mMonth < 13) {
                tmp.mDay = tmp.mDay - getNbDayInMonth(tmp);
                tmp.mMonth++;
            }
            tmp.mMonth = 1;
            tmp.mYear++;
        }

        while (tmp.mYear - date2.mYear > 0) {
            while (tmp.mMonth > 1) {
                tmp.mMonth--;
                tmp.mDay = tmp.mDay + getNbDayInMonth(tmp);
            }
            tmp.mMonth = 12;
            tmp.mDay = tmp.mDay + getNbDayInMonth(tmp);
            tmp.mYear--;
        }

        while (tmp.mMonth - date2.mMonth < 0) {
            tmp.mDay = tmp.mDay - getNbDayInMonth(tmp);
            tmp.mMonth++;
        }

        while (tmp.mMonth - date2.mMonth > 0) {
            tmp.mMonth--;
            tmp.mDay = tmp.mDay + getNbDayInMonth(tmp);
        }
        return (((tmp.mDay - date2.mDay) * 24 + tmp.mHour - date2.mHour) * 60 + tmp.mMinute - date2.mMinute) * 60 + tmp.mSecond - date2.mSecond;
    }

    public static GTimeStamp parseTimeString(String time) throws InvalidDataException {
        if (time.length() == 19) {
            return new GTimeStamp(
                    Integer.parseInt(time.substring(0,4)),
                    Integer.parseInt(time.substring(5,7)),
                    Integer.parseInt(time.substring(8,10)),
                    Integer.parseInt(time.substring(11,13)),
                    Integer.parseInt(time.substring(14,16)),
                    Integer.parseInt(time.substring(17,19)),
                    Calendar.getInstance().get(Calendar.ZONE_OFFSET));
        }
        else if (time.length() == 14) {
            // support for some sql formats
            return new GTimeStamp(
                    Integer.parseInt(time.substring(0,4)),
                    Integer.parseInt(time.substring(4,6)),
                    Integer.parseInt(time.substring(6,8)),
                    Integer.parseInt(time.substring(8,10)),
                    Integer.parseInt(time.substring(10,12)),
                    Integer.parseInt(time.substring(12,14)),
                    Calendar.getInstance().get(Calendar.ZONE_OFFSET));
        }
        else
            throw new InvalidDataException("Unknown time format: "+time);
    }

    public static String timeToString(GTimeStamp timeStamp) {
        StringBuffer time = new StringBuffer().append(timeStamp.mYear).append("-");

        if (timeStamp.mMonth<10) time.append("0");
        time.append(timeStamp.mMonth).append("-");

        if (timeStamp.mDay<10) time.append("0");
        time.append(timeStamp.mDay).append(" ");

        if (timeStamp.mHour<10) time.append("0");
        time.append(timeStamp.mHour).append(":");

        if (timeStamp.mMinute<10) time.append("0");
        time.append(timeStamp.mMinute).append(":");

        if (timeStamp.mSecond<10) time.append("0");
        time.append(timeStamp.mSecond);

        return time.toString();
    }
}
