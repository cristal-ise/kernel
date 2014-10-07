/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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

//Java
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * @author $Author: abranson $ $Date: 2005/10/05 07:39:36 $
 * @version $Revision: 1.71 $
 **************************************************************************/
public class Resource implements ResourceLoader {

	private final Hashtable<String, String> txtCache = new Hashtable<String, String>();
    private final URL baseURL;
    private final HashMap<String, URL> moduleBaseURLs = new HashMap<String, URL>();
    private final HashMap<String, URL> allBaseURLs = new HashMap<String, URL>();

    public Resource() throws InvalidDataException {
    	baseURL = getURLorResURL("org/cristalise/kernel/utils/resources/");
    	allBaseURLs.put(null, baseURL);
    }
    
    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.ResourceLoader#getKernelBaseURL()
	 */
    @Override
	public URL getKernelBaseURL() {
        return baseURL;
    }

    @Override
	public ClassLoader getClassLoader(String className) {
		return Resource.class.getClassLoader();
	}
    
    @Override
	public Class<?> getClassForName(String name) throws ClassNotFoundException {
		return Class.forName(name);
	}

	/* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.ResourceLoader#getKernelResourceURL(java.lang.String)
	 */
    @Override
	public URL getKernelResourceURL(String resName) throws MalformedURLException {
        return new URL(baseURL, resName);
    }

    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.ResourceLoader#addModuleBaseURL(java.lang.String, java.net.URL)
	 */
    @Override
	public void addModuleBaseURL(String ns, URL newBaseURL) {
        moduleBaseURLs.put(ns, newBaseURL);
        allBaseURLs.put(ns, newBaseURL);
        Logger.msg("Adding resource URL for "+ns+": "+newBaseURL.toString());
    }

    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.ResourceLoader#addModuleBaseURL(java.lang.String, java.lang.String)
	 */
    @Override
	public void addModuleBaseURL(String ns, String newBaseURL) throws InvalidDataException {
    	addModuleBaseURL(ns, getURLorResURL(newBaseURL));
    }
    
    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.ResourceLoader#getModuleBaseURLs()
	 */
    @Override
	public HashMap<String, URL> getModuleBaseURLs() {
        return moduleBaseURLs;
    }
    
    private HashMap<String, URL> getAllBaseURLs() {
        return allBaseURLs;
    }

    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.ResourceLoader#getModuleResourceURL(java.lang.String, java.lang.String)
	 */
    @Override
	public URL getModuleResourceURL(String ns, String resName) throws MalformedURLException {
        return new URL(moduleBaseURLs.get(ns), resName);
    }

    static private URL getURLorResURL(String newURL) throws InvalidDataException {
        URL result;
        try {
            result = new URL(newURL);
        } catch (java.net.MalformedURLException ex) {
            //it is assumed that a 'wrong' URL denotes a directory
            //of files resolvable from the CLASSPATH
            result = Resource.class.getClassLoader().getResource(newURL);
        }
        if (result == null) {
        	Logger.error("URL "+newURL+" could not be found");
        	throw new InvalidDataException();
        }
        return result;
    }
    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.ResourceLoader#findTextResource(java.lang.String)
	 */
    
    @Override
	public String findTextResource(String resName) {
    	for (String ns : getAllBaseURLs().keySet()) {
    		try {
    			return getTextResource(ns, resName);
    		} catch (ObjectNotFoundException ex) { }
    	}
        Logger.warning("Text resource '"+resName+"' not found.");
        return null;
    }
    
    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.ResourceLoader#getAllTextResources(java.lang.String)
	 */
    @Override
	public HashMap<String, String> getAllTextResources(String resName) {
    	HashMap<String, String> results = new HashMap<String, String>();
    	for (String ns : getAllBaseURLs().keySet()) {
        	try {
        		results.put(ns, getTextResource(ns, resName));
        	} catch (ObjectNotFoundException ex) { }
    	}
    	return results;
    }
    
    /* (non-Javadoc)
	 * @see org.cristalise.kernel.utils.ResourceLoader#getTextResource(java.lang.String, java.lang.String)
	 */
    @Override
	public String getTextResource(String ns, String resName) throws ObjectNotFoundException
    //        throws IOException
    {
        Logger.msg(8, "Resource::getTextResource() - Getting resource from "+ns+": " + resName);

        if (txtCache.containsKey(ns+'/'+resName)) {
            return txtCache.get(ns+'/'+resName);
        }

        try {

            String newRes = null;
            URL loc;
            
            if (ns == null) // kernel
                loc = getKernelResourceURL(resName);
            else
            	loc = getModuleResourceURL(ns, resName);
            Logger.msg(5, "Loading resource: "+loc.toString());
            newRes = FileStringUtility.url2String(loc);
            txtCache.put(ns+'/'+resName, newRes);
            return newRes;
        } catch (Exception e) {
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

	@Override
	public Enumeration<URL> getModuleDefURLs() throws Exception {
		return ClassLoader.getSystemResources("META-INF/cristal/module.xml");
	}
}
