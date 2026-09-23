package com.gp.grocerypos.dao;

import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PurchaseDAOImpl implements PurchaseDAO {

    private static final Logger log = LoggerFactory.getLogger(PurchaseDAOImpl.class);

    // Base SELECT — joins supplier and user
    private static final String SELECT_BASE =
        "SELECT p.id, p.purchase_number, p.supplier_invoice, " +
        "       p.purchase_date, p.total_amount, p.status, p.notes, p.created_at, " +
        "       s.id AS sup_id, s.name AS sup_name, " +
        "       u.id AS usr_id, u.full_name AS usr_name " +
        "FROM   purchases p " +
        "JOIN   suppliers s ON s.id = p.supplier_id " +
        "JOIN   users     u ON u.id = p.user_id ";

    private static final String FIND_BY_ID =
        SELECT_BASE + "WHERE p.id = ?";

    private static final String FIND_BY_NUMBER =
        SELECT_BASE + "WHERE p.purchase_number = ?";

    private static final String FIND_ALL =
        SELECT_BASE + "ORDER BY p.purchase_date DESC";

    private static final String FIND_BY_SUPPLIER =
        SELECT_BASE + "WHERE p.supplier_id = ? ORDER BY p.purchase_date DESC";

    private static final String FIND_BY_DATE =
        SELECT_BASE +
        "WHERE DATE(p.purchase_date) >= ? AND DATE(p.purchase_date) <= ? " +
        "ORDER BY p.purchase_date DESC";

    private static final String INSERT_HEADER =
        "INSERT INTO purchases " +
        "(purchase_number, supplier_id, supplier_invoice, user_id, " +
        " purchase_date, total_amount, status, notes) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ITEM =
        "INSERT INTO purchase_items " +
        "(purchase_id, product_id, quantity, unit_cost, total_cost) " +
        "VALUES (?, ?, ?, ?, ?)";

    private static final String FIND_ITEMS =
        "SELECT pi.id, pi.purchase_id, pi.quantity, pi.unit_cost, pi.total_cost, " +
        "       pr.id AS prod_id, pr.name AS prod_name, pr.barcode, pr.unit " +
        "FROM   purchase_items pi " +
        "JOIN   products pr ON pr.id = pi.product_id " +
        "WHERE  pi.purchase_id = ? " +
        "ORDER BY pi.id";

    private static final String CANCEL =
        "UPDATE purchases SET status = 'CANCELLED' WHERE id = ?";

    private static final String COUNT =
        "SELECT COUNT(*) FROM purchases";

    // -------------------------------------------------------------------------

    @Override
    public Optional<Purchase> findById(int id) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Purchase p = mapHeader(rs);
                        p.setItems(findItemsByPurchaseId(p.getId()));
                        return Optional.of(p);
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("findById failed for purchase id=" + id, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public Optional<Purchase> findByPurchaseNumber(String number) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_NUMBER)) {
                ps.setString(1, number);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Purchase p = mapHeader(rs);
                        p.setItems(findItemsByPurchaseId(p.getId()));
                        return Optional.of(p);
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("findByPurchaseNumber failed: " + number, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public List<Purchase> findAll() {
        return queryHeaders(FIND_ALL, null);
    }

    @Override
    public List<Purchase> findBySupplier(int supplierId) {
        return queryHeaders(FIND_BY_SUPPLIER, ps -> ps.setInt(1, supplierId));
    }

    @Override
    public List<Purchase> findByDateRange(LocalDate from, LocalDate to) {
        return queryHeaders(FIND_BY_DATE, ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
        });
    }

    private List<Purchase> queryHeaders(String sql, StatementSetter setter) {
        Connection conn = null;
        List<Purchase> list = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (setter != null) setter.set(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapHeader(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Purchase list query failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── Transactional insert (uses caller's connection) ────────────────────────

    @Override
    public int insertHeader(Purchase purchase, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                INSERT_HEADER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, purchase.getPurchaseNumber());
            ps.setInt(2, purchase.getSupplierId());
            ps.setString(3, purchase.getSupplierInvoice());
            ps.setInt(4, purchase.getUserId());
            ps.setTimestamp(5, purchase.getPurchaseDate() != null
                ? Timestamp.valueOf(purchase.getPurchaseDate()) : null);
            ps.setBigDecimal(6, purchase.getTotalAmount());
            ps.setString(7, purchase.getStatus() != null
                ? purchase.getStatus().name() : Purchase.Status.COMPLETED.name());
            ps.setString(8, purchase.getNotes());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    purchase.setId(id);
                    log.info("Inserted purchase header '{}' id={}", purchase.getPurchaseNumber(), id);
                    return id;
                }
            }
            throw new DatabaseException("Insert purchase returned no generated key");
        } catch (SQLException e) {
            throw new DatabaseException("insertHeader failed for purchase: "
                    + purchase.getPurchaseNumber(), e);
        }
    }

    @Override
    public void insertItems(List<PurchaseItem> items, int purchaseId, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_ITEM)) {
            for (PurchaseItem item : items) {
                ps.setInt(1, purchaseId);
                ps.setInt(2, item.getProductId());
                ps.setBigDecimal(3, item.getQuantity());
                ps.setBigDecimal(4, item.getUnitCost());
                ps.setBigDecimal(5, item.getTotalCost());
                ps.addBatch();
            }
            ps.executeBatch();
            log.debug("Inserted {} purchase items for purchaseId={}", items.size(), purchaseId);
        } catch (SQLException e) {
            throw new DatabaseException("insertItems failed for purchaseId=" + purchaseId, e);
        }
    }

    // ── Item queries (uses own connection) ────────────────────────────────────

    @Override
    public List<PurchaseItem> findItemsByPurchaseId(int purchaseId) {
        Connection conn = null;
        List<PurchaseItem> items = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_ITEMS)) {
                ps.setInt(1, purchaseId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) items.add(mapItem(rs));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new DatabaseException("findItemsByPurchaseId failed id=" + purchaseId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void cancel(int purchaseId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(CANCEL)) {
                ps.setInt(1, purchaseId);
                ps.executeUpdate();
                log.info("Purchase cancelled id={}", purchaseId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("cancel failed for purchaseId=" + purchaseId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public int count() {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(COUNT);
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("count purchases failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── Row mappers ───────────────────────────────────────────────────────────

    private Purchase mapHeader(ResultSet rs) throws SQLException {
        Purchase p = new Purchase();
        p.setId(rs.getInt("id"));
        p.setPurchaseNumber(rs.getString("purchase_number"));
        p.setSupplierInvoice(rs.getString("supplier_invoice"));
        p.setTotalAmount(rs.getBigDecimal("total_amount"));

        String status = rs.getString("status");
        p.setStatus(status != null ? Purchase.Status.valueOf(status) : Purchase.Status.COMPLETED);

        p.setNotes(rs.getString("notes"));

        Timestamp pd = rs.getTimestamp("purchase_date");
        if (pd != null) p.setPurchaseDate(pd.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(ca.toLocalDateTime());

        Supplier sup = new Supplier();
        sup.setId(rs.getInt("sup_id"));
        sup.setName(rs.getString("sup_name"));
        p.setSupplier(sup);

        User usr = new User();
        usr.setId(rs.getInt("usr_id"));
        usr.setFullName(rs.getString("usr_name"));
        p.setUser(usr);

        return p;
    }

    private PurchaseItem mapItem(ResultSet rs) throws SQLException {
        PurchaseItem item = new PurchaseItem();
        item.setId(rs.getInt("id"));
        item.setPurchaseId(rs.getInt("purchase_id"));
        item.setQuantity(rs.getBigDecimal("quantity"));
        item.setUnitCost(rs.getBigDecimal("unit_cost"));
        item.setTotalCost(rs.getBigDecimal("total_cost"));

        Product p = new Product();
        p.setId(rs.getInt("prod_id"));
        p.setName(rs.getString("prod_name"));
        p.setBarcode(rs.getString("barcode"));
        String unit = rs.getString("unit");
        if (unit != null) {
            try { p.setUnit(Product.Unit.valueOf(unit)); }
            catch (IllegalArgumentException ex) { p.setUnit(Product.Unit.PIECE); }
        }
        item.setProduct(p);

        return item;
    }

    @FunctionalInterface
    private interface StatementSetter {
        void set(PreparedStatement ps) throws SQLException;
    }
}
