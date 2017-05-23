package edu.uiuc.ncsa.security.storage.sql.internals;

import java.sql.Types;

/**
 * Looks up the vendor-specific SQL type that corresponds to a {@link java.sql.Types}
 * given type. Low-level utility and is not of interest to developers in general.
 * <p>Created by Jeff Gaynor<br>
 * on 8/30/11 at  3:49 PM
 */
public abstract class ColumnTypeTranslator {
    public String toSQL(ColumnDescriptorEntry cde) {
	try {
	    return toSQL(cde.getType());
	} catch (IllegalArgumentException e)	{
	    throw new IllegalArgumentException("Error: unsupported type "+cde.getType()+" for "+cde.getName());
	}
    }

    public String toSQL(int sqlType) {
        switch (sqlType) {
            case Types.LONGVARBINARY:
            case Types.VARBINARY:
                return "bytea";
            case Types.BOOLEAN:
                return "boolean";
            case Types.BIGINT:
                return "bigint";
            case Types.LONGVARCHAR:
            case Types.VARCHAR:
                return "text";
            case Types.TIMESTAMP:
                return "timestamp";
            default:
                throw new IllegalArgumentException("Error: This type is unsupported: "+sqlType);
        }
    }
}
