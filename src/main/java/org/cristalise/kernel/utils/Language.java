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

import java.util.Enumeration;
import java.util.Hashtable;

/**************************************************************************
 *
 * @author $Author: sgaspard $ $Date: 2004/09/21 13:17:40 $
 * @version $Revision: 1.9 $
 **************************************************************************/

public class Language
{
	public static Hashtable<?, ?> mTableOfTranslation = new Hashtable<Object, Object>();
    public static Hashtable<String, String> mTableOfUntranslated = new Hashtable<String, String>();
    public static boolean isTranlated=false;
	private Hashtable<?, ?> tableOfTranslation = new Hashtable<Object, Object>();

	public static String translate(String english)
	{
        if (!isTranlated) return english;
        String rep = english;
        if (rep != null && !rep.equals("")) {
            String translated = (String) mTableOfTranslation.get(english);
            if (translated != null) return translated;
            else
            {
                mTableOfUntranslated.put(english,"");
                try
                {
                    String s = "";
                    Enumeration<String> e = mTableOfUntranslated.keys();
                    while (e.hasMoreElements()) s =s+"\n"+e.nextElement();
                    FileStringUtility.string2File("untranslated.txt",s);
                }
                catch (Exception ex)
                {
                    Logger.warning("Could not write to preferences file. Preferences have not been updated.");
                }
            }

        }
		return rep;
	}

    public Language(String filePath)
    {
        String languageFile = filePath;
                if (languageFile == null || languageFile.length() == 0)
                // no language file defined for this process
                    return;
                tableOfTranslation = FileStringUtility.loadLanguageFile(languageFile);
    }
    public String insTranslate(String english)
    {
        String rep = english;
        if (rep != null && !rep.equals("")) {
            String translated = (String) tableOfTranslation.get(english);
            if (translated != null)
                rep = translated;
        }
        return rep;
    }


}
