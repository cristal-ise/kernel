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
package org.cristalise.kernel.process.resource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;


public interface ResourceLoader {

    public URL getKernelBaseURL();

    public URL getKernelResourceURL(String resName) throws MalformedURLException;

    public void addModuleBaseURL(String ns, URL newBaseURL);

    public void addModuleBaseURL(String ns, String newBaseURL) throws InvalidDataException;

    public HashMap<String, URL> getModuleBaseURLs();

    public URL getModuleResourceURL(String ns, String resName) throws MalformedURLException;


    //**************************************************************************
    // Gets any text resource files
    //**************************************************************************

    public String findTextResource(String resName);

    public HashMap<String, String> getAllTextResources(String resName);

    public String getTextResource(String ns, String resName) throws ObjectNotFoundException;

    public Class<?> getClassForName(String name) throws ClassNotFoundException;

    public ClassLoader getClassLoader(String className);

    public Enumeration<URL> getModuleDefURLs() throws Exception;

}