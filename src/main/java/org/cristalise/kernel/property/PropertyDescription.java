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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 
 *
 */
@Accessors(prefix = "m") @Data @AllArgsConstructor @NoArgsConstructor
public class PropertyDescription {
    /**
     * The name of the property
     */
    private String  mName = null;

    /**
     * The default value of the property. Use this to add value to an immutable property.
     */
    private String  mDefaultValue = null;

    /**
     * Class identifier are used in Dependency collection to check if the Item can be added or not. 
     * It is a common practice to use immutable Type property for this purpose.
     * ClassIdentifiers are transitive by default.
     */
    private boolean mIsClassIdentifier = false;

    /**
     * When true Property cannot change its value after instantiation
     */
    private boolean mIsMutable = false;

    /**
     * Transitive Properties are converted to VertexProperties (e.g. Dependency collection). 
     * ClassIdentifiers are Transitive too.l
     */
    private boolean mTransitive = false;

    /**
     * Transitive Properties are converted to VertexProperties. ClassIdentifiers are Transitive as well
     * 
     * @return if the PropertyDesc is transitive or not
     */
    public boolean isTransitive() {
        return mTransitive || mIsClassIdentifier;
    }

    //Method only kept for backward compatibility, because lombok generates different signature
    public void setIsClassIdentifier(boolean flag) {
        mIsClassIdentifier = flag;
    }

    //Method only kept for backward compatibility, because lombok generates different signature
    public void setIsMutable(boolean mutable) {
        mIsMutable = mutable;
    }

    //Method only kept for backward compatibility, because lombok generates different signature
    public boolean getIsClassIdentifier() {
        return mIsClassIdentifier;
    }

    //Method only kept for backward compatibility, because lombok generates different signature
    public boolean getIsMutable() {
        return mIsMutable;
    }

    /**
     * Instantiates a new Property from this definition
     * 
     * @return the newly created Property
     */
    public Property getProperty() {
        return new Property(mName, mDefaultValue, mIsMutable);
    }
}
