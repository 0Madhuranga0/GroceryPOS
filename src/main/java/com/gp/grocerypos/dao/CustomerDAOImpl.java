package com.gp.grocerypos.dao;

import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerDAOImpl implements CustomerDAO {

    private static final Logger log = LoggerFactory.getLogger(CustomerDAOImpl.class);

    private static final String COLS =
        "id, name, phone, email, address, loyalty_points, status, created_at, updated_at";

    private static final String FIND_BY_ID =
        "SELECT " + COLS + " FROM customers WHERE id = ?";

    private static final String FIND_BY_PHONE =
        "SELECT " + COLS + " FROM customers WHERE phone = ?";

    private static final String FIND_ALL =
        "SELECT " + COLS + " FROM customers ORDER BY name";

    private static final String FIND_ALL_ACTIVE =
        "SELECT " + COLS + " FROM customers WHERE status = 'ACTIVE' ORDER BY name";

    private static final String SEARCH =
        "SELECT " + COLS + " FROM customers " +
        "WHERE (LOWER(name) LIKE ? OR phone LIKE ?) " +
        "ORDER BY name";

    private static final String INSERT =
        "INSERT INTO customers (name, phone, email, address, loyalty_points, status) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE =
        "UPDATE customers SET name=?, phone=?, email=?, address=?, status=?, " +
        "updated_at=CURRENT_TIMESTAMP WHERE id=?";

    private static final String SET_STATUS =
        "UPDATE customers SET status=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";

    private static final String ADD_POINTS =
        "UPDATE customers SET loyalty_points = loyalty_points + ?, " +
        "updated_at=CURRENT_TIMESTAMP WHERE id=?";

    private static final String PHONE_EXISTS =
        "SELECT COUNT(*) FROM customers WHERE phone = ?";

    private static final String PHONE_EXISTS_EXCL =
        "SELECT COUNT(*) FROM customers WHERE phone = ? AND id <> ?";

    // -------------------------------------------------------------------------

    @Override
    public Optional<Customer> findById(int id) {
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
            throw new DatabaseException("findById failed for customer id=" + id, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public Optional<Customer> findByPhone(String phone) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_PHONE)) {
                ps.setString(1, phone);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("findByPhone failed for phone=" + phone, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public List<Customer> findAll() {
        return query(FIND_ALL);
    }

    @Override
    public List<Customer> findAllActive() {
        return query(FIND_ALL_ACTIVE);
    }

    private List<Customer> query(String sql) {
        Connection conn = null;
        List<Customer> list = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Customer list query failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public List<Customer> search(String keyword) {
        Connection conn = null;
        List<Customer> list = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SEARCH)) {
                String like = "%" + keyword.toLowerCase() + "%";
                ps.setString(1, like);
                ps.setString(2, "%" + keyword + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Customer search failed for keyword=" + keyword, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public int insert(Customer c) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, c.getName());
                ps.setString(2, c.getPhone());
                ps.setString(3, c.getEmail());
                ps.setString(4, c.getAddress());
                ps.setInt(5, c.getLoyaltyPoints());
                ps.setString(6, c.getStatus() != null ? c.getStatus().name() : Customer.Status.ACTIVE.name());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        c.setId(id);
                        log.info("Inserted customer '{}' id={}", c.getName(), id);
                        return id;
                    }
                }
            }
            throw new DatabaseException("Insert customer returned no generated key");
        } catch (SQLException e) {
            throw new DatabaseException("Insert customer failed: " + c.getName(), e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void update(Customer c) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
                ps.setString(1, c.getName());
                ps.setString(2, c.getPhone());
                ps.setString(3, c.getEmail());
                ps.setString(4, c.getAddress());
                ps.setString(5, c.getStatus().name());
                ps.setInt(6, c.getId());
                ps.executeUpdate();
                log.debug("Updated customer id={}", c.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Update customer failed id=" + c.getId(), e);
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
            throw new DatabaseException("Set customer status failed id=" + id, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void addLoyaltyPoints(int customerId, int points) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(ADD_POINTS)) {
                ps.setInt(1, points);
                ps.setInt(2, customerId);
                ps.executeUpdate();
                log.debug("Added {} loyalty points to customer id={}", points, customerId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("addLoyaltyPoints failed for customer id=" + customerId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public boolean phoneExists(String phone) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(PHONE_EXISTS)) {
                ps.setString(1, phone);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("phoneExists check failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public boolean phoneExistsExcluding(String phone, int excludeId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(PHONE_EXISTS_EXCL)) {
                ps.setString(1, phone);
                ps.setInt(2, excludeId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("phoneExistsExcluding check failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        c.setLoyaltyPoints(rs.getInt("loyalty_points"));
        c.setStatus(Customer.Status.valueOf(rs.getString("status")));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) c.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) c.setUpdatedAt(ua.toLocalDateTime());
        return c;
    }
}
