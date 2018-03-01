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
package org.cristalise.kernel.lifecycle.renderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.ArrayList;

import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.renderer.DefaultVertexRenderer;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.utils.DateUtility;

/**
 * Renders an elementary Activity instance
 */
public class ActivityRenderer extends DefaultVertexRenderer {

    public ActivityRenderer() {
        super();
    }

    private Paint mActivePaint       = new Color(100, 255, 100);
    private Paint mActiveCompPaint   = new Color(100, 255, 255);
    private Paint mInactivePaint     = new Color(255, 255, 255);
    private Paint mInactiveCompPaint = new Color(200, 200, 255);
    private Paint mErrorPaint        = new Color(255, 50, 0);
    private Paint mTextPaint         = Color.black;

    /**
     * Draws the Activity as a 3D rectangle without borders, with text lines for Name, DefinitionName, State, WaitTime and errors
     */
    @Override
    public void draw(Graphics2D g2d, Vertex vertex) {
        Activity activity = (Activity) vertex;
        boolean hasError = !activity.verify();

        drawOutline3DRect(g2d, vertex, getActColor(activity, hasError));

        // String description = activity.getDescription();
        ArrayList<String> linesOfText = new ArrayList<String>();
        String type = activity.getTypeName();

        if (type != null) linesOfText.add("(" + type + ")");

        linesOfText.add(activity.getName());

        if (hasError) {
            linesOfText.add(activity.getErrors());
        }
        else {
            String stateName = "Invalid State";
            try {
                stateName = activity.getStateName();
            }
            catch (InvalidDataException | NullPointerException ex) {}

            linesOfText.add(stateName + (" " + getWaitTime(activity.getStateDate())));
        }

        drawLinesOfTexts(g2d, vertex, linesOfText, mTextPaint);
    }

    private Paint getActColor(Activity activity, boolean hasError) {
        boolean active = activity.getActive();
        boolean isComposite = activity.getIsComposite();

        if (hasError)
            return mErrorPaint;
        else if (active)
            if (isComposite) return mActiveCompPaint;
            else             return mActivePaint;
        else if (isComposite)
            return mInactiveCompPaint;
        else
            return mInactivePaint;
    }

    private static String getWaitTime(GTimeStamp date) {
        GTimeStamp now = new GTimeStamp();

        DateUtility.setToNow(now);

        long diff = DateUtility.diff(now, date);
        long secondes = diff % 60;
        long minutes  = (diff / 60) % 60;
        long hours    = (diff / 3600) % 24;
        long days     = (diff / 3600 / 24);

        if (days > 0)    return days    + " " + "d" + " " + hours   + " " + "h";
        if (hours > 0)   return hours   + " " + "h" + " " + minutes + " " + "min";
        if (minutes > 0) return minutes + " " + "min";

        return secondes + " " + "sec";
    }
}
