package com.gp.grocerypos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton that loads and exposes all application configuration from
 * {@code config/app.properties} on the classpath.
 * <p>
 * Usage:
 * <pre>
 *     AppConfig config = AppConfig.getInstance();
 *     String storeName = config.get("store.name");
 *     DatabaseConfig db = config.getDatabaseConfig();
 * </pre>
 * Configuration is loaded once at first access and cached for the
 * lifetime of the application.
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "config/app.properties";

    // Singleton
    private static volatile AppConfig instance;

    private final Properties props;
    private DatabaseConfig databaseConfig;

    // -------------------------------------------------------------------------
    // Singleton accessor
    // -------------------------------------------------------------------------

    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Constructor — private
    // -------------------------------------------------------------------------

    private AppConfig() {
        props = new Properties();
        loadProperties();
        buildDatabaseConfig();
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    private void loadProperties() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                    "Configuration file not found on classpath: " + CONFIG_FILE);
            }
            props.load(in);
            log.info("Application configuration loaded from {}", CONFIG_FILE);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to load application configuration: " + e.getMessage(), e);
        }
    }

    private void buildDatabaseConfig() {
        databaseConfig = DatabaseConfig.builder()
            .host(get("db.host", "localhost"))
            .port(getInt("db.port", 3306))
            .dbName(get("db.name", "grocery_pos"))
            .username(get("db.username", "root"))
            .password(get("db.password", ""))
            .timezone(get("db.timezone", "UTC"))
            .useSSL(getBoolean("db.useSSL", false))
            .allowPublicKeyRetrieval(getBoolean("db.allowPublicKeyRetrieval", true))
            .autoReconnect(getBoolean("db.autoReconnect", true))
            .minIdle(getInt("db.pool.minIdle", 2))
            .maxPoolSize(getInt("db.pool.maxPoolSize", 10))
            .connectionTimeout(getLong("db.pool.connectionTimeout", 30_000L))
            .idleTimeout(getLong("db.pool.idleTimeout", 600_000L))
            .maxLifetime(getLong("db.pool.maxLifetime", 1_800_000L))
            .build();

        log.debug("Database configuration built: {}", databaseConfig);
    }

    // -------------------------------------------------------------------------
    // Public accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the value for the given key, or throws if the key is missing.
     */
    public String get(String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required configuration key: " + key);
        }
        return value.trim();
    }

    /**
     * Returns the value for the given key, or {@code defaultValue} if absent.
     */
    public String get(String key, String defaultValue) {
        String value = props.getProperty(key);
        return (value != null) ? value.trim() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Config key '{}' has non-integer value '{}'. Using default {}.", key, value, defaultValue);
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Config key '{}' has non-long value '{}'. Using default {}.", key, value, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value.trim());
    }

    public double getDouble(String key, double defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Config key '{}' has non-double value '{}'. Using default {}.", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Returns the fully-constructed, immutable {@link DatabaseConfig}.
     */
    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    // Convenience shortcuts for frequently-used values
    public String getAppName()        { return get("app.name", "GroceryPOS"); }
    public String getAppVersion()     { return get("app.version", "1.0.0"); }
    public String getCurrencySymbol() { return get("app.currency.symbol", "Rs."); }
    public String getLogDir()         { return get("log.dir", "logs"); }
    public String getBackupDir()      { return get("backup.dir", "backups"); }
}
