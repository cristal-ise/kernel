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

import java.util.ArrayList;

/**************************************************************************
 *
 * $Revision: 1.2 $
 * $Date: 2003/06/06 11:37:45 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/
public class ErrorInfo {
    ArrayList<String> msg;
    boolean fatal = false;

    public ErrorInfo() {
        super();
        msg = new ArrayList<String>();
    }
    
    public ErrorInfo(String error) {
    	this();
    	msg.add(error);
    }

    public void addError(String error) {
        msg.add(error);
    }

    @Override
	public String toString() {
        StringBuffer err = new StringBuffer();
        for (String element : msg) {
            err.append(element+"\n");
        }
        return err.toString();
    }
    
    public void setErrors(ArrayList<String> msg) {
    	this.msg = msg;
    }
    
    public ArrayList<String> getErrors() {
    	return msg;
    }

    public void setFatal() {
        fatal=true;
    }

    public boolean getFatal() {
        return fatal;
    }
}
