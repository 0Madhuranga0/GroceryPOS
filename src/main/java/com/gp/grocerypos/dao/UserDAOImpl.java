package com.gp.grocerypos.dao;

import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.model.Permission;
import com.gp.grocerypos.model.Role;
import com.gp.grocerypos.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link UserDAO}.
 * <p>
 * All SQL uses {@link PreparedStatement} — no string concatenation with
 * user input. All exceptions are wrapped in {@link DatabaseException} so
 * callers never deal with raw {@link SQLException}.
 */
public class UserDAOImpl implements UserDAO {

    private static final Logger log = LoggerFactory.getLogger(UserDAOImpl.class);

    // -------------------------------------------------------------------------
    // SQL constants
    // -------------------------------------------------------------------------

    private static final String SELECT_USER_BASE =
        "SELECT u.id, u.username, u.password_hash, u.full_name, u.email, u.phone, " +
        "       u.status, u.last_login, u.created_at, u.updated_at, " +
        "       r.id AS role_id, r.name AS role_name, r.description AS role_desc " +
        "FROM   users u " +
        "JOIN   roles r ON r.id = u.role_id ";

    private static final String FIND_BY_USERNAME =
        SELECT_USER_BASE + "WHERE u.username = ?";

    private static final String FIND_BY_ID =
        SELECT_USER_BASE + "WHERE u.id = ?";

    private static final String FIND_ALL =
        SELECT_USER_BASE + "ORDER BY u.full_name";

    private static final String FIND_ALL_ACTIVE =
        SELECT_USER_BASE + "WHERE u.status = 'ACTIVE' ORDER BY u.full_name";

    private static final String INSERT_USER =
        "INSERT INTO users (username, password_hash, full_name, email, phone, role_id, status) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_USER =
        "UPDATE users SET full_name = ?, email = ?, phone = ?, role_id = ?, status = ?, " +
        "updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    private static final String UPDATE_PASSWORD =
        "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    private static final String UPDATE_LAST_LOGIN =
        "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";

    private static final String DEACTIVATE_USER =
        "UPDATE users SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    private static final String ACTIVATE_USER =
        "UPDATE users SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    private static final String FIND_PERMISSIONS_BY_ROLE =
        "SELECT p.id, p.name, p.description, p.module " +
        "FROM   permissions p " +
        "JOIN   role_permissions rp ON rp.permission_id = p.id " +
        "WHERE  rp.role_id = ? " +
        "ORDER BY p.module, p.name";

    private static final String USERNAME_EXISTS =
        "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?)";

    // -------------------------------------------------------------------------
    // findByUsername
    // -------------------------------------------------------------------------

    @Override
    public Optional<User> findByUsername(String username) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_USERNAME)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            log.error("findByUsername failed for '{}': {}", username, e.getMessage(), e);
            throw new DatabaseException("Failed to find user by username: " + username, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Override
    public Optional<User> findById(int id) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            log.error("findById failed for id={}: {}", id, e.getMessage(), e);
            throw new DatabaseException("Failed to find user by id: " + id, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // findAll / findAllActive
    // -------------------------------------------------------------------------

    @Override
    public List<User> findAll() {
        return executeListQuery(FIND_ALL);
    }

    @Override
    public List<User> findAllActive() {
        return executeListQuery(FIND_ALL_ACTIVE);
    }

    private List<User> executeListQuery(String sql) {
        Connection conn = null;
        List<User> users = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
            return users;
        } catch (SQLException e) {
            log.error("User list query failed: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to load user list", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // insert
    // -------------------------------------------------------------------------

    @Override
    public int insert(User user) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getFullName());
                ps.setString(4, user.getEmail());
                ps.setString(5, user.getPhone());
                ps.setInt(6, user.getRole().getId());
                ps.setString(7, user.getStatus() != null
                        ? user.getStatus().name() : User.Status.ACTIVE.name());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        user.setId(id);
                        log.info("Inserted user '{}' with id={}", user.getUsername(), id);
                        return id;
                    }
                }
            }
            throw new DatabaseException("Insert user succeeded but no generated key returned");
        } catch (SQLException e) {
            log.error("Insert user failed for '{}': {}", user.getUsername(), e.getMessage(), e);
            throw new DatabaseException("Failed to insert user: " + user.getUsername(), e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Override
    public void update(User user) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_USER)) {
                ps.setString(1, user.getFullName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPhone());
                ps.setInt(4, user.getRole().getId());
                ps.setString(5, user.getStatus().name());
                ps.setInt(6, user.getId());
                ps.executeUpdate();
                log.debug("Updated user id={}", user.getId());
            }
        } catch (SQLException e) {
            log.error("Update user failed for id={}: {}", user.getId(), e.getMessage(), e);
            throw new DatabaseException("Failed to update user id=" + user.getId(), e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // updatePassword
    // -------------------------------------------------------------------------

    @Override
    public void updatePassword(int userId, String newPasswordHash) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_PASSWORD)) {
                ps.setString(1, newPasswordHash);
                ps.setInt(2, userId);
                ps.executeUpdate();
                log.info("Password updated for user id={}", userId);
            }
        } catch (SQLException e) {
            log.error("updatePassword failed for id={}: {}", userId, e.getMessage(), e);
            throw new DatabaseException("Failed to update password for user id=" + userId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // updateLastLogin
    // -------------------------------------------------------------------------

    @Override
    public void updateLastLogin(int userId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_LAST_LOGIN)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            // Non-fatal — log and continue; the login itself already succeeded
            log.warn("updateLastLogin failed for user id={}: {}", userId, e.getMessage());
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // deactivate / activate
    // -------------------------------------------------------------------------

    @Override
    public void deactivate(int userId) {
        executeStatusUpdate(DEACTIVATE_USER, userId, "deactivate");
    }

    @Override
    public void activate(int userId) {
        executeStatusUpdate(ACTIVATE_USER, userId, "activate");
    }

    private void executeStatusUpdate(String sql, int userId, String action) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
                log.info("User id={} status changed via action '{}'", userId, action);
            }
        } catch (SQLException e) {
            log.error("{} user failed for id={}: {}", action, userId, e.getMessage(), e);
            throw new DatabaseException("Failed to " + action + " user id=" + userId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // findPermissionsByRoleId
    // -------------------------------------------------------------------------

    @Override
    public List<Permission> findPermissionsByRoleId(int roleId) {
        Connection conn = null;
        List<Permission> permissions = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_PERMISSIONS_BY_ROLE)) {
                ps.setInt(1, roleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Permission p = new Permission(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("module")
                        );
                        permissions.add(p);
                    }
                }
            }
            log.debug("Loaded {} permissions for role id={}", permissions.size(), roleId);
            return permissions;
        } catch (SQLException e) {
            log.error("findPermissionsByRoleId failed for roleId={}: {}", roleId, e.getMessage(), e);
            throw new DatabaseException("Failed to load permissions for role id=" + roleId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // usernameExists
    // -------------------------------------------------------------------------

    @Override
    public boolean usernameExists(String username) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(USERNAME_EXISTS)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            log.error("usernameExists failed for '{}': {}", username, e.getMessage(), e);
            throw new DatabaseException("Failed to check username existence", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // Row mapping helper
    // -------------------------------------------------------------------------

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setStatus(User.Status.valueOf(rs.getString("status")));

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        Role role = new Role(
            rs.getInt("role_id"),
            rs.getString("role_name"),
            rs.getString("role_desc")
        );
        user.setRole(role);

        return user;
    }
}
