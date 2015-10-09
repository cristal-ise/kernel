package org.cristalise.kernel.persistency;

import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;

public abstract class TransactionalClusterStorage extends ClusterStorage {

	public abstract void begin(Object locker);

	public abstract void commit(Object locker);
	
	public abstract void abort(Object locker);

	public abstract void put(ItemPath itemPath, C2KLocalObject obj, Object locker);

	public abstract void delete(ItemPath itemPath, String path, Object locker);

}
