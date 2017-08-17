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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;

public class XMLClusterStorage extends ClusterStorage {
    String  rootDir        = null;
    String  fileExtension  = ".xml";
    boolean useDirectories = true;

    public XMLClusterStorage() {}

    /**
     * Create new XMLClusterStorage with specific setup, Used in predefined step 
     * {@link org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport}
     * 
     * @param root specify the root directory
     */
    public XMLClusterStorage(String root) {
        this(root, null, null);
    }

    /**
     * Create new XMLClusterStorage with specific setup, Used in predefined step 
     * {@link org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport}
     * 
     * @param root specify the root directory
     * @param ext the extension of the files with dot, e.g. '.xml', used to save the cluster content.
     *        If it is null the default '.xml' extension is used.
     * @param useDir specify if the files should be stored in directories or in single files, e.g. Property.Type,xml
     *        If it is null the default is true.
     */
    public XMLClusterStorage(String root, String ext, Boolean useDir) {
        rootDir = new File(root).getAbsolutePath();

        if (ext    != null) fileExtension  = ext;
        if (useDir != null) useDirectories = useDir;
    }

    @Override
    public void open(Authenticator auth) throws PersistencyException {
        if (StringUtils.isBlank(rootDir)) {
            String rootProp = Gateway.getProperties().getString("XMLStorage.root");

            if (rootProp == null)
                throw new PersistencyException("XMLClusterStorage.open() - Root path not given in config file.");

            rootDir = new File(rootProp).getAbsolutePath();
        }

        if (!FileStringUtility.checkDir(rootDir)) {
            Logger.error("XMLClusterStorage.open() - Path " + rootDir + "' does not exist. Attempting to create.");
            boolean success = FileStringUtility.createNewDir(rootDir);

            if (!success)
                throw new PersistencyException("XMLClusterStorage.open() - Could not create dir " + rootDir + ". Cannot continue.");
        }
        
        Logger.debug(5, "XMLClusterStorage.open() - DONE rootDir:'" + rootDir + "' ext:'" + fileExtension + "' userDir:" + useDirectories);
    }

    @Override
    public void close() {
        rootDir = null;
    }

    // introspection
    @Override
    public short queryClusterSupport(ClusterType clusterType) {
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
            ClusterType type      = ClusterStorage.getClusterType(path);
            String      filePath  = getFilePath(itemPath, path) + fileExtension;
            String      objString = FileStringUtility.file2String(filePath);

            if (objString.length() == 0) return null;

            Logger.debug(9, "XMLClusterStorage.get() - objString:" + objString);

            if (type == ClusterType.OUTCOME) return new Outcome(path, objString);
            else                             return (C2KLocalObject) Gateway.getMarshaller().unmarshall(objString);
        }
        catch (Exception e) {
            Logger.msg(3, "XMLClusterStorage.get() - The path " + path + " from " + itemPath + " does not exist.: " + e.getMessage());
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj) throws PersistencyException {
        try {
            String filePath = getFilePath(itemPath, getPath(obj) + fileExtension);
            Logger.msg(7, "XMLClusterStorage.put() - Writing " + filePath);
            String data = Gateway.getMarshaller().marshall(obj);

            String dir = filePath.substring(0, filePath.lastIndexOf('/'));

            if (!FileStringUtility.checkDir(dir)) {
                boolean success = FileStringUtility.createNewDir(dir);
                if (!success)
                    throw new PersistencyException("XMLClusterStorage.put() - Could not create dir " + dir + ". Cannot continue.");
            }
            FileStringUtility.string2File(filePath, data);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException("XMLClusterStorage.put() - Could not write " + getPath(obj) + " to " + itemPath);
        }
    }

    @Override
    public void delete(ItemPath itemPath, String path) throws PersistencyException {
        try {
            String filePath = getFilePath(itemPath, path + fileExtension);
            boolean success = FileStringUtility.deleteDir(filePath, true, true);
            if (success) return;

            filePath = getFilePath(itemPath, path);
            success = FileStringUtility.deleteDir(filePath, true, true);
            if (success) return;
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException(
                    "XMLClusterStorage.delete() - Failure deleting path " + path + " in " + itemPath + " Error: " + e.getMessage());
        }
        throw new PersistencyException("XMLClusterStorage.delete() - Failure deleting path " + path + " in " + itemPath);
    }

    @Override
    public String[] getClusterContents(ItemPath itemPath, String path) throws PersistencyException {
        try {
            if (useDirectories) return getContentsFromDirectories(itemPath, path);
            else                return getContentsFromFileNames(itemPath, path);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException("XMLClusterStorage.getClusterContents() - Could not get contents of " + path + " from "
                    + itemPath + ": " + e.getMessage());
        }
    }

    private String[] getContentsFromFileNames(ItemPath itemPath, String path) throws IOException {
        TreeSet<String> result = new TreeSet<>();

        String resource = getResourceName(path);

        try (Stream<Path> pathes = Files.list(Paths.get(rootDir + "/" + itemPath.getUUID()))) {
            pathes.filter(p -> p.getFileName().toString().startsWith(resource))
                  .forEach(p -> {
                      String content = p.getFileName().toString().substring(resource.length()+1);

                      if (content.endsWith(fileExtension)) content = content.substring(0, content.length() - fileExtension.length());

                      int i = content.indexOf('.');
                      if (i != -1) content = content.substring(0, i);

                      result.add(content);
                  });
        }
        return result.toArray(new String[0]);
    }

    private String[] getContentsFromDirectories(ItemPath itemPath, String path) {
        String[] result = new String[0];

        String filePath = getFilePath(itemPath, path);
        ArrayList<String> paths = FileStringUtility.listDir(filePath, true, false);
        if (paths == null) return result; // dir doesn't exist yet

        ArrayList<String> contents = new ArrayList<String>();
        String previous = null;
        for (int i = 0; i < paths.size(); i++) {
            String next = paths.get(i);

            // trim off the extension (e.g '.xml') from the end if it's there
            if (next.endsWith(fileExtension)) next = next.substring(0, next.length() - fileExtension.length());

            // avoid duplicates (xml and dir)
            if (next.equals(previous)) continue;
            previous = next;

            // only keep the last bit of the path
            if (next.indexOf('/') > -1) next = next.substring(next.lastIndexOf('/') + 1);
            contents.add(next);
        }

        result = contents.toArray(result);

        return result;
    }

    protected String getFilePath(ItemPath itemPath, String path)  {
        path = getResourceName(path);

        String filePath = rootDir + "/" + itemPath.getUUID() + "/" + path;
        Logger.msg(8, "XMLClusterStorage.getFilePath() - " + filePath);

        return filePath;
    }

    protected String getResourceName(String path) {
        //remove leading '/' if exists
        if (path.length() != 0 && path.charAt(0) == '/') path = path.substring(1);

        if (!useDirectories) path = path.replace("/", ".");

        return path;
    }
}
