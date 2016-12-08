package edu.uiuc.ncsa.security.storage.data;

import edu.uiuc.ncsa.security.core.Identifiable;
import edu.uiuc.ncsa.security.core.IdentifiableProvider;
import edu.uiuc.ncsa.security.storage.sql.internals.ColumnMap;

import java.util.List;

/**
 * A class that converts between objects and maps. You must supply some key.
 * <p>Created by Jeff Gaynor<br>
 * on 4/13/12 at  11:38 AM
 */
public class MapConverter<V extends Identifiable> {
    public SerializationKeys keys;
    protected IdentifiableProvider<V> provider;

    public SerializationKeys getKeys() {
        return keys;
    }

    public MapConverter(SerializationKeys keys, IdentifiableProvider<V> provider) {
        this.keys = keys;
        this.provider = provider;
    }

    /**
     * Takes a map and returns an object of the given type, initialized with the values of the map.
     *
     * @param data
     * @return
     */
    public V fromMap(ConversionMap<String, Object> data) {
        return fromMap(data, null);
    }

    public V createIfNeeded(V v){
        if (v == null) {
            v = provider.get(false);
        }
       return v;
    }
    public V fromMap(ConversionMap<String, Object> map, V v)  {
        v = createIfNeeded(v);
        v.setIdentifier(map.getIdentifier(keys.identifier()));
        return v;

    }

    /**
     * Takes the value and writes the data to the map. The reason that the map is supplied is
     * that there are many specialized maps. It would place undue constraints on this class to try
     * and manage these as well.
     *
     * @param value
     * @param data
     */
    public void toMap(V value, ConversionMap<String, Object> data) {
        data.put(keys.identifier(), value.getIdentifierString());
    }

    /**
     * Given a set of attributes, create a new object whose properties are restricted to the given list of
     * attributes. Note the the {@link SerializationKeys} has a method {@link SerializationKeys#allKeys()}
     * that allows you to get every key for this object so you can simply remove what you do not want or need.
     *
     * @param v
     * @param attributes
     * @return
     */
    public V subset(V v, List<String> attributes) {
        ColumnMap map = new ColumnMap();

        toMap(v, map);
        ColumnMap reducedMap = new ColumnMap();

        for (String key : attributes) {
            reducedMap.put(key, map.get(key));
        }
        // Have to always include the identifier.
        reducedMap.put(getKeys().identifier(), v.getIdentifierString());
        V x =  fromMap(reducedMap, null);
        return x;

    }
}
