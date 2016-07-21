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

package org.cristalise.kernel.property;

import org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription;


/**
 * Helper enumeration to make built-in Property names easier to maintain and document
 */
public enum BuiltInItemProperties {
    /**
     * The name or ID of the Item, specified by the Factory usually and sent as the first parameter in the
     * {@link CreateItemFromDescription} predefined Step. It will be automatically added even if it was not defined.
     */
    NAME("Name"),
    
    /**
     * The type of the Item, used in Dev module, but it is a good practice to set it as an immutable, fixed value 
     * Property. It is convenient to use in lookup searches for Description Items.
     */
    TYPE("Type"),
    
    /**
     * Store the Agent name used used to call the {@link CreateItemFromDescription} predefined Step.
     * 
     */
    CREATOR("Creator");

    private String propName;

    private BuiltInItemProperties(final String n) {
        propName = n;
    }

    public String getName() {
        return propName;
    }

    public String toString() {
        return getName();
    }

    public static BuiltInItemProperties getValue(String propName) {
        for (BuiltInItemProperties prop : BuiltInItemProperties.values()) {
            if(prop.getName().equals(propName) || prop.name().equals(propName)) return prop;
        }
        return null;
    }
}
