package org.cristalise.kernel.utils;

import org.apache.commons.lang3.reflect.FieldUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CorbaExceptionUtility {

    /**
     * The exception can have a "details" field if it was defined in the CommonException.idl, 
     * use that instead of getMessage()
     * 
     * @param ex the exception to process
     * @return the message to return
     */
    public static String unpackMessage(Throwable ex) {
        try {
            return (String)FieldUtils.readField(ex, "details");
        }
        catch (IllegalArgumentException | IllegalAccessException e) {}

        return ex.getMessage();
    }
}
