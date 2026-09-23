package com.gp.grocerypos.database;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton JDBC connection pool for the GroceryPOS application.
 * <p>
 * Maintains a pool of {@link Connection} objects backed by MySQL Connector/J.
 * Connections are checked out via {@link #getConnection()} and returned via
 * {@link #releaseConnection(Connection)}.
 * <p>
 * The pool grows on demand up to {@code db.pool.maxPoolSize} and shrinks
 * back toward {@code db.pool.minIdle} when connections are returned and
 * the pool is above the idle threshold.
 * <p>
 * <b>Usage pattern in DAO classes:</b>
 * <pre>{@code
 *   Connection conn = DatabaseConnection.getInstance().getConnection();
 *   try {
 *       // ... use conn ...
 *   } finally {
 *       DatabaseConnection.getInstance().releaseConnection(conn);
 *   }
 * }</pre>
 * <b>Transaction pattern (service layer):</b>
 * <pre>{@code
 *   Connection conn = DatabaseConnection.getInstance().getConnection();
 *   try {
 *       conn.setAutoCommit(false);
 *       // ... multiple DAO calls passing conn ...
 *       conn.commit();
 *   } catch (Exception e) {
 *       conn.rollback();
 *       throw e;
 *   } finally {
 *       conn.setAutoCommit(true);
 *       DatabaseConnection.getInstance().releaseConnection(conn);
 *   }
 * }</pre>
 */
public final class DatabaseConnection {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

    // Singleton
    private static volatile DatabaseConnection instance;

    private final DatabaseConfig config;

    // Pool state
    private final Deque<Connection> pool;
    private final AtomicInteger     totalConnections = new AtomicInteger(0);
    private final Object            poolLock         = new Object();

    // -------------------------------------------------------------------------
    // Singleton accessor
    // -------------------------------------------------------------------------

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private DatabaseConnection() {
        this.config = AppConfig.getInstance().getDatabaseConfig();
        this.pool   = new ArrayDeque<>();

        loadDriver();
        initializePool();
    }

    // -------------------------------------------------------------------------
    // Driver loading
    // -------------------------------------------------------------------------

    private void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            log.debug("MySQL JDBC driver loaded.");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "MySQL Connector/J driver not found on classpath. " +
                "Ensure mysql-connector-j is listed in pom.xml.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Pool initialisation — creates minIdle connections at startup
    // -------------------------------------------------------------------------

    private void initializePool() {
        int minIdle = config.getMinIdle();
        log.info("Initialising connection pool (minIdle={}, maxSize={}) for {}",
                minIdle, config.getMaxPoolSize(), config.buildJdbcUrl());
        for (int i = 0; i < minIdle; i++) {
            try {
                pool.add(createConnection());
            } catch (SQLException e) {
                // Non-fatal at startup — pool will try again on first getConnection()
                log.warn("Could not pre-create connection #{} during pool init: {}", i + 1, e.getMessage());
            }
        }
        log.info("Connection pool ready. Pre-created {} connection(s).", pool.size());
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Borrows a connection from the pool.
     * If the pool is empty and the maximum pool size has not been reached, a new
     * connection is created. Waits up to {@code connectionTimeout} ms if the pool
     * is exhausted.
     *
     * @return a valid, open {@link Connection}
     * @throws SQLException if a connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        synchronized (poolLock) {
            long deadline = System.currentTimeMillis() + config.getConnectionTimeout();

            while (true) {
                // Try to get an idle connection from the pool
                while (!pool.isEmpty()) {
                    Connection conn = pool.poll();
                    if (isConnectionValid(conn)) {
                        log.trace("Connection borrowed from pool. Pool size now: {}", pool.size());
                        return conn;
                    } else {
                        // Stale — discard and decrement counter
                        closeQuietly(conn);
                        totalConnections.decrementAndGet();
                        log.debug("Discarded stale connection from pool.");
                    }
                }

                // Pool is empty — create a new connection if under the limit
                if (totalConnections.get() < config.getMaxPoolSize()) {
                    Connection conn = createConnection();
                    log.trace("New connection created. Total: {}", totalConnections.get());
                    return conn;
                }

                // Pool exhausted — wait for a connection to be returned
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new SQLException(
                        "Connection pool exhausted. Could not obtain a connection within " +
                        config.getConnectionTimeout() + " ms. " +
                        "Consider increasing db.pool.maxPoolSize.");
                }
                try {
                    log.debug("Pool exhausted. Waiting up to {}ms for a connection.", remaining);
                    poolLock.wait(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for a database connection.", e);
                }
            }
        }
    }

    /**
     * Returns a connection to the pool.
     * <p>
     * If the pool already holds more than {@code minIdle} idle connections, the
     * returned connection is closed instead of being pooled, to avoid hoarding.
     * Always ensure autoCommit is reset to {@code true} before returning.
     *
     * @param connection the connection to return; ignored if {@code null}
     */
    public void releaseConnection(Connection connection) {
        if (connection == null) return;

        synchronized (poolLock) {
            try {
                // Always reset auto-commit so the next caller gets a clean connection
                if (!connection.getAutoCommit()) {
                    connection.setAutoCommit(true);
                }

                if (pool.size() < config.getMaxPoolSize() && isConnectionValid(connection)) {
                    pool.offer(connection);
                    log.trace("Connection returned to pool. Pool size: {}", pool.size());
                    poolLock.notifyAll(); // wake up any threads waiting for a connection
                } else {
                    closeQuietly(connection);
                    totalConnections.decrementAndGet();
                    log.trace("Connection closed (pool full or connection invalid). Total: {}",
                            totalConnections.get());
                }
            } catch (SQLException e) {
                log.warn("Error resetting connection auto-commit on release: {}", e.getMessage());
                closeQuietly(connection);
                totalConnections.decrementAndGet();
            }
        }
    }

    /**
     * Tests the database connection by borrowing and immediately returning one.
     *
     * @return {@code true} if the connection succeeds, {@code false} otherwise
     */
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            releaseConnection(conn);
            log.info("Database connection test successful: {}", config);
            return true;
        } catch (SQLException e) {
            log.error("Database connection test FAILED: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Closes all pooled connections and shuts down the pool.
     * Should be called on application exit.
     */
    public void shutdown() {
        synchronized (poolLock) {
            log.info("Shutting down connection pool. Closing {} idle connection(s).", pool.size());
            Connection conn;
            while ((conn = pool.poll()) != null) {
                closeQuietly(conn);
                totalConnections.decrementAndGet();
            }
            log.info("Connection pool shut down. Remaining tracked connections: {}",
                    totalConnections.get());
        }
    }

    /**
     * Returns the number of connections currently idle in the pool.
     */
    public int getIdleCount() {
        synchronized (poolLock) {
            return pool.size();
        }
    }

    /**
     * Returns the total number of connections created (idle + checked-out).
     */
    public int getTotalConnections() {
        return totalConnections.get();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Connection createConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(
            config.buildJdbcUrl(),
            config.getUsername(),
            config.getPassword()
        );
        totalConnections.incrementAndGet();
        return conn;
    }

    private boolean isConnectionValid(Connection conn) {
        try {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn == null) return;
        try {
            conn.close();
        } catch (SQLException e) {
            log.warn("Failed to close connection quietly: {}", e.getMessage());
        }
    }
}
