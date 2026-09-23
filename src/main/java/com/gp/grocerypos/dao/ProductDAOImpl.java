package com.gp.grocerypos.dao;

import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.exception.StockException;
import com.gp.grocerypos.model.Category;
import com.gp.grocerypos.model.Product;
import com.gp.grocerypos.model.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAOImpl implements ProductDAO {

    private static final Logger log = LoggerFactory.getLogger(ProductDAOImpl.class);

    // ── Base SELECT with category and supplier joins ──────────────────────────
    private static final String SELECT_BASE =
        "SELECT p.id, p.barcode, p.name, p.description, p.brand, p.unit, " +
        "       p.purchase_price, p.selling_price, p.wholesale_price, " +
        "       p.current_stock, p.minimum_stock, p.expiry_date, " +
        "       p.status, p.created_at, p.updated_at, " +
        "       c.id AS cat_id, c.name AS cat_name, " +
        "       s.id AS sup_id, s.name AS sup_name " +
        "FROM   products p " +
        "LEFT JOIN categories c ON c.id = p.category_id " +
        "LEFT JOIN suppliers  s ON s.id = p.supplier_id ";

    private static final String FIND_BY_ID =
        SELECT_BASE + "WHERE p.id = ?";

    private static final String FIND_BY_BARCODE =
        SELECT_BASE + "WHERE p.barcode = ?";

    private static final String FIND_ALL =
        SELECT_BASE + "ORDER BY p.name";

    private static final String FIND_ALL_ACTIVE =
        SELECT_BASE + "WHERE p.status = 'ACTIVE' ORDER BY p.name";

    private static final String FIND_BY_CATEGORY =
        SELECT_BASE + "WHERE p.category_id = ? ORDER BY p.name";

    private static final String FIND_BY_SUPPLIER =
        SELECT_BASE + "WHERE p.supplier_id = ? ORDER BY p.name";

    private static final String SEARCH =
        SELECT_BASE +
        "WHERE p.status = 'ACTIVE' AND " +
        "(LOWER(p.name) LIKE ? OR p.barcode LIKE ? OR LOWER(p.brand) LIKE ?) " +
        "ORDER BY p.name";

    private static final String FIND_LOW_STOCK =
        SELECT_BASE +
        "WHERE p.status = 'ACTIVE' AND p.current_stock <= p.minimum_stock " +
        "ORDER BY p.current_stock";

    private static final String FIND_EXPIRING_SOON =
        SELECT_BASE +
        "WHERE p.status = 'ACTIVE' AND p.expiry_date IS NOT NULL " +
        "AND p.expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) " +
        "AND p.expiry_date >= CURDATE() " +
        "ORDER BY p.expiry_date";

    private static final String FIND_EXPIRED =
        SELECT_BASE +
        "WHERE p.status = 'ACTIVE' AND p.expiry_date IS NOT NULL " +
        "AND p.expiry_date < CURDATE() " +
        "ORDER BY p.expiry_date";

    private static final String INSERT =
        "INSERT INTO products (barcode, name, description, category_id, supplier_id, brand, unit, " +
        "purchase_price, selling_price, wholesale_price, current_stock, minimum_stock, " +
        "expiry_date, status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE =
        "UPDATE products SET barcode=?, name=?, description=?, category_id=?, supplier_id=?, " +
        "brand=?, unit=?, purchase_price=?, selling_price=?, wholesale_price=?, " +
        "minimum_stock=?, expiry_date=?, status=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";

    private static final String SET_STATUS =
        "UPDATE products SET status=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";

    /**
     * Atomic stock update — uses SQL expression so no race condition.
     * The WHERE clause with current_stock >= ABS(delta) prevents negative stock.
     */
    private static final String UPDATE_STOCK_SAFE =
        "UPDATE products SET current_stock = current_stock + ?, " +
        "updated_at=CURRENT_TIMESTAMP " +
        "WHERE id=? AND (current_stock + ?) >= 0";

    private static final String UPDATE_STOCK_ALLOW_NEG =
        "UPDATE products SET current_stock = current_stock + ?, " +
        "updated_at=CURRENT_TIMESTAMP WHERE id=?";

    private static final String SET_STOCK =
        "UPDATE products SET current_stock=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";

    private static final String GET_STOCK =
        "SELECT current_stock, name FROM products WHERE id=?";

    private static final String UPDATE_PURCHASE_PRICE =
        "UPDATE products SET purchase_price=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";

    private static final String BARCODE_EXISTS =
        "SELECT COUNT(*) FROM products WHERE barcode = ?";

    private static final String BARCODE_EXISTS_EXCL =
        "SELECT COUNT(*) FROM products WHERE barcode = ? AND id <> ?";

    private static final String COUNT_ACTIVE =
        "SELECT COUNT(*) FROM products WHERE status = 'ACTIVE'";

    // -------------------------------------------------------------------------
    // Lookups
    // -------------------------------------------------------------------------

    @Override
    public Optional<Product> findById(int id) {
        return queryOne(FIND_BY_ID, ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<Product> findByBarcode(String barcode) {
        return queryOne(FIND_BY_BARCODE, ps -> ps.setString(1, barcode));
    }

    private Optional<Product> queryOne(String sql, StatementSetter setter) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                setter.set(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Product query failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public List<Product> findAll() {
        return queryList(FIND_ALL, null);
    }

    @Override
    public List<Product> findAllActive() {
        return queryList(FIND_ALL_ACTIVE, null);
    }

    @Override
    public List<Product> findByCategory(int categoryId) {
        return queryList(FIND_BY_CATEGORY, ps -> ps.setInt(1, categoryId));
    }

    @Override
    public List<Product> findBySupplier(int supplierId) {
        return queryList(FIND_BY_SUPPLIER, ps -> ps.setInt(1, supplierId));
    }

    @Override
    public List<Product> search(String keyword) {
        Connection conn = null;
        List<Product> list = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SEARCH)) {
                String like = "%" + keyword.toLowerCase() + "%";
                ps.setString(1, like);
                ps.setString(2, "%" + keyword + "%");
                ps.setString(3, like);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Product search failed for keyword=" + keyword, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public List<Product> findLowStock() {
        return queryList(FIND_LOW_STOCK, null);
    }

    @Override
    public List<Product> findExpiringSoon(int warningDays) {
        return queryList(FIND_EXPIRING_SOON, ps -> ps.setInt(1, warningDays));
    }

    @Override
    public List<Product> findExpired() {
        return queryList(FIND_EXPIRED, null);
    }

    private List<Product> queryList(String sql, StatementSetter setter) {
        Connection conn = null;
        List<Product> list = new ArrayList<>();
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
            throw new DatabaseException("Product list query failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // Insert / Update
    // -------------------------------------------------------------------------

    @Override
    public int insert(Product p) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getBarcode());
                ps.setString(2, p.getName());
                ps.setString(3, p.getDescription());
                setNullableInt(ps, 4, p.getCategoryId());
                setNullableInt(ps, 5, p.getSupplierId());
                ps.setString(6, p.getBrand());
                ps.setString(7, p.getUnit() != null ? p.getUnit().name() : Product.Unit.PIECE.name());
                ps.setBigDecimal(8,  p.getPurchasePrice());
                ps.setBigDecimal(9,  p.getSellingPrice());
                ps.setBigDecimal(10, p.getWholesalePrice());
                ps.setBigDecimal(11, p.getCurrentStock());
                ps.setBigDecimal(12, p.getMinimumStock());
                if (p.getExpiryDate() != null) {
                    ps.setDate(13, Date.valueOf(p.getExpiryDate()));
                } else {
                    ps.setNull(13, Types.DATE);
                }
                ps.setString(14, p.getStatus() != null ? p.getStatus().name() : Product.Status.ACTIVE.name());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        p.setId(id);
                        log.info("Inserted product '{}' id={}", p.getName(), id);
                        return id;
                    }
                }
            }
            throw new DatabaseException("Insert product returned no generated key");
        } catch (SQLException e) {
            throw new DatabaseException("Insert product failed: " + p.getName(), e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void update(Product p) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
                ps.setString(1, p.getBarcode());
                ps.setString(2, p.getName());
                ps.setString(3, p.getDescription());
                setNullableInt(ps, 4, p.getCategoryId());
                setNullableInt(ps, 5, p.getSupplierId());
                ps.setString(6, p.getBrand());
                ps.setString(7, p.getUnit().name());
                ps.setBigDecimal(8,  p.getPurchasePrice());
                ps.setBigDecimal(9,  p.getSellingPrice());
                ps.setBigDecimal(10, p.getWholesalePrice());
                ps.setBigDecimal(11, p.getMinimumStock());
                if (p.getExpiryDate() != null) {
                    ps.setDate(12, Date.valueOf(p.getExpiryDate()));
                } else {
                    ps.setNull(12, Types.DATE);
                }
                ps.setString(13, p.getStatus().name());
                ps.setInt(14, p.getId());
                ps.executeUpdate();
                log.debug("Updated product id={}", p.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Update product failed id=" + p.getId(), e);
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
            throw new DatabaseException("Set product status failed id=" + id, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // Stock management
    // -------------------------------------------------------------------------

    @Override
    public BigDecimal updateStock(int productId, BigDecimal delta, boolean allowNegative) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();

            // Read current stock and product name first (for error message)
            BigDecimal currentStock = null;
            String productName = "";
            try (PreparedStatement ps = conn.prepareStatement(GET_STOCK)) {
                ps.setInt(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentStock = rs.getBigDecimal(1);
                        productName  = rs.getString(2);
                    }
                }
            }

            String sql = allowNegative ? UPDATE_STOCK_ALLOW_NEG : UPDATE_STOCK_SAFE;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBigDecimal(1, delta);
                ps.setInt(2, productId);
                if (!allowNegative) {
                    ps.setBigDecimal(3, delta); // used in WHERE current_stock + delta >= 0
                }
                int rows = ps.executeUpdate();

                if (rows == 0 && !allowNegative) {
                    // WHERE clause prevented the update — stock would go negative
                    throw new StockException(
                        productId, productName,
                        currentStock != null ? currentStock : BigDecimal.ZERO,
                        delta.negate()
                    );
                }
            }

            // Return the new stock value
            try (PreparedStatement ps = conn.prepareStatement(GET_STOCK)) {
                ps.setInt(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal newStock = rs.getBigDecimal(1);
                        log.debug("Stock updated for product id={}: {} + {} = {}",
                                productId, currentStock, delta, newStock);
                        return newStock;
                    }
                }
            }

            return BigDecimal.ZERO;
        } catch (StockException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("updateStock failed for product id=" + productId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void setStock(int productId, BigDecimal newStock) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SET_STOCK)) {
                ps.setBigDecimal(1, newStock);
                ps.setInt(2, productId);
                ps.executeUpdate();
                log.info("Stock set to {} for product id={}", newStock, productId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("setStock failed for product id=" + productId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void updatePurchasePrice(int productId, BigDecimal newPrice) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_PURCHASE_PRICE)) {
                ps.setBigDecimal(1, newPrice);
                ps.setInt(2, productId);
                ps.executeUpdate();
                log.debug("Purchase price updated to {} for product id={}", newPrice, productId);
            }
        } catch (SQLException e) {
            throw new DatabaseException("updatePurchasePrice failed for product id=" + productId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // Existence checks
    // -------------------------------------------------------------------------

    @Override
    public boolean barcodeExists(String barcode) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(BARCODE_EXISTS)) {
                ps.setString(1, barcode);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("barcodeExists check failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public boolean barcodeExistsExcluding(String barcode, int excludeId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(BARCODE_EXISTS_EXCL)) {
                ps.setString(1, barcode);
                ps.setInt(2, excludeId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("barcodeExistsExcluding check failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public int countActive() {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(COUNT_ACTIVE);
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("countActive failed for products", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // -------------------------------------------------------------------------
    // Row mapper
    // -------------------------------------------------------------------------

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setBarcode(rs.getString("barcode"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setBrand(rs.getString("brand"));

        String unitStr = rs.getString("unit");
        if (unitStr != null) {
            try { p.setUnit(Product.Unit.valueOf(unitStr)); }
            catch (IllegalArgumentException ex) { p.setUnit(Product.Unit.PIECE); }
        }

        p.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        p.setSellingPrice(rs.getBigDecimal("selling_price"));
        p.setWholesalePrice(rs.getBigDecimal("wholesale_price"));
        p.setCurrentStock(rs.getBigDecimal("current_stock"));
        p.setMinimumStock(rs.getBigDecimal("minimum_stock"));

        Date expiry = rs.getDate("expiry_date");
        if (expiry != null) p.setExpiryDate(expiry.toLocalDate());

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try { p.setStatus(Product.Status.valueOf(statusStr)); }
            catch (IllegalArgumentException ex) { p.setStatus(Product.Status.ACTIVE); }
        }

        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) p.setUpdatedAt(ua.toLocalDateTime());

        // Category (may be null if not joined or product has no category)
        int catId = rs.getInt("cat_id");
        if (!rs.wasNull()) {
            Category cat = new Category(catId, rs.getString("cat_name"));
            p.setCategory(cat);
        }

        // Supplier (may be null)
        int supId = rs.getInt("sup_id");
        if (!rs.wasNull()) {
            Supplier sup = new Supplier();
            sup.setId(supId);
            sup.setName(rs.getString("sup_name"));
            p.setSupplier(sup);
        }

        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setNullableInt(PreparedStatement ps, int index, int value) throws SQLException {
        if (value > 0) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    /** Functional interface for setting PreparedStatement parameters. */
    @FunctionalInterface
    private interface StatementSetter {
        void set(PreparedStatement ps) throws SQLException;
    }
}
