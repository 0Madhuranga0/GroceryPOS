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

public class ReturnDAOImpl implements ReturnDAO {

    private static final Logger log = LoggerFactory.getLogger(ReturnDAOImpl.class);

    // Base SELECT joins original sale invoice, user full_name
    private static final String SELECT_BASE =
        "SELECT r.id, r.return_number, r.sale_id, r.return_date, " +
        "       r.total_refund, r.reason, r.status, r.created_at, " +
        "       s.invoice_number, " +
        "       u.id AS usr_id, u.full_name AS usr_name " +
        "FROM   returns r " +
        "JOIN   sales s ON s.id = r.sale_id " +
        "JOIN   users u ON u.id = r.user_id ";

    private static final String FIND_BY_ID =
        SELECT_BASE + "WHERE r.id = ?";
    private static final String FIND_BY_NUMBER =
        SELECT_BASE + "WHERE r.return_number = ?";
    private static final String FIND_BY_SALE =
        SELECT_BASE + "WHERE r.sale_id = ? ORDER BY r.return_date DESC";
    private static final String FIND_BY_DATE =
        SELECT_BASE +
        "WHERE DATE(r.return_date) >= ? AND DATE(r.return_date) <= ? " +
        "ORDER BY r.return_date DESC";
    private static final String FIND_ALL =
        SELECT_BASE + "ORDER BY r.return_date DESC";

    private static final String INSERT_HEADER =
        "INSERT INTO returns (return_number, sale_id, user_id, return_date, " +
        "total_refund, reason, status) VALUES (?,?,?,?,?,?,?)";

    private static final String INSERT_ITEM =
        "INSERT INTO return_items " +
        "(return_id, sale_item_id, product_id, quantity, unit_price, refund_amount, restock) " +
        "VALUES (?,?,?,?,?,?,?)";

    private static final String FIND_ITEMS =
        "SELECT ri.id, ri.return_id, ri.sale_item_id, ri.quantity, " +
        "       ri.unit_price, ri.refund_amount, ri.restock, " +
        "       p.id AS prod_id, p.name AS prod_name, p.barcode " +
        "FROM   return_items ri " +
        "JOIN   products p ON p.id = ri.product_id " +
        "WHERE  ri.return_id = ? ORDER BY ri.id";

    // ── Reads (own connection) ────────────────────────────────────────────────

    @Override
    public Optional<SaleReturn> findById(int id) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        SaleReturn sr = mapHeader(rs);
                        sr.setItems(findItemsByReturnId(sr.getId()));
                        return Optional.of(sr);
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("findById return failed id=" + id, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public Optional<SaleReturn> findByReturnNumber(String number) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_BY_NUMBER)) {
                ps.setString(1, number);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        SaleReturn sr = mapHeader(rs);
                        sr.setItems(findItemsByReturnId(sr.getId()));
                        return Optional.of(sr);
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("findByReturnNumber failed: " + number, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    @Override public List<SaleReturn> findAll()                   { return query(FIND_ALL,     null); }
    @Override public List<SaleReturn> findBySaleId(int saleId)    {
        return query(FIND_BY_SALE, ps -> ps.setInt(1, saleId));
    }

    @Override
    public List<SaleReturn> findByDateRange(LocalDate from, LocalDate to) {
        return query(FIND_BY_DATE, ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
        });
    }

    private List<SaleReturn> query(String sql, StatementSetter setter) {
        Connection conn = null;
        List<SaleReturn> list = new ArrayList<>();
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
            throw new DatabaseException("Return list query failed", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── Transactional writes (caller's connection) ────────────────────────────

    @Override
    public int insertHeader(SaleReturn sr, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                INSERT_HEADER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sr.getReturnNumber());
            ps.setInt(2,    sr.getSaleId());
            ps.setInt(3,    sr.getUserId());
            ps.setTimestamp(4, sr.getReturnDate() != null
                ? Timestamp.valueOf(sr.getReturnDate()) : null);
            ps.setBigDecimal(5, sr.getTotalRefund());
            ps.setString(6,     sr.getReason());
            ps.setString(7, sr.getStatus() != null
                ? sr.getStatus().name() : SaleReturn.Status.COMPLETED.name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    sr.setId(id);
                    log.info("Inserted return header '{}' id={}",
                            sr.getReturnNumber(), id);
                    return id;
                }
            }
            throw new DatabaseException("Insert return returned no generated key");
        } catch (SQLException e) {
            throw new DatabaseException("insertHeader return failed: "
                    + sr.getReturnNumber(), e);
        }
    }

    @Override
    public void insertItems(List<ReturnItem> items, int returnId, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_ITEM)) {
            for (ReturnItem item : items) {
                ps.setInt(1,         returnId);
                ps.setInt(2,         item.getSaleItemId());
                ps.setInt(3,         item.getProductId());
                ps.setBigDecimal(4,  item.getQuantity());
                ps.setBigDecimal(5,  item.getUnitPrice());
                ps.setBigDecimal(6,  item.getRefundAmount());
                ps.setInt(7,         item.isRestock() ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
            log.debug("Inserted {} return items for returnId={}", items.size(), returnId);
        } catch (SQLException e) {
            throw new DatabaseException("insertItems return failed for returnId=" + returnId, e);
        }
    }

    // ── Items query (own connection) ──────────────────────────────────────────

    @Override
    public List<ReturnItem> findItemsByReturnId(int returnId) {
        Connection conn = null;
        List<ReturnItem> items = new ArrayList<>();
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(FIND_ITEMS)) {
                ps.setInt(1, returnId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) items.add(mapItem(rs));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new DatabaseException("findItemsByReturnId failed id=" + returnId, e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── Row mappers ───────────────────────────────────────────────────────────

    private SaleReturn mapHeader(ResultSet rs) throws SQLException {
        SaleReturn sr = new SaleReturn();
        sr.setId(rs.getInt("id"));
        sr.setReturnNumber(rs.getString("return_number"));
        sr.setTotalRefund(rs.getBigDecimal("total_refund"));
        sr.setReason(rs.getString("reason"));

        String status = rs.getString("status");
        sr.setStatus(status != null
            ? SaleReturn.Status.valueOf(status) : SaleReturn.Status.COMPLETED);

        Timestamp rd = rs.getTimestamp("return_date");
        if (rd != null) sr.setReturnDate(rd.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) sr.setCreatedAt(ca.toLocalDateTime());

        // Lightweight Sale reference (just id + invoice number)
        Sale sale = new Sale();
        sale.setId(rs.getInt("sale_id"));
        sale.setInvoiceNumber(rs.getString("invoice_number"));
        sr.setOriginalSale(sale);

        // Lightweight User reference
        User u = new User();
        u.setId(rs.getInt("usr_id"));
        u.setFullName(rs.getString("usr_name"));
        sr.setUser(u);

        return sr;
    }

    private ReturnItem mapItem(ResultSet rs) throws SQLException {
        ReturnItem item = new ReturnItem();
        item.setId(rs.getInt("id"));
        item.setReturnId(rs.getInt("return_id"));
        item.setQuantity(rs.getBigDecimal("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setRefundAmount(rs.getBigDecimal("refund_amount"));
        item.setRestock(rs.getInt("restock") == 1);

        // Lightweight SaleItem reference
        SaleItem si = new SaleItem();
        si.setId(rs.getInt("sale_item_id"));
        item.setOriginalSaleItem(si);

        // Lightweight Product reference
        Product p = new Product();
        p.setId(rs.getInt("prod_id"));
        p.setName(rs.getString("prod_name"));
        p.setBarcode(rs.getString("barcode"));
        item.setProduct(p);

        return item;
    }

    @FunctionalInterface
    private interface StatementSetter {
        void set(PreparedStatement ps) throws SQLException;
    }
}
