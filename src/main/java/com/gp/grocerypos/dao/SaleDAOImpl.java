package com.gp.grocerypos.dao;

import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleDAOImpl implements SaleDAO {

    private static final Logger log = LoggerFactory.getLogger(SaleDAOImpl.class);

    // ── Base SELECT joins customer and user ───────────────────────────────────
    private static final String SELECT_BASE =
        "SELECT s.id, s.invoice_number, s.sale_date, s.subtotal, s.discount_amount, " +
        "       s.tax_amount, s.grand_total, s.payment_method, s.cash_received, " +
        "       s.change_amount, s.status, s.notes, s.created_at, " +
        "       c.id AS cust_id, c.name AS cust_name, " +
        "       u.id AS usr_id,  u.full_name AS usr_name " +
        "FROM   sales s " +
        "LEFT JOIN customers c ON c.id = s.customer_id " +
        "JOIN     users      u ON u.id = s.user_id ";

    private static final String FIND_BY_ID =
        SELECT_BASE + "WHERE s.id = ?";
    private static final String FIND_BY_INVOICE =
        SELECT_BASE + "WHERE s.invoice_number = ?";
    private static final String FIND_ALL =
        SELECT_BASE + "ORDER BY s.sale_date DESC";
    private static final String FIND_BY_DATE =
        SELECT_BASE +
        "WHERE DATE(s.sale_date) >= ? AND DATE(s.sale_date) <= ? " +
        "ORDER BY s.sale_date DESC";
    private static final String FIND_BY_USER =
        SELECT_BASE + "WHERE s.user_id = ? ORDER BY s.sale_date DESC";
    private static final String FIND_BY_CUSTOMER =
        SELECT_BASE + "WHERE s.customer_id = ? ORDER BY s.sale_date DESC";
    private static final String FIND_BY_PAYMENT =
        SELECT_BASE + "WHERE s.payment_method = ? ORDER BY s.sale_date DESC";

    private static final String INSERT_HEADER =
        "INSERT INTO sales (invoice_number, customer_id, user_id, sale_date, " +
        "subtotal, discount_amount, tax_amount, grand_total, payment_method, " +
        "cash_received, change_amount, status, notes) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String INSERT_ITEM =
        "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, " +
        "discount_amount, total_price) VALUES (?,?,?,?,?,?)";

    private static final String INSERT_PAYMENT =
        "INSERT INTO payments (sale_id, payment_method, amount) VALUES (?,?,?)";

    private static final String FIND_ITEMS =
        "SELECT si.id, si.sale_id, si.quantity, si.unit_price, " +
        "       si.discount_amount, si.total_price, " +
        "       p.id AS prod_id, p.name AS prod_name, p.barcode, p.unit " +
        "FROM   sale_items si " +
        "JOIN   products p ON p.id = si.product_id " +
        "WHERE  si.sale_id = ? ORDER BY si.id";

    private static final String VOID_SALE =
        "UPDATE sales SET status='VOIDED' WHERE id=?";
    private static final String MARK_RETURNED =
        "UPDATE sales SET status='RETURNED' WHERE id=?";

    private static final String COUNT_TODAY =
        "SELECT COUNT(*) FROM sales " +
        "WHERE DATE(sale_date)=CURDATE() AND status='COMPLETED'";
    private static final String SUM_TODAY =
        "SELECT COALESCE(SUM(grand_total),0) FROM sales " +
        "WHERE DATE(sale_date)=CURDATE() AND status='COMPLETED'";

    // ── Reads (own connection) ────────────────────────────────────────────────

    @Override
    public Optional<Sale> findById(int id) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Sale s = mapHeader(rs);
                        s.setItems(findItemsBySaleId(s.getId()));
                        return Optional.of(s);
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("findById sale failed id=" + id, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public Optional<Sale> findByInvoiceNumber(String number) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_INVOICE)) {
                ps.setString(1, number);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Sale s = mapHeader(rs);
                        s.setItems(findItemsBySaleId(s.getId()));
                        return Optional.of(s);
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("findByInvoiceNumber failed: " + number, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override public List<Sale> findAll()                         { return query(FIND_ALL, null); }
    @Override public List<Sale> findByUser(int userId)           {
        return query(FIND_BY_USER,     ps -> ps.setInt(1, userId));
    }
    @Override public List<Sale> findByCustomer(int customerId)   {
        return query(FIND_BY_CUSTOMER, ps -> ps.setInt(1, customerId));
    }
    @Override public List<Sale> findByPaymentMethod(String m)    {
        return query(FIND_BY_PAYMENT,  ps -> ps.setString(1, m));
    }

    @Override
    public List<Sale> findByDateRange(LocalDate from, LocalDate to) {
        return query(FIND_BY_DATE, ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
        });
    }

    private List<Sale> query(String sql, StatementSetter setter) {
        Connection conn = null;
        List<Sale> list = new ArrayList<>();
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
            throw new DatabaseException("Sale list query failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── Transactional writes (caller's connection) ────────────────────────────

    @Override
    public int insertHeader(Sale sale, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                INSERT_HEADER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sale.getInvoiceNumber());
            // customer_id is nullable
            if (sale.getCustomerId() > 0) ps.setInt(2, sale.getCustomerId());
            else                          ps.setNull(2, Types.INTEGER);
            ps.setInt(3, sale.getUserId());
            ps.setTimestamp(4, Timestamp.valueOf(sale.getSaleDate()));
            ps.setBigDecimal(5,  sale.getSubtotal());
            ps.setBigDecimal(6,  sale.getDiscountAmount());
            ps.setBigDecimal(7,  sale.getTaxAmount());
            ps.setBigDecimal(8,  sale.getGrandTotal());
            ps.setString(9,      sale.getPaymentMethod());
            ps.setBigDecimal(10, sale.getCashReceived());
            ps.setBigDecimal(11, sale.getChangeAmount());
            ps.setString(12, sale.getStatus() != null
                ? sale.getStatus().name() : Sale.Status.COMPLETED.name());
            ps.setString(13, sale.getNotes());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    sale.setId(id);
                    log.info("Inserted sale header '{}' id={}", sale.getInvoiceNumber(), id);
                    return id;
                }
            }
            throw new DatabaseException("Insert sale returned no generated key");
        } catch (SQLException e) {
            throw new DatabaseException("insertHeader sale failed: "
                    + sale.getInvoiceNumber(), e);
        }
    }

    @Override
    public void insertItems(List<SaleItem> items, int saleId, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_ITEM)) {
            for (SaleItem item : items) {
                ps.setInt(1, saleId);
                ps.setInt(2, item.getProductId());
                ps.setBigDecimal(3, item.getQuantity());
                ps.setBigDecimal(4, item.getUnitPrice());
                ps.setBigDecimal(5, item.getDiscountAmount());
                ps.setBigDecimal(6, item.getTotalPrice());
                ps.addBatch();
            }
            ps.executeBatch();
            log.debug("Inserted {} sale items for saleId={}", items.size(), saleId);
        } catch (SQLException e) {
            throw new DatabaseException("insertItems sale failed for saleId=" + saleId, e);
        }
    }

    @Override
    public void insertPayment(int saleId, String method,
                              BigDecimal amount, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_PAYMENT)) {
            ps.setInt(1, saleId);
            ps.setString(2, method);
            ps.setBigDecimal(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("insertPayment failed for saleId=" + saleId, e);
        }
    }

    // ── Items query (own connection) ──────────────────────────────────────────

    @Override
    public List<SaleItem> findItemsBySaleId(int saleId) {
        Connection conn = null;
        List<SaleItem> items = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_ITEMS)) {
                ps.setInt(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) items.add(mapItem(rs));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new DatabaseException("findItemsBySaleId failed id=" + saleId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void voidSale(int saleId) {
        updateStatus(VOID_SALE, saleId);
    }

    @Override
    public void markReturned(int saleId) {
        updateStatus(MARK_RETURNED, saleId);
    }

    private void updateStatus(String sql, int saleId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, saleId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DatabaseException("updateStatus sale failed id=" + saleId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public int countToday() {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(COUNT_TODAY);
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("countToday sales failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public BigDecimal sumTodaySales() {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SUM_TODAY);
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DatabaseException("sumTodaySales failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── Row mappers ───────────────────────────────────────────────────────────

    private Sale mapHeader(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("id"));
        s.setInvoiceNumber(rs.getString("invoice_number"));
        s.setSubtotal(rs.getBigDecimal("subtotal"));
        s.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        s.setTaxAmount(rs.getBigDecimal("tax_amount"));
        s.setGrandTotal(rs.getBigDecimal("grand_total"));
        s.setPaymentMethod(rs.getString("payment_method"));
        s.setCashReceived(rs.getBigDecimal("cash_received"));
        s.setChangeAmount(rs.getBigDecimal("change_amount"));
        s.setNotes(rs.getString("notes"));

        String status = rs.getString("status");
        if (status != null) {
            try { s.setStatus(Sale.Status.valueOf(status)); }
            catch (IllegalArgumentException ex) { s.setStatus(Sale.Status.COMPLETED); }
        }

        Timestamp sd = rs.getTimestamp("sale_date");
        if (sd != null) s.setSaleDate(sd.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) s.setCreatedAt(ca.toLocalDateTime());

        // Customer (nullable LEFT JOIN)
        int custId = rs.getInt("cust_id");
        if (!rs.wasNull()) {
            Customer c = new Customer();
            c.setId(custId);
            c.setName(rs.getString("cust_name"));
            s.setCustomer(c);
        }

        // User
        User u = new User();
        u.setId(rs.getInt("usr_id"));
        u.setFullName(rs.getString("usr_name"));
        s.setUser(u);

        return s;
    }

    private SaleItem mapItem(ResultSet rs) throws SQLException {
        SaleItem item = new SaleItem();
        item.setId(rs.getInt("id"));
        item.setSaleId(rs.getInt("sale_id"));
        item.setQuantity(rs.getBigDecimal("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        item.setTotalPrice(rs.getBigDecimal("total_price"));

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
