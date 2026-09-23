package com.gp.grocerypos.dao;

import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDAOImpl implements CategoryDAO {

    private static final Logger log = LoggerFactory.getLogger(CategoryDAOImpl.class);

    private static final String FIND_BY_ID =
        "SELECT id, name, description, status, created_at, updated_at " +
        "FROM categories WHERE id = ?";

    private static final String FIND_BY_NAME =
        "SELECT id, name, description, status, created_at, updated_at " +
        "FROM categories WHERE LOWER(name) = LOWER(?)";

    private static final String FIND_ALL =
        "SELECT id, name, description, status, created_at, updated_at " +
        "FROM categories ORDER BY name";

    private static final String FIND_ALL_ACTIVE =
        "SELECT id, name, description, status, created_at, updated_at " +
        "FROM categories WHERE status = 'ACTIVE' ORDER BY name";

    private static final String INSERT =
        "INSERT INTO categories (name, description, status) VALUES (?, ?, ?)";

    private static final String UPDATE =
        "UPDATE categories SET name = ?, description = ?, status = ?, " +
        "updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    private static final String SET_STATUS =
        "UPDATE categories SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    private static final String DELETE =
        "DELETE FROM categories WHERE id = ?";

    private static final String NAME_EXISTS =
        "SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(?)";

    private static final String NAME_EXISTS_EXCL =
        "SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(?) AND id <> ?";

    // -------------------------------------------------------------------------

    @Override
    public Optional<Category> findById(int id) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("findById failed for category id=" + id, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public Optional<Category> findByName(String name) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_NAME)) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("findByName failed for category name=" + name, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public List<Category> findAll() {
        return query(FIND_ALL);
    }

    @Override
    public List<Category> findAllActive() {
        return query(FIND_ALL_ACTIVE);
    }

    private List<Category> query(String sql) {
        Connection conn = null;
        List<Category> list = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Category list query failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public int insert(Category category) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, category.getName());
                ps.setString(2, category.getDescription());
                ps.setString(3, category.getStatus() != null
                        ? category.getStatus().name() : Category.Status.ACTIVE.name());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        category.setId(id);
                        log.info("Inserted category '{}' id={}", category.getName(), id);
                        return id;
                    }
                }
            }
            throw new DatabaseException("Insert category returned no generated key");
        } catch (SQLException e) {
            throw new DatabaseException("Insert category failed: " + category.getName(), e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void update(Category category) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
                ps.setString(1, category.getName());
                ps.setString(2, category.getDescription());
                ps.setString(3, category.getStatus().name());
                ps.setInt(4, category.getId());
                ps.executeUpdate();
                log.debug("Updated category id={}", category.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Update category failed id=" + category.getId(), e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void activate(int id) {
        setStatus(id, "ACTIVE");
    }

    @Override
    public void deactivate(int id) {
        setStatus(id, "INACTIVE");
    }

    private void setStatus(int id, String status) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SET_STATUS)) {
                ps.setString(1, status);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Set category status failed id=" + id, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void delete(int id) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
                ps.setInt(1, id);
                ps.executeUpdate();
                log.info("Deleted category id={}", id);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Delete category failed id=" + id, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public boolean nameExists(String name) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(NAME_EXISTS)) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("nameExists check failed for category", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public boolean nameExistsExcluding(String name, int excludeId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(NAME_EXISTS_EXCL)) {
                ps.setString(1, name);
                ps.setInt(2, excludeId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("nameExistsExcluding check failed for category", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private Category map(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setStatus(Category.Status.valueOf(rs.getString("status")));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) c.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) c.setUpdatedAt(ua.toLocalDateTime());
        return c;
    }
}
