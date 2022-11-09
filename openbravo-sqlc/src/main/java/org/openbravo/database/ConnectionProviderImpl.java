/*
 ************************************************************************************
 * Copyright (C) 2001-2017 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.StackKeyedObjectPoolFactory;

import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.exception.PoolNotFoundException;

public class ConnectionProviderImpl implements ConnectionProvider {

    static Logger LOGGER = Logger.getLogger(ConnectionProviderImpl.class.getName());
    String defaultPoolName = "";
    String bbdd = "";
    String rdbms = "";
    String contextName = "openbravo";
    private String externalPoolClassName;

    private static ExternalConnectionPool externalConnectionPool;

    public ConnectionProviderImpl(Properties properties) throws PoolNotFoundException {
        create(properties, false, "openbravo");
    }

    public ConnectionProviderImpl(String file) throws PoolNotFoundException {
        this(file, false, "openbravo");
    }

    public ConnectionProviderImpl(String file, String _context) throws PoolNotFoundException {
        this(file, false, _context);
    }

    public ConnectionProviderImpl(String file, boolean isRelative, String _context)
            throws PoolNotFoundException {
        create(file, isRelative, _context);
    }

    private void create(String file, boolean isRelative, String _context)
            throws PoolNotFoundException {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));
            create(properties, isRelative, _context);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"Error loading properties", e);
        }
    }

    private void create(Properties properties, boolean isRelative, String _context)
            throws PoolNotFoundException {

        LOGGER.finest("Creating ConnectionProviderImpl");
        if (_context != null && !_context.equals("")) {
            contextName = _context;
        }

        String poolName = null;
        String dbDriver = null;
        String dbServer = null;
        String dbLogin = null;
        String dbPassword = null;
        int minConns = 1;
        int maxConns = 10;
        double maxConnTime = 0.5;
        String dbSessionConfig = null;
        String _rdbms = null;

        poolName = properties.getProperty("bbdd.poolName", "myPool");
        externalPoolClassName = properties.getProperty("db.externalPoolClassName");
        dbDriver = properties.getProperty("bbdd.driver");
        dbServer = properties.getProperty("bbdd.url");
        dbLogin = properties.getProperty("bbdd.user");
        dbPassword = properties.getProperty("bbdd.password");
        minConns = new Integer(properties.getProperty("bbdd.minConns", "1"));
        maxConns = new Integer(properties.getProperty("bbdd.maxConns", "10"));
        maxConnTime = new Double(properties.getProperty("maxConnTime", "0.5"));
        dbSessionConfig = properties.getProperty("bbdd.sessionConfig");
        _rdbms = properties.getProperty("bbdd.rdbms");
        if (_rdbms.equalsIgnoreCase("POSTGRE")) {
            dbServer += "/" + properties.getProperty("bbdd.sid");
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("poolName: " + poolName);
            LOGGER.finest("externalPoolClassName: " + externalPoolClassName);
            LOGGER.finest("dbDriver: " + dbDriver);
            LOGGER.finest("dbServer: " + dbServer);
            LOGGER.finest("dbLogin: " + dbLogin);
            LOGGER.finest("dbPassword: " + dbPassword);
            LOGGER.finest("minConns: " + minConns);
            LOGGER.finest("maxConns: " + maxConns);
            LOGGER.finest("maxConnTime: " + Double.toString(maxConnTime));
            LOGGER.finest("dbSessionConfig: " + dbSessionConfig);
            LOGGER.finest("rdbms: " + _rdbms);
        }

        if (externalPoolClassName != null && !"".equals(externalPoolClassName)) {
            try {
                externalConnectionPool = ExternalConnectionPool.getInstance(externalPoolClassName);
            } catch (Throwable e) {
                externalConnectionPool = null;
                externalPoolClassName = null;
            }
        }

        try {
            addNewPool(dbDriver, dbServer, dbLogin, dbPassword, minConns, maxConns, maxConnTime,
                    dbSessionConfig, _rdbms, poolName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,"",e);
            throw new PoolNotFoundException("Failed when creating database connections pool", e);
        }
    }

    public void destroy(String name) throws Exception {
        if (externalConnectionPool != null) {
            externalConnectionPool.closePool();
            externalConnectionPool = null;
        } else {
            PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
            driver.closePool(name);
        }
    }

    public void reload(String file, boolean isRelative, String _context) throws Exception {
        destroy();
        create(file, isRelative, _context);
    }

    public void destroy() throws Exception {
        destroy(defaultPoolName);
    }

    public void addNewPool(String dbDriver, String dbServer, String dbLogin, String dbPassword,
            int minConns, int maxConns, double maxConnTime, String dbSessionConfig, String _rdbms,
            String name) throws Exception {

        if (this.defaultPoolName == null || this.defaultPoolName.equals("")) {
            this.defaultPoolName = name;
            this.bbdd = dbServer;
            this.rdbms = _rdbms;
        }
        if (externalConnectionPool != null) {
            // No need to create and add a new pool
            return;
        }

        LOGGER.finest("Loading underlying JDBC driver.");
        try {
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            throw new Exception(e);
        }
        LOGGER.finest("Done.");

        GenericObjectPool connectionPool = new GenericObjectPool(null);
        connectionPool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);
        connectionPool.setMaxActive(maxConns);
        connectionPool.setTestOnBorrow(false);
        connectionPool.setTestOnReturn(false);
        connectionPool.setTestWhileIdle(false);

        KeyedObjectPoolFactory keyedObject = new StackKeyedObjectPoolFactory();
        ConnectionFactory connectionFactory = new OpenbravoDriverManagerConnectionFactory(dbServer,
                dbLogin, dbPassword, dbSessionConfig, _rdbms);
        @SuppressWarnings("unused")
        // required by dbcp
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
                connectionFactory, connectionPool, keyedObject, null, false, true);

        Class.forName("org.apache.commons.dbcp.PoolingDriver");
        PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
        driver.registerPool(contextName + "_" + name, connectionPool);

    }

    public ObjectPool getPool(String poolName) throws PoolNotFoundException {
        if (poolName == null || poolName.equals("")) {
            throw new PoolNotFoundException("Couldn´t get an unnamed pool");
        }
        ObjectPool connectionPool = null;
        try {
            PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
            connectionPool = driver.getConnectionPool(contextName + "_" + poolName);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE,"",ex);
        }
        if (connectionPool == null) {
            throw new PoolNotFoundException(poolName + " not found");
        } else {
            return connectionPool;
        }
    }

    public ObjectPool getPool() throws PoolNotFoundException {
        return getPool(defaultPoolName);
    }

    public Connection getConnection() throws NoConnectionAvailableException {
        return getConnection(defaultPoolName);
    }

    /**
     * Optimization, try to get the connection associated with the current
     * thread, to always get the same connection for all getConnection() calls
     * inside a request.
     */
    public Connection getConnection(String poolName) throws NoConnectionAvailableException {
        if (poolName == null || poolName.equals("")) {
            throw new NoConnectionAvailableException("Couldn´t get a connection for an unnamed pool");
        }

        // try to get the connection from the session to use a single connection for the
        // whole request
        Connection conn = SessionInfo.getSessionConnection();
        if (conn == null) {
            conn = getNewConnection(poolName);
            SessionInfo.setSessionConnection(conn);
        }
        return conn;
    }

    /**
     * Gets a new connection without trying to obtain the sessions's one from
     * available pool
     */
    private Connection getNewConnection(String poolName) throws NoConnectionAvailableException {
        if (poolName == null || poolName.equals("")) {
            throw new NoConnectionAvailableException("Couldn´t get a connection for an unnamed pool");
        }
        Connection conn;
        if (externalConnectionPool == null && externalPoolClassName != null
                && !"".equals(externalPoolClassName)) {
            try {
                externalConnectionPool = ExternalConnectionPool.getInstance(externalPoolClassName);
            } catch (Throwable e) {
                externalConnectionPool = null;
                externalPoolClassName = null;
            }
        }

        if (externalConnectionPool != null) {
            conn = externalConnectionPool.getConnection();
        } else {
            conn = getCommonsDbcpPoolConnection(poolName);
        }
        return conn;
    }

    /**
     * Gets a new connection without trying to obtain the sessions's one from
     * DBCP pool
     */
    private Connection getCommonsDbcpPoolConnection(String poolName)
            throws NoConnectionAvailableException {
        if (poolName == null || poolName.equals("")) {
            throw new NoConnectionAvailableException("Couldn´t get a connection for an unnamed pool");
        }
        Connection conn = null;
        try {
            conn = DriverManager
                    .getConnection("jdbc:apache:commons:dbcp:" + contextName + "_" + poolName);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE,"Error getting connection", ex);
            throw new NoConnectionAvailableException(
                    "There are no connections available in jdbc:apache:commons:dbcp:" + contextName + "_"
                    + poolName);
        }
        return conn;
    }

    public String getRDBMS() {
        return rdbms;
    }

    public boolean releaseConnection(Connection conn) {
        if (conn == null) {
            return false;
        }
        try {
            // Set autocommit, this makes not necessary to explicitly commit, all
            // prepared statements are
            // commited
            conn.setAutoCommit(true);
            if (SessionInfo.getSessionConnection() == null) {
                // close connection if it's not attached to session, other case it
                // will be closed when the
                // request is done
                LOGGER.finest("close connection directly (no connection in session)");
                if (!conn.isClosed()) {
                    conn.close();
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE,"Error on releaseConnection", ex);
            return false;
        }
        return true;
    }

    /**
     * Close for transactional connections
     */
    private boolean closeConnection(Connection conn) {
        if (conn == null) {
            return false;
        }
        try {
            conn.setAutoCommit(true);
            conn.close();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE,"Error on closeConnection", ex);
            return false;
        }
        return true;
    }

    public Connection getTransactionConnection() throws NoConnectionAvailableException, SQLException {
        Connection conn = getNewConnection(defaultPoolName);
        if (conn == null) {
            throw new NoConnectionAvailableException("Couldn´t get an available connection");
        }
        conn.setAutoCommit(false);
        return conn;
    }

    public void releaseCommitConnection(Connection conn) throws SQLException {
        if (conn == null) {
            return;
        }
        conn.commit();
        closeConnection(conn);
    }

    public void releaseRollbackConnection(Connection conn) throws SQLException {
        if (conn == null) {
            return;
        }
        // prevent extra exception if the connection is already closed
        // especially needed here because rollback occurs in case of
        // application exceptions also. If the conn.isClosed and a rollback
        // is done then the real app exception is hidden.
        if (conn.isClosed()) {
            return;
        }
        conn.rollback();
        closeConnection(conn);
    }

    public PreparedStatement getPreparedStatement(String SQLPreparedStatement) throws Exception {
        return getPreparedStatement(defaultPoolName, SQLPreparedStatement);
    }

    public PreparedStatement getPreparedStatement(String poolName, String SQLPreparedStatement)
            throws Exception {
        if (poolName == null || poolName.equals("")) {
            throw new PoolNotFoundException("Can't get the pool. No pool name specified");
        }
        LOGGER.finest("connection requested");
        Connection conn = getConnection(poolName);
        LOGGER.finest("connection established");
        return getPreparedStatement(conn, SQLPreparedStatement);
    }

    public PreparedStatement getPreparedStatement(Connection conn, String SQLPreparedStatement)
            throws SQLException {
        if (conn == null || SQLPreparedStatement == null || SQLPreparedStatement.equals("")) {
            return null;
        }
        PreparedStatement ps = null;
        try {
            LOGGER.finest("preparedStatement requested");
            ps = conn.prepareStatement(SQLPreparedStatement, ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            LOGGER.finest("preparedStatement received");
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

    /**
     * Returns the actual status of the dynamic pool.
     */
    public String getStatus() {
        StringBuffer strResultado = new StringBuffer();
        strResultado.append("Not implemented yet");
        return strResultado.toString();
    }// End getStatus()
}
