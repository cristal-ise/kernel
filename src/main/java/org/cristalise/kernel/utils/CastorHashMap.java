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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


// This subclass of hashtable can be marshalled
// and unmarshalled with Castor
public class CastorHashMap extends HashMap<String,Object>
{
    public CastorHashMap()
    {
        clear();
    }
    
    ArrayList<String> abstractPropNames = new ArrayList<String>();

    public KeyValuePair[] getKeyValuePairs()
    {
        int            numKeys       = size();

        KeyValuePair[] keyValuePairs = new KeyValuePair[numKeys];
        Iterator<String> keyIter     = keySet().iterator();
        int            i             = 0;

        for(i=0; i<numKeys; i++)
            if (keyIter.hasNext())
            {
                String name = keyIter.next();
                keyValuePairs[i]   = new KeyValuePair(name ,get(name), abstractPropNames.contains(name));
            }

        return keyValuePairs;
    }


    public void setKeyValuePairs(KeyValuePair[] keyValuePairs)
    {
        int i = 0;

        // Clears this hashtable so that it contains no keys
        clear();

        // Put each key value pair into this hashtable
        for(i=0; i<keyValuePairs.length; i++)
        {
            setKeyValuePair(keyValuePairs[i]);
        }
    }

    @Override
	public void clear() {
		super.clear();
		abstractPropNames = new ArrayList<String>();
	}


	public void setKeyValuePair(KeyValuePair keyValuePair)
    {
	    put(keyValuePair.getKey(), keyValuePair.getValue());
	    if (keyValuePair.isAbstract())
	    	abstractPropNames.add(keyValuePair.getKey());
	    else
	    	abstractPropNames.remove(keyValuePair.getKey());
    }

	public ArrayList<String> getAbstract() {
		return abstractPropNames;
	}


	public void put(String key, Object value, boolean isAbstract) {
		super.put(key, value);
		if (isAbstract) 
			abstractPropNames.add(key);
		else 
			abstractPropNames.remove(key);
		
	}
}
