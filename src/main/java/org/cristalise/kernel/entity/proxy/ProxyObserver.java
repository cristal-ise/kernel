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
package org.cristalise.kernel.entity.proxy;

import org.cristalise.kernel.entity.C2KLocalObject;



public interface ProxyObserver<V extends C2KLocalObject>
{
   /**************************************************************************
    * Subscribed items are broken apart and fed one by one to these methods.
    * Replacement after an event is done by feeding the new memberbase with the same id.
    * ID could be an XPath?
    **************************************************************************/
    public void add(V contents);

   /**************************************************************************
    * the 'type' parameter should be an indication of the type of object
    * supplied so that the subscriber can associate the call back with
    * one of its subscriptions. If we go with an Xpath subscription form,
    * then the id will probably be sufficient.
    * Should be comparable (substring whatever) with the parameter given to
    * the subscribe method of ItemProxy.
    **************************************************************************/
    public void remove(String id);
    
    public void control(String control, String msg);
}
