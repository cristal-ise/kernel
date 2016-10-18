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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.exolab.castor.mapping.AbstractFieldHandler;

/**
 * 
 */
public class CDataFieldHandler extends AbstractFieldHandler<String> {

    private Method getBeanMethod(@SuppressWarnings("rawtypes") Class clazz, String propertyName, boolean setter) 
            throws IntrospectionException
    {
        for (PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
            if (pd.getName().equals(propertyName)) {
                if (setter) return pd.getWriteMethod();
                else        return pd.getReadMethod();
            }
        }
        throw new IntrospectionException("Property missing:"+propertyName);
    }

    @Override
    public String getValue(Object object) throws IllegalStateException {
        try {
            Method getter = getBeanMethod(object.getClass(), getFieldDescriptor().getFieldName(), false);
            Object value = getter.invoke(object);

            if (value == null) return "";
            else               return "<![CDATA[" + value.toString() + "]]>";
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | IntrospectionException e) {
            Logger.error(e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setValue(Object object, String value) throws IllegalStateException, IllegalArgumentException {
        try {
            Method setter = getBeanMethod(object.getClass(), getFieldDescriptor().getFieldName(), true);
            setter.invoke(object, value);
        }
        catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
            Logger.error(e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void resetValue(Object object) throws IllegalStateException, IllegalArgumentException {}

    @Override
    public String newInstance(Object parent) throws IllegalStateException {return null;}

    @Override
    public Object newInstance(Object parent, Object[] args) throws IllegalStateException {return null;}
}