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
package org.cristalise.storage;
import java.io.File;
import java.util.ArrayList;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;


public class XMLClusterStorage extends ClusterStorage {
    String rootDir=null;

    public XMLClusterStorage() {
    }

    @Override
	public void open(Authenticator auth) throws PersistencyException {
        String rootProp = Gateway.getProperties().getString("XMLStorage.root");
        if (rootProp == null)
            throw new PersistencyException("XMLClusterStorage.open() - Root path not given in config file.");

        rootDir = new File(rootProp).getAbsolutePath();

        if( !FileStringUtility.checkDir( rootDir ) ) {
            Logger.error("XMLClusterStorage.open() - Path " + rootDir + "' does not exist. Attempting to create.");
            boolean success = FileStringUtility.createNewDir(rootDir);
            if (!success) throw new PersistencyException("XMLClusterStorage.open() - Could not create dir "+ rootDir +". Cannot continue.");
        }
    }

    @Override
	public void close() {
        rootDir = null;
    }

    // introspection
    @Override
	public short queryClusterSupport(String clusterType) {
        return ClusterStorage.READWRITE;
    }

    @Override
	public String getName() {
        return "XML File Cluster Storage";
    }

    @Override
	public String getId() {
        return "XML";
    }

    @Override
    public boolean checkQuerySupport(String language) {
        Logger.warning("XMLClusterStorage DOES NOT Support any query");
        return false;
    }

    @Override
    public String executeQuery(Query query) throws PersistencyException {
        throw new PersistencyException("UNIMPLEMENTED funnction");
    }

    @Override
	public C2KLocalObject get(ItemPath itemPath, String path) throws PersistencyException {
        try {
            String type = ClusterStorage.getClusterType(path);
            String filePath = getFilePath(itemPath, path)+".xml";
            String objString = FileStringUtility.file2String(filePath);
            if (objString.length() == 0) return null;
            Logger.debug(9, "XMLClusterStorage.get() - objString:" + objString);

            if (type.equals("Outcome")) return new Outcome(path, objString);
            else                        return (C2KLocalObject)Gateway.getMarshaller().unmarshall(objString);

        } catch (Exception e) {
            Logger.msg(3,"XMLClusterStorage.get() - The path "+path+" from "+itemPath+" does not exist.: "+e.getMessage());
        }
        return null;
    }

    // store object by path
    @Override
	public void put(ItemPath itemPath, C2KLocalObject obj) throws PersistencyException {
        try {
            String filePath = getFilePath(itemPath, getPath(obj)+".xml");
            Logger.msg(7, "XMLClusterStorage.put() - Writing "+filePath);
            String data = Gateway.getMarshaller().marshall(obj);

            String dir = filePath.substring(0, filePath.lastIndexOf('/'));
        if( !FileStringUtility.checkDir( dir ) ) {
            boolean success = FileStringUtility.createNewDir(dir);
            if (!success) throw new PersistencyException("XMLClusterStorage.put() - Could not create dir "+ dir +". Cannot continue.");
        }
            FileStringUtility.string2File(filePath, data);
        } catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException("XMLClusterStorage.put() - Could not write "+getPath(obj)+" to "+itemPath);
        }
    }

    // delete cluster
    @Override
	public void delete(ItemPath itemPath, String path) throws PersistencyException {
        try {
            String filePath = getFilePath(itemPath, path+".xml");
            boolean success = FileStringUtility.deleteDir(filePath, true, true);
            if (success) return;
            filePath = getFilePath(itemPath, path);
            success = FileStringUtility.deleteDir(filePath, true, true);
            if (success) return;
        } catch(Exception e) {
            Logger.error(e);
            throw new PersistencyException("XMLClusterStorage.delete() - Failure deleting path "+path+" in "+itemPath + " Error: "+e.getMessage());
        }
        throw new PersistencyException("XMLClusterStorage.delete() - Failure deleting path "+path+" in "+itemPath);
    }

    /* navigation */

    // directory listing
    @Override
	public String[] getClusterContents(ItemPath itemPath, String path) throws PersistencyException {
		String[] result = new String[0];
        try {
            String filePath = getFilePath(itemPath, path);
            ArrayList<String> paths = FileStringUtility.listDir( filePath, true, false );
            if (paths == null) return result; // dir doesn't exist yet
            ArrayList<String> contents = new ArrayList<String>();
            String previous = null;
            for (int i=0; i<paths.size(); i++) {
                String next = paths.get(i);

                // trim off the xml from the end if it's there
                if (next.endsWith(".xml")) next = next.substring(0, next.length()-4);

                // avoid duplicates (xml and dir)
                if (next.equals(previous)) continue;
                previous = next;

                // only keep the last bit of the path
                if (next.indexOf('/') > -1) next = next.substring(next.lastIndexOf('/')+1);
                contents.add(next);
            }

            result = contents.toArray(result);
            return result;
        } catch (Exception e) {
        	Logger.error(e);
            throw new PersistencyException("XMLClusterStorage.getClusterContents() - Could not get contents of "+path+" from "+itemPath+": "+e.getMessage());
        }
    }

    protected String getFilePath(ItemPath itemPath, String path) throws InvalidItemPathException {
        if (path.length() == 0 || path.charAt(0) != '/') path = "/"+path;
        String filePath = rootDir+itemPath.toString()+path;
        Logger.msg(8, "XMLClusterStorage.getFilePath() - "+filePath);
        return filePath;
    }
}
