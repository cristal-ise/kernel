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

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 
 *
 */
@Accessors(prefix = "m") @Data
public class PropertyDescription {
    /**
     * 
     */
    private String  mName = null;

    /**
     * 
     */
    private String  mDefaultValue = null;

    /**
     * 
     */
    private boolean mIsClassIdentifier = false;

    /**
     * 
     */
    private boolean mIsMutable = false;

    /**
     * Transitive Properties are converted to VertexProperties. ClassIdentifiers are Transitive as well
     */
    private boolean mTransitive = false;

    public PropertyDescription() {
    }

    public boolean isTransitive() {
        return mTransitive || mIsClassIdentifier;
    }

    public PropertyDescription(String name, String defaultValue, boolean isClassIdentifier, boolean isMutable) {
        setName(name);
        setDefaultValue(defaultValue);
        setIsClassIdentifier(isClassIdentifier);
        setIsMutable(isMutable);
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

    public Property getProperty() {
        return new Property(mName, mDefaultValue, mIsMutable);
    }
}
