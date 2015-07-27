package org.cristalise.kernel.lifecycle.routingHelpers;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.ItemPath;

public interface DataHelper {

	public String get(ItemPath item, String dataPath, Object locker) throws InvalidDataException, PersistencyException, ObjectNotFoundException;
}
