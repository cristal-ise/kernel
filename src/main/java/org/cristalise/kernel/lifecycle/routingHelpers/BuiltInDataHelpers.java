package org.cristalise.kernel.lifecycle.routingHelpers;

public enum BuiltInDataHelpers {
    PROPERTY_DH("property"),
    VIEWPOINT_DH("viewpoint"),
    ACTIVITY_DH("activity");

    private String dhName;

    private BuiltInDataHelpers(final String n) {
        dhName = n;
    }

    public String getName() {
        return dhName;
    }

    public String toString() {
        return getName();
    }

    public static BuiltInDataHelpers getValue(String name) {
        for (BuiltInDataHelpers prop : BuiltInDataHelpers.values()) {
            if(prop.getName().equals(name) || prop.name().equals(name)) return prop;
        }
        return null;
    }

}
