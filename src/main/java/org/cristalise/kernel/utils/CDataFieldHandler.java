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