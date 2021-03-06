package edu.uiuc.ncsa.security.delegation.server.storage;

import edu.uiuc.ncsa.security.core.IdentifiableProvider;
import edu.uiuc.ncsa.security.core.XMLConverter;
import edu.uiuc.ncsa.security.core.exceptions.NotImplementedException;
import edu.uiuc.ncsa.security.storage.AggregateStore;
import edu.uiuc.ncsa.security.storage.data.MapConverter;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 5/24/12 at  11:18 AM
 */
public class AggregateClientStore<V extends ClientStore> extends AggregateStore<V> implements ClientStore {
    public AggregateClientStore(V... stores) {
        super(stores);
    }

    @Override
    public XMLConverter getXMLConverter() {
        throw new NotImplementedException("Error: No single converter for an aggregate store is possible");
    }

    @Override
    public MapConverter getMapConverter() {
        throw new NotImplementedException("Error: No single converter for an aggregate store is possible");
    }

    @Override
    public IdentifiableProvider getACProvider() {
        return null;
    }
}
