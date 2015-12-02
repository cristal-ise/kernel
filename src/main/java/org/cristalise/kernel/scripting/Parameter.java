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
package org.cristalise.kernel.scripting;


/**************************************************************************
* Place holder for the Parameter details to be passed to the script.
**************************************************************************/
public class Parameter {

    private String name;
    private Class<?> type;
    private boolean initialised=false;

    public Parameter(String name) {
        this.name = name;
    }
    
    public Parameter(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public void setName(String n)
    {
        name=n;
    }

    public String getName()
    {
        return name;
    }

    public void setType(Class<?> t)
    {
        type=t;
    }

    public Class<?> getType()
    {
        return type;
    }

    public void setInitialised(boolean state)
    {
        initialised=state;
    }

    public boolean getInitialised()
    {
        return initialised;
    }

}
