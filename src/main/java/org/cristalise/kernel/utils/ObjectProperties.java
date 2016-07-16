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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

public class ObjectProperties extends Properties {

    private static final long serialVersionUID = 8214748637885650335L;

    public ObjectProperties() {
    }

    public ObjectProperties(Properties defaults) {
        super(defaults);
    }

    public String getString(String propName) {
        return getString(propName, null);
    }

    public String getString(String propName, String defaultValue) {
        String value = super.getProperty(propName, defaultValue);
        if (value!=null) value = value.trim();
        return value;
    }

    /**
     * ogattaz proposal
     * 
     * @param propName
     *            the name of the property
     * @return the object value of the property. Returns null if the property
     *         doesn't exist or if the properties of the gateway is null
     */
    public Object getObject(String propName) {
        return getObject(propName, null);
    }

    /**
     * ogattaz proposal
     * 
     * @param aPropertyName
     *            the name of the property
     * @param defaultValue
     *            the default value.
     * @return the object value of the property. Returns the default value if the property
     *         doesn't exist or if the properties of the gateway is null.
     * @return
     */
    public Object getObject(String propName,
            Object defaultValue) {

        Object wValue = get(propName);
        if (wValue == null) {
            return defaultValue;
        }
        return wValue;
    }

    /**
     * ogattaz proposal
     * 
     * @param propName
     *            the name of the paroperty
     * @return the boolean value of the property. Returns false if the property
     *         doesn't exist or if the value is not a String or a Boolean
     *         instance
     */
    public boolean getBoolean(String aPropertyName) {
        return getBoolean(aPropertyName, Boolean.FALSE);
    }

    /**
     * ogattaz proposal
     * 
     * @param propName
     *            the name of the parameter stored in the clc file
     * @param defaultValue
     *            the default value
     * @return the boolean value of the property. Returns the default value if
     *         the property doesn't exist or if the value is not a String or a
     *         Boolean instance
     */
    public boolean getBoolean(String aPropertyName,
            boolean defaultValue) {

        Object wValue = getObject(aPropertyName, Boolean.valueOf(defaultValue));
        if (wValue instanceof Boolean) {
            return ((Boolean) wValue).booleanValue();
        }
        if (wValue instanceof String) {
            return Boolean.parseBoolean((String) wValue);
        }
        Logger.error("getBoolean(): unable to retrieve a int value for ["+aPropertyName+"]. Returning default value ["+defaultValue+"]. object found="+wValue);

        return defaultValue;
    }

    /**
     * ogattaz proposal
     * 
     * @param propName
     *            the name of the property
     * @return the int value of the property. Returns -1 if the property doesn't
     *         exist or if the value is not a String or an Integer instance
     */
    public int getInt(String aPropertyName) {
        return getInt(aPropertyName, -1);
    }

    /**
     * ogattaz proposal
     * 
     * @param propName
     *            the name of the property
     * @param defaultValue
     *            the default value
     * @return the int value of the property. Returns the default vakue if the
     *         property doesn't exist or if the value is not a String or an
     *         Integer instance
     */
    public int getInt(String aPropertyName, int defaultValue) {

        Object wValue = getObject(aPropertyName, Integer.valueOf(defaultValue));
        if (wValue instanceof Integer) {
            return ((Integer) wValue).intValue();
        }
        if (wValue instanceof String) {
            try {
                return Integer.parseInt((String) wValue);
            } catch (NumberFormatException ex) { }
        }
        Logger.error("getInt(): unable to retrieve a int value for ["+aPropertyName+"]. Returning default value ["+defaultValue+"]. object found="+wValue);
        return defaultValue;
    }

    /**
     * Allow setting of properties as Objects
     * 
     * @param aPropertyName
     *            the name of the property
     * @param aPropertyValue
     */
    public void setProperty(String aPropertyName, Object aPropertyValue) {
        put(aPropertyName, aPropertyValue);
    }

    public void dumpProps(int logLevel) {
        Logger.msg(logLevel, "Properties:");
        for (Enumeration<?> e = propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            Object value = getObject(name);
            if (value == null)
                Logger.msg("    "+name+": null");
            else
                Logger.msg("    "+name+" ("+getObject(name).getClass().getSimpleName()+"): '"+getObject(name).toString()+"'");
        }
    }

    public Object getInstance(String propName, Object defaultVal) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Object prop = getObject(propName, defaultVal);
        if (prop == null || prop.equals(""))
            throw new InstantiationException("Property '"+propName+"' was not defined. Cannot instantiate.");
        if (prop instanceof String)
            return Class.forName(((String)prop).trim()).newInstance();
        return prop;
    }

    public Object getInstance(String propName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return getInstance(propName, null);
    }		

    public ArrayList<?> getInstances(String propName, Object defaultVal) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Object val = getObject(propName, defaultVal);
        if (val == null) return null;
        if (val instanceof ArrayList)
            return (ArrayList<?>)val;
        else if (val instanceof String) {
            ArrayList<Object> retArr = new ArrayList<Object>();
            StringTokenizer tok = new StringTokenizer((String)val, ",");
            while (tok.hasMoreTokens()) 
                retArr.add(getInstance(tok.nextToken()));
            return retArr;
        }
        else {
            ArrayList<Object> retArr = new ArrayList<Object>();
            retArr.add(val);
            return retArr;
        }
    }

    public ArrayList<?> getInstances(String propName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return getInstances(propName, null);
    }
}
