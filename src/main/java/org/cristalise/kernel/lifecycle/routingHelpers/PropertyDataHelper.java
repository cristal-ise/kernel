package org.cristalise.kernel.lifecycle.routingHelpers;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.exolab.castor.mapping.xml.Property;

public class PropertyDataHelper implements DataHelper {

	@Override
	public String get(ItemPath item, String dataPath, Object locker)
			throws InvalidDataException, PersistencyException,
			ObjectNotFoundException {
		//Syntax of search : <PropertyName>
		Property prop = (Property)Gateway.getStorage().get(item, ClusterStorage.PROPERTY+"/"+dataPath, locker);
		return prop.getValue();
	}
	
}