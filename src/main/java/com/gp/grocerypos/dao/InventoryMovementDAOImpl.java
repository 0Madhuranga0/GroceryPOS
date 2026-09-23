package com.gp.grocerypos.dao;

import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.model.InventoryMovement;
import com.gp.grocerypos.model.Product;
import com.gp.grocerypos.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InventoryMovementDAOImpl implements InventoryMovementDAO {

    private static final Logger log = LoggerFactory.getLogger(InventoryMovementDAOImpl.class);

    // Base SELECT — joins product name and user full_name for display
    private static final String SELECT_BASE =
        "SELECT im.id, im.product_id, im.movement_type, im.quantity, " +
        "       im.previous_stock, im.new_stock, im.reference_id, im.reference_type, " +
        "       im.reason, im.user_id, im.created_at, " +
        "       p.name  AS product_name, " +
        "       p.barcode AS product_barcode, " +
        "       u.full_name AS user_full_name " +
        "FROM   inventory_movements im " +
        "JOIN   products p ON p.id = im.product_id " +
        "JOIN   users    u ON u.id = im.user_id ";

    private static final String INSERT =
        "INSERT INTO inventory_movements " +
        "(product_id, movement_type, quantity, previous_stock, new_stock, " +
        " reference_id, reference_type, reason, user_id) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_BY_PRODUCT =
        SELECT_BASE + "WHERE im.product_id = ? ORDER BY im.created_at DESC";

    private static final String FIND_BY_PRODUCT_DATE =
        SELECT_BASE +
        "WHERE im.product_id = ? " +
        "AND DATE(im.created_at) >= ? AND DATE(im.created_at) <= ? " +
        "ORDER BY im.created_at DESC";

    private static final String FIND_BY_TYPE =
        SELECT_BASE + "WHERE im.movement_type = ? ORDER BY im.created_at DESC";

    private static final String FIND_BY_REFERENCE =
        SELECT_BASE +
        "WHERE im.reference_id = ? AND im.reference_type = ? " +
        "ORDER BY im.created_at DESC";

    private static final String FIND_BY_USER =
        SELECT_BASE + "WHERE im.user_id = ? ORDER BY im.created_at DESC";

    private static final String FIND_BY_DATE_RANGE =
        SELECT_BASE +
        "WHERE DATE(im.created_at) >= ? AND DATE(im.created_at) <= ? " +
        "ORDER BY im.created_at DESC";

    private static final String FIND_RECENT =
        SELECT_BASE + "ORDER BY im.created_at DESC LIMIT ?";

    // -------------------------------------------------------------------------

    @Override
    public int insert(InventoryMovement m) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, m.getProductId());
                ps.setString(2, m.getMovementType().name());
                ps.setBigDecimal(3, m.getQuantity());
                ps.setBigDecimal(4, m.getPreviousStock());
                ps.setBigDecimal(5, m.getNewStock());

                if (m.getReferenceId() != null) {
                    ps.setInt(6, m.getReferenceId());
                } else {
                    ps.setNull(6, Types.INTEGER);
                }
                ps.setString(7, m.getReferenceType());
                ps.setString(8, m.getReason());
                ps.setInt(9, m.getUserId());

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        m.setId(id);
                        log.debug("Recorded inventory movement id={} type={} productId={}",
                                id, m.getMovementType(), m.getProductId());
                        return id;
                    }
                }
            }
            throw new DatabaseException("Insert inventory movement returned no generated key");
        } catch (SQLException e) {
            throw new DatabaseException("Insert inventory movement failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public List<InventoryMovement> findByProduct(int productId) {
        return query(FIND_BY_PRODUCT, ps -> ps.setInt(1, productId));
    }

    @Override
    public List<InventoryMovement> findByProductAndDateRange(
            int productId, LocalDate from, LocalDate to) {
        return query(FIND_BY_PRODUCT_DATE, ps -> {
            ps.setInt(1, productId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
        });
    }

    @Override
    public List<InventoryMovement> findByType(InventoryMovement.MovementType type) {
        return query(FIND_BY_TYPE, ps -> ps.setString(1, type.name()));
    }

    @Override
    public List<InventoryMovement> findByReference(int referenceId, String referenceType) {
        return query(FIND_BY_REFERENCE, ps -> {
            ps.setInt(1, referenceId);
            ps.setString(2, referenceType);
        });
    }

    @Override
    public List<InventoryMovement> findByUser(int userId) {
        return query(FIND_BY_USER, ps -> ps.setInt(1, userId));
    }

    @Override
    public List<InventoryMovement> findByDateRange(LocalDate from, LocalDate to) {
        return query(FIND_BY_DATE_RANGE, ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
        });
    }

    @Override
    public List<InventoryMovement> findRecent(int limit) {
        return query(FIND_RECENT, ps -> ps.setInt(1, limit));
    }

    // ── Internal query helper ─────────────────────────────────────────────────

    private List<InventoryMovement> query(String sql, StatementSetter setter) {
        Connection conn = null;
        List<InventoryMovement> list = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (setter != null) setter.set(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("InventoryMovement query failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    private InventoryMovement map(ResultSet rs) throws SQLException {
        InventoryMovement m = new InventoryMovement();
        m.setId(rs.getInt("id"));

        // Product (lightweight — id, name, barcode only)
        Product p = new Product();
        p.setId(rs.getInt("product_id"));
        p.setName(rs.getString("product_name"));
        p.setBarcode(rs.getString("product_barcode"));
        m.setProduct(p);

        // Movement type
        try {
            m.setMovementType(InventoryMovement.MovementType.valueOf(rs.getString("movement_type")));
        } catch (IllegalArgumentException ex) {
            m.setMovementType(InventoryMovement.MovementType.ADJUSTMENT);
        }

        m.setQuantity(rs.getBigDecimal("quantity"));
        m.setPreviousStock(rs.getBigDecimal("previous_stock"));
        m.setNewStock(rs.getBigDecimal("new_stock"));

        int refId = rs.getInt("reference_id");
        if (!rs.wasNull()) m.setReferenceId(refId);
        m.setReferenceType(rs.getString("reference_type"));
        m.setReason(rs.getString("reason"));

        // User (lightweight — id, full_name only)
        User u = new User();
        u.setId(rs.getInt("user_id"));
        u.setFullName(rs.getString("user_full_name"));
        m.setUser(u);

        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) m.setCreatedAt(ca.toLocalDateTime());

        return m;
    }

    @FunctionalInterface
    private interface StatementSetter {
        void set(PreparedStatement ps) throws SQLException;
    }
}
