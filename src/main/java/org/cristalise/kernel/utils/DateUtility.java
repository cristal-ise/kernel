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
package org.cristalise.kernel.utils;
import org.cristalise.kernel.common.GTimeStamp;
/**
 * @version $Revision: 1.8 $ $Date: 2005/05/10 15:14:55 $
 * @author $Author: abranson $
 */
public class DateUtility
{
	public static GTimeStamp setToNow(GTimeStamp date)
	{
		java.util.Calendar now = java.util.Calendar.getInstance();
		date.mYear = now.get(java.util.Calendar.YEAR);
		date.mMonth = now.get(java.util.Calendar.MONTH) + 1;
		date.mDay = now.get(java.util.Calendar.DAY_OF_MONTH);
		date.mHour = now.get(java.util.Calendar.HOUR_OF_DAY);
		date.mMinute = now.get(java.util.Calendar.MINUTE);
		date.mSecond = now.get(java.util.Calendar.SECOND);
		date.mTimeOffset = now.get(java.util.Calendar.ZONE_OFFSET);
		return date;
	}

	public static String getSQLFormat(GTimeStamp timeStamp)
	{
		StringBuffer time = new StringBuffer().append(timeStamp.mYear).append("-");
		if (timeStamp.mMonth < 10)
			time.append("0");
		time.append(timeStamp.mMonth).append("-");
		if (timeStamp.mDay < 10)
			time.append("0");
		time.append(timeStamp.mDay).append(" ");
		if (timeStamp.mHour < 10)
			time.append("0");
		time.append(timeStamp.mHour).append(":");
		if (timeStamp.mMinute < 10)
			time.append("0");
		time.append(timeStamp.mMinute).append(":");
		if (timeStamp.mSecond < 10)
			time.append("0");
		time.append(timeStamp.mSecond);
		return time.toString();
	}

    public static int getNbDayInYear(GTimeStamp date)
    {
        int centuary = date.mYear / 100;
        int cdivby4 = centuary / 4;
        int ydivby4 = date.mYear / 4;
        if (centuary * 100 - date.mYear == 0)
        {
            if (centuary == cdivby4 * 4)
                return 366;
            else
                return 365;
        }
        else if (date.mYear == ydivby4 * 4)
            return 366;
        else
            return 365;
    }
    public static int getNbDayInMonth(GTimeStamp date)
    {
        switch (date.mMonth)
        {
            case 2 :
                if (getNbDayInYear(date) == 365)
                    return 28;
                else
                    return 29;
            case 4 :
                return 30;
            case 6 :
                return 30;
            case 9 :
                return 30;
            case 11 :
                return 30;
            default :
                return 31;
        }
    }

    public static long diff(GTimeStamp date1, GTimeStamp date2)
    {
        GTimeStamp tmp = new GTimeStamp(date1.mYear, date1.mMonth, date1.mDay, date1.mHour, date1.mMinute, date1.mSecond, date1.mTimeOffset);
        while (tmp.mYear - date2.mYear < 0)
        {
            while (tmp.mMonth < 13)
            {
                tmp.mDay = tmp.mDay - getNbDayInMonth(tmp);
                tmp.mMonth++;
            }
            tmp.mMonth = 1;
            tmp.mYear++;
        }
        while (tmp.mYear - date2.mYear > 0)
        {
            while (tmp.mMonth > 1)
            {
                tmp.mMonth--;
                tmp.mDay = tmp.mDay + getNbDayInMonth(tmp);
            }
            tmp.mMonth = 12;
            tmp.mDay = tmp.mDay + getNbDayInMonth(tmp);
            tmp.mYear--;
        }
        while (tmp.mMonth - date2.mMonth < 0)
        {
            tmp.mDay = tmp.mDay - getNbDayInMonth(tmp);
            tmp.mMonth++;
        }
        while (tmp.mMonth - date2.mMonth > 0)
        {
            tmp.mMonth--;
            tmp.mDay = tmp.mDay + getNbDayInMonth(tmp);
        }
        return (((tmp.mDay - date2.mDay) * 24 + tmp.mHour - date2.mHour) * 60 + tmp.mMinute - date2.mMinute) * 60 + tmp.mSecond - date2.mSecond;
    }
}
