package com.gp.grocerypos.dao;

import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.model.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SupplierDAOImpl implements SupplierDAO {

    private static final Logger log = LoggerFactory.getLogger(SupplierDAOImpl.class);

    private static final String COLS =
        "id, name, contact_person, phone, email, address, notes, status, created_at, updated_at";

    private static final String FIND_BY_ID =
        "SELECT " + COLS + " FROM suppliers WHERE id = ?";

    private static final String FIND_ALL =
        "SELECT " + COLS + " FROM suppliers ORDER BY name";

    private static final String FIND_ALL_ACTIVE =
        "SELECT " + COLS + " FROM suppliers WHERE status = 'ACTIVE' ORDER BY name";

    private static final String SEARCH =
        "SELECT " + COLS + " FROM suppliers " +
        "WHERE (LOWER(name) LIKE ? OR LOWER(contact_person) LIKE ?) " +
        "ORDER BY name";

    private static final String INSERT =
        "INSERT INTO suppliers (name, contact_person, phone, email, address, notes, status) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE =
        "UPDATE suppliers SET name=?, contact_person=?, phone=?, email=?, address=?, " +
        "notes=?, status=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";

    private static final String SET_STATUS =
        "UPDATE suppliers SET status=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";

    private static final String DELETE =
        "DELETE FROM suppliers WHERE id=?";

    private static final String NAME_EXISTS =
        "SELECT COUNT(*) FROM suppliers WHERE LOWER(name) = LOWER(?)";

    private static final String NAME_EXISTS_EXCL =
        "SELECT COUNT(*) FROM suppliers WHERE LOWER(name) = LOWER(?) AND id <> ?";

    // -------------------------------------------------------------------------

    @Override
    public Optional<Supplier> findById(int id) {
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
            throw new DatabaseException("findById failed for supplier id=" + id, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public List<Supplier> findAll() {
        return query(FIND_ALL, null);
    }

    @Override
    public List<Supplier> findAllActive() {
        return query(FIND_ALL_ACTIVE, null);
    }

    @Override
    public List<Supplier> search(String keyword) {
        Connection conn = null;
        List<Supplier> list = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SEARCH)) {
                String like = "%" + keyword.toLowerCase() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Supplier search failed for keyword=" + keyword, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    private List<Supplier> query(String sql, Object ignored) {
        Connection conn = null;
        List<Supplier> list = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Supplier list query failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public int insert(Supplier s) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, s.getName());
                ps.setString(2, s.getContactPerson());
                ps.setString(3, s.getPhone());
                ps.setString(4, s.getEmail());
                ps.setString(5, s.getAddress());
                ps.setString(6, s.getNotes());
                ps.setString(7, s.getStatus() != null ? s.getStatus().name() : Supplier.Status.ACTIVE.name());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        s.setId(id);
                        log.info("Inserted supplier '{}' id={}", s.getName(), id);
                        return id;
                    }
                }
            }
            throw new DatabaseException("Insert supplier returned no generated key");
        } catch (SQLException e) {
            throw new DatabaseException("Insert supplier failed: " + s.getName(), e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void update(Supplier s) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
                ps.setString(1, s.getName());
                ps.setString(2, s.getContactPerson());
                ps.setString(3, s.getPhone());
                ps.setString(4, s.getEmail());
                ps.setString(5, s.getAddress());
                ps.setString(6, s.getNotes());
                ps.setString(7, s.getStatus().name());
                ps.setInt(8, s.getId());
                ps.executeUpdate();
                log.debug("Updated supplier id={}", s.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Update supplier failed id=" + s.getId(), e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void activate(int id) { setStatus(id, "ACTIVE"); }

    @Override
    public void deactivate(int id) { setStatus(id, "INACTIVE"); }

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
            throw new DatabaseException("Set supplier status failed id=" + id, e);
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
                log.info("Deleted supplier id={}", id);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Delete supplier failed id=" + id, e);
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
            throw new DatabaseException("nameExists check failed for supplier", e);
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
            throw new DatabaseException("nameExistsExcluding check failed for supplier", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private Supplier map(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getInt("id"));
        s.setName(rs.getString("name"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        s.setNotes(rs.getString("notes"));
        s.setStatus(Supplier.Status.valueOf(rs.getString("status")));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) s.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) s.setUpdatedAt(ua.toLocalDateTime());
        return s;
    }
}
