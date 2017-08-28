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
package org.cristalise.kernel.lifecycle.instance.predefined.server;

import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;
import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.JOB;
import static org.cristalise.kernel.persistency.ClusterType.LIFECYCLE;
import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;
import static org.cristalise.kernel.persistency.ClusterType.PATH;
import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.XMLClusterStorage;

/**
 * This 
 *
 */
public class BulkImport extends PredefinedStep {

    /**
     * 
     */
    public static final String BULK_IMPORT_ROOT_DIRECTORY = "BulkImport.rootDirectory";
    /**
     * 
     */
    public static final String BULK_IMPORT_USE_DIRECTORIES = "BulkImport.useDirectories";
    /**
     * 
     */
    public static final String BULK_IMPORT_FILE_EXTENSION = "BulkImport.fileExtension";

    private String  root;
    private String  ext;
    private Boolean  useDir;

    XMLClusterStorage importCluster;

    public BulkImport() {
        super();

        root   = Gateway.getProperties().getString( BULK_IMPORT_ROOT_DIRECTORY);
        ext    = Gateway.getProperties().getString( BULK_IMPORT_FILE_EXTENSION, "");
        useDir = Gateway.getProperties().getBoolean(BULK_IMPORT_USE_DIRECTORIES, false);
    }

    public void initialise() throws InvalidDataException {
        if (importCluster == null) {
            if (root == null)
                throw new InvalidDataException("BulkImport.runActivityLogic() - Root path not given in config file.");

            importCluster = new XMLClusterStorage(root, ext, useDir);
        }
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, Object locker)
            throws InvalidDataException, InvalidCollectionModification, ObjectAlreadyExistsException, ObjectCannotBeUpdated,
                   ObjectNotFoundException, PersistencyException, CannotManageException
    {
        initialise();

        importAllClusters();

        return requestData;
    }

    public void importAllClusters() throws InvalidDataException, PersistencyException {
        for (ItemPath item: getItemsToImport(root)) {
            Object sublocker = new Object();

            for (ClusterType type : importCluster.getClusters(item)) {
                switch (type) {
                    case PATH:       importPath(item, sublocker);       break;
                    case PROPERTY:   importProperty(item, sublocker);   break;
                    case LIFECYCLE:  importLifeCycle(item, sublocker);  break;
                    case HISTORY:    importHistory(item, sublocker);    break;
                    case VIEWPOINT:  importViewPoint(item, sublocker);  break;
                    case OUTCOME:    importOutcome(item, sublocker);    break;
                    case COLLECTION: importCollection(item, sublocker); break;
                    case JOB:        importJob(item, sublocker);        break;

                    default:
                        break;
                }
            }

            //importCluster.delete(item, "");

            Gateway.getStorage().commit(sublocker);
        }
    }

    private List<ItemPath> getItemsToImport(String root) throws InvalidDataException {
        List<ItemPath> items = new ArrayList<>();
        try {
            try (Stream<Path> files = Files.walk(Paths.get(root))) {
                files.filter(Files::isDirectory)
                    .forEach(path -> {
                        try {
                            items.add(new ItemPath(path.getFileName().toString()));
                        }
                        catch (InvalidItemPathException e) {
                            Logger.warning("BulkImport.getItemsToImport() - Unvalid UUID for import directory:"+path.getFileName().toString());
                        }
                    } );
            }

            return items;
        }
        catch (IOException e) {
            Logger.error(e);
            throw new InvalidDataException(e.getMessage());
        }
    }

    public void importProperty(ItemPath item, Object locker) throws PersistencyException {
        String[] contents = importCluster.getClusterContents(item, PROPERTY);

        for (String c : contents) {
            String path = PROPERTY+"/"+c;
            C2KLocalObject prop = importCluster.get(item, path);
            Gateway.getStorage().put(item, prop, locker);

            //importCluster.delete(item, path);
        }
    }

    public void importViewPoint(ItemPath item, Object locker) throws PersistencyException {
        String[] contents = importCluster.getClusterContents(item, VIEWPOINT);

        for (String c : contents) {
            String[] subContents = importCluster.getClusterContents(item, VIEWPOINT+"/"+c);

            for (String sc : subContents) {
                String path = VIEWPOINT+"/"+c+"/"+sc;
                C2KLocalObject view = importCluster.get(item, path);
                Gateway.getStorage().put(item, view, locker);

                //importCluster.delete(item, path);
            }
        }
    }

    public void importLifeCycle(ItemPath item, Object locker) throws PersistencyException {
        String[] contents = importCluster.getClusterContents(item, LIFECYCLE);

        for (String c : contents) {
            String path = LIFECYCLE+"/"+c;
            C2KLocalObject wf = importCluster.get(item, path);
            Gateway.getStorage().put(item, wf, locker);

            //importCluster.delete(item, path);
        }
    }

    public void importHistory(ItemPath item, Object locker) throws PersistencyException {
        String[] contents = importCluster.getClusterContents(item, HISTORY);

        for (String c : contents) {
            String path = HISTORY+"/"+c;
            C2KLocalObject obj = importCluster.get(item, path);
            Gateway.getStorage().put(item, obj, locker);

            //importCluster.delete(item, path);
        }
    }

    public void importOutcome(ItemPath item, Object locker) throws PersistencyException {
        String[] schemas = importCluster.getClusterContents(item, OUTCOME);

        for (String schema : schemas) {
            String[] versions = importCluster.getClusterContents(item, OUTCOME+"/"+schema);

            for (String version : versions) {
                String[] events = importCluster.getClusterContents(item, OUTCOME+"/"+schema+"/"+version);

                for (String event : events) {
                    C2KLocalObject obj = importCluster.get(item, OUTCOME+"/"+schema+"/"+version+"/"+event);
                    Gateway.getStorage().put(item, obj, locker);

                    //importCluster.delete(item, path.toString());
                }
            }
        }
    }

    public void importJob(ItemPath item, Object locker) throws PersistencyException {
        String[] contents = importCluster.getClusterContents(item, JOB);

        for (String c : contents) {
            String path = JOB+"/"+c;
            C2KLocalObject job = importCluster.get(item, path);
            Gateway.getStorage().put(item, job, locker);

            //importCluster.delete(item, path);
        }
    }

    public void importCollection(ItemPath item, Object locker) throws PersistencyException {
        String[] names = importCluster.getClusterContents(item, COLLECTION);

        for (String name : names) {
            String[] versions = importCluster.getClusterContents(item, COLLECTION+"/"+name);

            for (String version : versions) {
                C2KLocalObject coll = importCluster.get(item, COLLECTION+"/"+name+"/"+version);
                Gateway.getStorage().put(item, coll, locker);

                //importCluster.delete(item, path.toString());
            }
        }
    }

    public void importDomainPath(ItemPath item, Object locker) throws PersistencyException {
        String[] domains = importCluster.getClusterContents(item, PATH+"/Domain");

        for (String name : domains) {
            try {
                Gateway.getLookupManager().add( (DomainPath)importCluster.get(item, PATH+"/Domain/"+name) );
            }
            catch (ObjectCannotBeUpdated | ObjectAlreadyExistsException | CannotManageException e) {
                Logger.error(e);
                throw new PersistencyException(e.getMessage());
            }

            //importCluster.delete(item, PATH+"/Domain/"+name);
        }
    }

    public void importRolePath(ItemPath item, AgentPath agentPath, Object locker) throws PersistencyException {
        String[] roles = importCluster.getClusterContents(item, PATH+"/Role");

        for (String role : roles) {
            RolePath rolePath = (RolePath)importCluster.get(item, PATH+"/Role/"+role);

            if (!Gateway.getLookup().exists(rolePath)) {
                try {
                    Gateway.getLookupManager().add(rolePath);
                    if (agentPath != null) Gateway.getLookupManager().addRole(agentPath, rolePath);
                }
                catch (ObjectCannotBeUpdated | ObjectAlreadyExistsException | CannotManageException | ObjectNotFoundException e) {
                    Logger.error(e);
                    throw new PersistencyException(e.getMessage());
                }
            }

            //importCluster.delete(item, PATH+"/Role/"+role);
        }
        
    }

    public ItemPath importItemPath(ItemPath item, Object locker) throws PersistencyException {
        try {
            ItemPath itemPath = (ItemPath)importCluster.get(item, PATH+"/Item");
            Gateway.getLookupManager().add(itemPath);

            //importCluster.delete(item, PATH+"/Item");

            return itemPath;
        }
        catch (ObjectCannotBeUpdated | ObjectAlreadyExistsException | CannotManageException e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    public AgentPath importAgentPath(ItemPath item, Object locker) throws PersistencyException {
        try {
            AgentPath agentPath = (AgentPath)importCluster.get(item, PATH+"/Item");
            Gateway.getLookupManager().add(agentPath);

            Gateway.getLookupManager().setAgentPassword(agentPath, "aaa");

            //importCluster.delete(item, PATH+"/Item");

            return agentPath;
        }
        catch (ObjectCannotBeUpdated | ObjectAlreadyExistsException | CannotManageException | ObjectNotFoundException | NoSuchAlgorithmException e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
        
    }

    public void importPath(ItemPath item, Object locker) throws PersistencyException {
        String[] contents = importCluster.getClusterContents(item, PATH);

        AgentPath agentPath = null;
        String entity = "";

        if (Arrays.asList(contents).contains("Item"))  entity = "Item";
        if (Arrays.asList(contents).contains("Agent")) entity = "Agent";


        if (StringUtils.isNotBlank(entity)) {
            if      (entity.equals("Item"))  importItemPath(item, locker);
            else if (entity.equals("Agent")) agentPath = importAgentPath(item, locker);
        }
        else Logger.warning("BulkImport.importPath() - WARNING: '"+item+"' has no Path.Item or Path.Agent files");;

        for (String c : contents) {
            if      (c.equals("Domain")) importDomainPath(item, locker);
            else if (c.equals("Role"))   importRolePath(item, agentPath, locker);
        }
    }
}
