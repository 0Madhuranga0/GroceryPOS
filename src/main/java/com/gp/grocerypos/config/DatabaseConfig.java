package com.gp.grocerypos.config;

/**
 * Immutable value object that holds database connection parameters.
 * <p>
 * Populated by {@link AppConfig} from {@code app.properties}.
 * Passed to {@link com.gp.grocerypos.database.DatabaseConnection} at startup.
 * Never hard-code these values in application code.
 */
public final class DatabaseConfig {

    private final String host;
    private final int    port;
    private final String dbName;
    private final String username;
    private final String password;
    private final String timezone;
    private final boolean useSSL;
    private final boolean allowPublicKeyRetrieval;
    private final boolean autoReconnect;

    // Connection pool
    private final int    minIdle;
    private final int    maxPoolSize;
    private final long   connectionTimeout;
    private final long   idleTimeout;
    private final long   maxLifetime;

    private DatabaseConfig(Builder builder) {
        this.host                    = builder.host;
        this.port                    = builder.port;
        this.dbName                  = builder.dbName;
        this.username                = builder.username;
        this.password                = builder.password;
        this.timezone                = builder.timezone;
        this.useSSL                  = builder.useSSL;
        this.allowPublicKeyRetrieval = builder.allowPublicKeyRetrieval;
        this.autoReconnect           = builder.autoReconnect;
        this.minIdle                 = builder.minIdle;
        this.maxPoolSize             = builder.maxPoolSize;
        this.connectionTimeout       = builder.connectionTimeout;
        this.idleTimeout             = builder.idleTimeout;
        this.maxLifetime             = builder.maxLifetime;
    }

    // -------------------------------------------------------------------------
    // JDBC URL builder
    // -------------------------------------------------------------------------

    /**
     * Builds a JDBC connection URL from the stored parameters.
     *
     * @return a fully-formed JDBC URL string
     */
    public String buildJdbcUrl() {
        return String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=%b&allowPublicKeyRetrieval=%b" +
            "&autoReconnect=%b&serverTimezone=%s&characterEncoding=UTF-8",
            host, port, dbName,
            useSSL, allowPublicKeyRetrieval,
            autoReconnect, timezone
        );
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String  getHost()                    { return host; }
    public int     getPort()                    { return port; }
    public String  getDbName()                  { return dbName; }
    public String  getUsername()                { return username; }
    public String  getPassword()                { return password; }
    public String  getTimezone()                { return timezone; }
    public boolean isUseSSL()                   { return useSSL; }
    public boolean isAllowPublicKeyRetrieval()  { return allowPublicKeyRetrieval; }
    public boolean isAutoReconnect()            { return autoReconnect; }
    public int     getMinIdle()                 { return minIdle; }
    public int     getMaxPoolSize()             { return maxPoolSize; }
    public long    getConnectionTimeout()       { return connectionTimeout; }
    public long    getIdleTimeout()             { return idleTimeout; }
    public long    getMaxLifetime()             { return maxLifetime; }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String  host                    = "localhost";
        private int     port                    = 3306;
        private String  dbName                  = "grocery_pos";
        private String  username                = "root";
        private String  password                = "";
        private String  timezone                = "UTC";
        private boolean useSSL                  = false;
        private boolean allowPublicKeyRetrieval = true;
        private boolean autoReconnect           = true;
        private int     minIdle                 = 2;
        private int     maxPoolSize             = 10;
        private long    connectionTimeout       = 30_000L;
        private long    idleTimeout             = 600_000L;
        private long    maxLifetime             = 1_800_000L;

        public Builder host(String host)                               { this.host = host; return this; }
        public Builder port(int port)                                  { this.port = port; return this; }
        public Builder dbName(String dbName)                           { this.dbName = dbName; return this; }
        public Builder username(String username)                       { this.username = username; return this; }
        public Builder password(String password)                       { this.password = password; return this; }
        public Builder timezone(String timezone)                       { this.timezone = timezone; return this; }
        public Builder useSSL(boolean useSSL)                         { this.useSSL = useSSL; return this; }
        public Builder allowPublicKeyRetrieval(boolean val)           { this.allowPublicKeyRetrieval = val; return this; }
        public Builder autoReconnect(boolean autoReconnect)           { this.autoReconnect = autoReconnect; return this; }
        public Builder minIdle(int minIdle)                           { this.minIdle = minIdle; return this; }
        public Builder maxPoolSize(int maxPoolSize)                   { this.maxPoolSize = maxPoolSize; return this; }
        public Builder connectionTimeout(long connectionTimeout)      { this.connectionTimeout = connectionTimeout; return this; }
        public Builder idleTimeout(long idleTimeout)                  { this.idleTimeout = idleTimeout; return this; }
        public Builder maxLifetime(long maxLifetime)                  { this.maxLifetime = maxLifetime; return this; }

        public DatabaseConfig build() {
            if (host == null || host.isBlank())     throw new IllegalStateException("db.host must not be blank");
            if (dbName == null || dbName.isBlank()) throw new IllegalStateException("db.name must not be blank");
            if (username == null)                   throw new IllegalStateException("db.username must not be null");
            return new DatabaseConfig(this);
        }
    }

    @Override
    public String toString() {
        // Never include password in toString
        return String.format("DatabaseConfig{host='%s', port=%d, db='%s', user='%s'}",
                host, port, dbName, username);
    }
}
