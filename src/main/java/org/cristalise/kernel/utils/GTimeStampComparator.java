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

import java.util.Comparator;

import org.cristalise.kernel.common.GTimeStamp;

public class GTimeStampComparator implements Comparator<GTimeStamp> {

    @Override
    public int compare(GTimeStamp t0, GTimeStamp t1) {

        int retVal = compareInt(t0.mYear, t1.mYear);
        if (retVal == 0) retVal = compareInt(t0.mMonth, t1.mMonth);
        if (retVal == 0) retVal = compareInt(t0.mDay, t1.mDay);
        if (retVal == 0) retVal = compareInt(t0.mHour - (t0.mTimeOffset / 3600), t1.mHour - (t1.mTimeOffset / 3600));
        if (retVal == 0) retVal = compareInt(t0.mMinute, t1.mMinute);
        if (retVal == 0) retVal = compareInt(t0.mSecond, t1.mSecond);

        return retVal;
    }

    private static int compareInt(int i, int j) {
        return i - j;
    }
}
