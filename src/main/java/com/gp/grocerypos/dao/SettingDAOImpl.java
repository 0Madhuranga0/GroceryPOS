package com.gp.grocerypos.dao;

import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.model.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SettingDAOImpl implements SettingDAO {

    private static final Logger log = LoggerFactory.getLogger(SettingDAOImpl.class);

    private static final String FIND_BY_KEY =
        "SELECT id, setting_key, setting_value, setting_group, description " +
        "FROM settings WHERE setting_key = ?";

    private static final String FIND_ALL =
        "SELECT id, setting_key, setting_value, setting_group, description " +
        "FROM settings ORDER BY setting_group, setting_key";

    private static final String FIND_BY_GROUP =
        "SELECT id, setting_key, setting_value, setting_group, description " +
        "FROM settings WHERE setting_group = ? ORDER BY setting_key";

    private static final String SET_VALUE =
        "UPDATE settings SET setting_value = ? WHERE setting_key = ?";

    private static final String UPSERT =
        "INSERT INTO settings (setting_key, setting_value, setting_group, description) " +
        "VALUES (?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";

    /**
     * Atomic increment using a single UPDATE + SELECT.
     * Wraps in a transaction to prevent concurrent counter collisions.
     */
    private static final String INCREMENT =
        "UPDATE settings SET setting_value = CAST(CAST(setting_value AS UNSIGNED) + 1 AS CHAR) " +
        "WHERE setting_key = ?";

    private static final String GET_VALUE =
        "SELECT setting_value FROM settings WHERE setting_key = ?";

    // -------------------------------------------------------------------------

    @Override
    public Optional<Setting> findByKey(String key) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_KEY)) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("findByKey failed for setting key=" + key, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public String getValue(String key, String defaultValue) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(GET_VALUE)) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String val = rs.getString(1);
                        return val != null ? val : defaultValue;
                    }
                }
            }
            return defaultValue;
        } catch (SQLException e) {
            log.warn("getValue failed for key='{}', returning default: {}", key, e.getMessage());
            return defaultValue;
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public List<Setting> findAll() {
        Connection conn = null;
        List<Setting> list = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_ALL);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("findAll settings failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public List<Setting> findByGroup(String group) {
        Connection conn = null;
        List<Setting> list = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_GROUP)) {
                ps.setString(1, group);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("findByGroup settings failed for group=" + group, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void setValue(String key, String value) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SET_VALUE)) {
                ps.setString(1, value);
                ps.setString(2, key);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    throw new DatabaseException("Setting key not found: " + key);
                }
                log.debug("Setting '{}' updated to '{}'", key, value);
            }
        } catch (SQLException e) {
            throw new DatabaseException("setValue failed for key=" + key, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void upsert(Setting setting) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPSERT)) {
                ps.setString(1, setting.getKey());
                ps.setString(2, setting.getValue());
                ps.setString(3, setting.getGroup() != null ? setting.getGroup() : "GENERAL");
                ps.setString(4, setting.getDescription());
                ps.executeUpdate();
                log.debug("Upserted setting '{}'", setting.getKey());
            }
        } catch (SQLException e) {
            throw new DatabaseException("upsert setting failed for key=" + setting.getKey(), e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public int incrementAndGet(String key) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                // Lock the row and increment atomically
                try (PreparedStatement ps = conn.prepareStatement(INCREMENT)) {
                    ps.setString(1, key);
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        conn.rollback();
                        throw new DatabaseException("Counter key not found in settings: " + key);
                    }
                }

                // Read back the new value within the same transaction
                int newValue = 0;
                try (PreparedStatement ps = conn.prepareStatement(GET_VALUE)) {
                    ps.setString(1, key);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            newValue = Integer.parseInt(rs.getString(1).trim());
                        }
                    }
                }

                conn.commit();
                log.debug("Counter '{}' incremented to {}", key, newValue);
                return newValue;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (DatabaseException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("incrementAndGet failed for key=" + key, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private Setting map(ResultSet rs) throws SQLException {
        Setting s = new Setting();
        s.setId(rs.getInt("id"));
        s.setKey(rs.getString("setting_key"));
        s.setValue(rs.getString("setting_value"));
        s.setGroup(rs.getString("setting_group"));
        s.setDescription(rs.getString("description"));
        return s;
    }
}
