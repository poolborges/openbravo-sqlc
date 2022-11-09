/*
 * 
 * Copyright (C) 2001-2017 Openbravo S.L.U. Licensed under the Apache Software
 * License version 2.0 You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.openbravo.database;

import java.io.FileInputStream;
import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;


import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.exception.PoolNotFoundException;

public class JNDIConnectionProvider implements ConnectionProvider {

    protected static Logger LOGGER = Logger.getLogger(JNDIConnectionProvider.class.getName());
    protected static Map<String, PoolInfo> pools = new HashMap<String, PoolInfo>();
    protected String defaultPoolName = "";

    protected class PoolInfo implements Serializable {

        private static final long serialVersionUID = 1L;
        public String name = null;
        public DataSource ds = null;
        public String rdbms = null;
        public String dbSession = null;

        public PoolInfo(String name, DataSource ds, String rdbms, String dbSession) {
            this.name = name;
            this.ds = ds;
            this.rdbms = rdbms;
            this.dbSession = dbSession;
        }
    }

    public JNDIConnectionProvider(String file, boolean isRelative) throws PoolNotFoundException {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Creating JNDIConnectionProviderImpl from file " + file);
        }

        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(file));
            String poolName = properties.getProperty("bbdd.poolName", "myPool");
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("poolName: " + poolName);
            }

            String jndiResourceName = properties.getProperty("JNDI.resourceName");
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("jndiResourceName: " + jndiResourceName);
            }
            String dbSessionConfig = properties.getProperty("bbdd.sessionConfig");
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("dbSessionConfig: " + dbSessionConfig);
            }
            String rdbms = properties.getProperty("bbdd.rdbms");
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("rdbms: " + rdbms);
            }

            // Add the new pool to the list of available pools
            Context initctx = new InitialContext();
            Context ctx = (Context) initctx.lookup("java:/comp/env");
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Connected to java:/comp/env");
            }
            DataSource ds = (DataSource) ctx.lookup(jndiResourceName);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Datasource retrieved from JNDI server. Resource " + jndiResourceName);
            }
            pools.put(poolName, new PoolInfo(poolName, ds, rdbms, dbSessionConfig));
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Added to pools");
            }

            // First defined pool is the default pool
            if ("".equals(defaultPoolName)) {
                defaultPoolName = poolName;
            }

            // initialize the pool with dbSessionConfig
            Connection con = null;
            try {
                LOGGER.info("Initializing connection...");
                con = ds.getConnection();
                LOGGER.info(" Got connection" + con.toString());
                PreparedStatement pstmt = con.prepareStatement(dbSessionConfig);
                LOGGER.finest("Prepared statement with query: " + dbSessionConfig);
                pstmt.executeQuery();
                LOGGER.finest("Connection initialized");
            } finally {
                if (con != null) {
                    con.close();
                }
            }
            LOGGER.finest("Created JNDI ConnectionProvider");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,"Error creating JNDI connection", e);
            throw new PoolNotFoundException("Failed when creating database connections pool: "
                    + e.getMessage());
        }

    }

    public Connection getConnection() throws NoConnectionAvailableException {
        return getConnection(defaultPoolName);
    }

    public Connection getConnection(String poolName) throws NoConnectionAvailableException {
        Connection conn = null;
        try {
            conn = pools.get(poolName).ds.getConnection();
        } catch (SQLException e) {
            throw new NoConnectionAvailableException(e.getMessage());
        }

        return conn;
    }

    public String getRDBMS() {
        return getRDBMS(defaultPoolName);
    }

    public String getRDBMS(String poolName) {
        return pools.get(poolName).rdbms;
    }

    protected boolean releaseConnection(Connection conn) {
        if (conn == null) {
            return false;
        }

        try {
            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public Connection getTransactionConnection() throws NoConnectionAvailableException, SQLException {
        Connection conn = getConnection();
        if (conn == null) {
            throw new NoConnectionAvailableException("Couldn´t get an available connection");
        }
        conn.setAutoCommit(false);
        return conn;
    }

    public void releaseCommitConnection(Connection conn) throws SQLException {
        if (conn != null) {
            conn.commit();
            releaseConnection(conn);
        }
    }

    public void releaseRollbackConnection(Connection conn) throws SQLException {
        if (conn != null) {
            conn.rollback();
            releaseConnection(conn);
        }
    }

    public PreparedStatement getPreparedStatement(String SQLPreparedStatement) throws Exception {
        return getPreparedStatement(defaultPoolName, SQLPreparedStatement);
    }

    public PreparedStatement getPreparedStatement(String poolName, String SQLPreparedStatement)
            throws Exception {
        if (poolName == null || poolName.equals("")) {
            throw new PoolNotFoundException("Can't get the pool. No pool name specified");
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("connection requested");
        }
        Connection conn = getConnection(poolName);
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("connection established");
        }
        return getPreparedStatement(conn, SQLPreparedStatement);
    }

    public PreparedStatement getPreparedStatement(Connection conn, String SQLPreparedStatement)
            throws SQLException {
        if (conn == null || SQLPreparedStatement == null || SQLPreparedStatement.equals("")) {
            return null;
        }
        PreparedStatement ps = null;
        try {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("preparedStatement requested");
            }
            ps = conn.prepareStatement(SQLPreparedStatement, ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("preparedStatement received");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,"getPreparedStatement: " + SQLPreparedStatement + "\n" + e);
            releaseConnection(conn);
            throw e;
        }
        return (ps);
    }

    public CallableStatement getCallableStatement(String SQLCallableStatement) throws Exception {
        return getCallableStatement(defaultPoolName, SQLCallableStatement);
    }

    public CallableStatement getCallableStatement(String poolName, String SQLCallableStatement)
            throws Exception {
        if (poolName == null || poolName.equals("")) {
            throw new PoolNotFoundException("Can't get the pool. No pool name specified");
        }
        Connection conn = getConnection(poolName);
        return getCallableStatement(conn, SQLCallableStatement);
    }

    public CallableStatement getCallableStatement(Connection conn, String SQLCallableStatement)
            throws SQLException {
        if (conn == null || SQLCallableStatement == null || SQLCallableStatement.equals("")) {
            return null;
        }
        CallableStatement cs = null;
        try {
            cs = conn.prepareCall(SQLCallableStatement);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,"getCallableStatement: " + SQLCallableStatement + "\n" + e);
            releaseConnection(conn);
            throw e;
        }
        return (cs);
    }

    public Statement getStatement() throws Exception {
        return getStatement(defaultPoolName);
    }

    public Statement getStatement(String poolName) throws Exception {
        if (poolName == null || poolName.equals("")) {
            throw new PoolNotFoundException("Can't get the pool. No pool name specified");
        }
        Connection conn = getConnection(poolName);
        return getStatement(conn);
    }

    public Statement getStatement(Connection conn) throws SQLException {
        if (conn == null) {
            return null;
        }
        try {
            return (conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,"getStatement: " + e);
            releaseConnection(conn);
            throw e;
        }
    }

    public void releasePreparedStatement(PreparedStatement preparedStatement) throws SQLException {
        if (preparedStatement == null) {
            return;
        }
        Connection conn = null;
        try {
            conn = preparedStatement.getConnection();
            preparedStatement.close();
            releaseConnection(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,"releasePreparedStatement: " + e);
            releaseConnection(conn);
            throw e;
        }
    }

    public void releaseCallableStatement(CallableStatement callableStatement) throws SQLException {
        if (callableStatement == null) {
            return;
        }
        Connection conn = null;
        try {
            conn = callableStatement.getConnection();
            callableStatement.close();
            releaseConnection(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,"releaseCallableStatement: " + e);
            releaseConnection(conn);
            throw e;
        }
    }

    public void releaseStatement(Statement statement) throws SQLException {
        if (statement == null) {
            return;
        }
        Connection conn = null;
        try {
            conn = statement.getConnection();
            statement.close();
            releaseConnection(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,"releaseStatement: " + e);
            releaseConnection(conn);
            throw e;
        }
    }

    public void releaseTransactionalStatement(Statement statement) throws SQLException {
        if (statement == null) {
            return;
        }
        statement.close();
    }

    public void releaseTransactionalPreparedStatement(PreparedStatement preparedStatement)
            throws SQLException {
        if (preparedStatement == null) {
            return;
        }
        preparedStatement.close();
    }

    public void destroy() {
    }

    /**
     * Returns the actual status of the dynamic pool.
     */
    public String getStatus() {
        StringBuffer strResultado = new StringBuffer();
        strResultado.append("Not implemented yet");
        return strResultado.toString();
    }// End getStatus()
}
