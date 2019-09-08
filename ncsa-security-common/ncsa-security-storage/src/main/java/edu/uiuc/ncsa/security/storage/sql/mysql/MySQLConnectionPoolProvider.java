package edu.uiuc.ncsa.security.storage.sql.mysql;

import edu.uiuc.ncsa.security.core.configuration.provider.CfgEvent;
import edu.uiuc.ncsa.security.storage.sql.ConnectionPoolProvider;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 1/17/12 at  11:54 AM
 */
public class MySQLConnectionPoolProvider extends ConnectionPoolProvider<MySQLConnectionPool> {


    /**
     * Sets the defaults for this connection.
     * @param database
     * @param schema
     * @param host
     * @param port
     * @param driver
     */
    public MySQLConnectionPoolProvider(String database, String schema,  String host, int port, String driver, boolean useSSL) {
        super(database, schema, host, port, driver, useSSL);
    }

    /**
     * Another constructor, accepting the standard mysql defaults for driver, host and port.
     * @param database
     * @param schema
     */
     public MySQLConnectionPoolProvider(String database, String schema) {
         super(database, schema);

         // Note: driver classname is renamed from "com.mysql.jdbc.Driver" to "com.mysql.cj.jdbc.Driver"
         //       in mysql-connector-java 8.
         // Use 5.1 for mysql-5.1 (or at least <5.5) such as CentOS6, see
         // https://dev.mysql.com/doc/relnotes/connector-j/8.0/en/news-8-0-11.html
//       driver = "com.mysql.cj.jdbc.Driver";
         driver = "com.mysql.jdbc.Driver";
         port = 3306;
         host = "localhost";
    }


    @Override
    public Object componentFound(CfgEvent configurationEvent) {

        return null;
    }

    MySQLConnectionPool pool = null;
    @Override
    public MySQLConnectionPool get() {
        if(pool == null) {
            MySQLConnectionParameters x = new MySQLConnectionParameters(
                    checkValue(USERNAME),
                    checkValue(PASSWORD),
                    checkValue(DATABASE, database),
                    checkValue(SCHEMA, schema),
                    checkValue(HOST, host),
                    checkValue(PORT, port),
                    checkValue(DRIVER, driver),
                    checkValue(USE_SSL, useSSL),
                    checkValue(PARAMETERS,"")
            );
            pool =  new MySQLConnectionPool(x);
        }
        return pool;
    }

    @Override
    protected boolean checkEvent(CfgEvent cfgEvent) {
        return false;
    }
}
