package com.gp.grocerypos.controller.dashboard;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.service.SessionContext;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller for the Dashboard view.
 * <p>
 * Loads live summary statistics from the database and populates the
 * stat cards. All queries are lightweight indexed lookups.
 * <p>
 * Phase 2 shows the core cards. Additional cards (profit, recent
 * transactions table) will be added in later phases.
 */
public class DashboardController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    // Stat card labels
    @FXML private Label todaySalesLabel;
    @FXML private Label todayTransactionsLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label expiringLabel;
    @FXML private Label totalSuppliersLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private Label activeUsersLabel;

    // Welcome / date
    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionContext session = SessionContext.getInstance();
        String name = session.isLoggedIn()
                ? session.getCurrentUser().getFullName()
                : "User";
        welcomeLabel.setText("Welcome back, " + name + "!");
        dateLabel.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));

        loadStats();
        log.debug("DashboardController initialised.");
    }

    // ─────────────────────────────────────────────────────────────
    // Load all stat values
    // ─────────────────────────────────────────────────────────────

    private void loadStats() {
        String today = LocalDate.now().toString(); // yyyy-MM-dd

        todaySalesLabel.setText(       querySingle(SQL_TODAY_SALES,        today));
        todayTransactionsLabel.setText(querySingle(SQL_TODAY_TRANSACTIONS,  today));
        totalProductsLabel.setText(    querySingleNoParam(SQL_TOTAL_PRODUCTS));
        lowStockLabel.setText(         querySingleNoParam(SQL_LOW_STOCK));
        expiringLabel.setText(         queryExpiring());
        totalSuppliersLabel.setText(   querySingleNoParam(SQL_TOTAL_SUPPLIERS));
        totalCustomersLabel.setText(   querySingleNoParam(SQL_TOTAL_CUSTOMERS));
        activeUsersLabel.setText(      querySingleNoParam(SQL_ACTIVE_USERS));
    }

    // ─────────────────────────────────────────────────────────────
    // SQL
    // ─────────────────────────────────────────────────────────────

    private static final String SQL_TODAY_SALES =
        "SELECT COALESCE(SUM(grand_total), 0) FROM sales " +
        "WHERE DATE(sale_date) = ? AND status = 'COMPLETED'";

    private static final String SQL_TODAY_TRANSACTIONS =
        "SELECT COUNT(*) FROM sales " +
        "WHERE DATE(sale_date) = ? AND status = 'COMPLETED'";

    private static final String SQL_TOTAL_PRODUCTS =
        "SELECT COUNT(*) FROM products WHERE status = 'ACTIVE'";

    private static final String SQL_LOW_STOCK =
        "SELECT COUNT(*) FROM products " +
        "WHERE status = 'ACTIVE' AND current_stock <= minimum_stock";

    private static final String SQL_TOTAL_SUPPLIERS =
        "SELECT COUNT(*) FROM suppliers WHERE status = 'ACTIVE'";

    private static final String SQL_TOTAL_CUSTOMERS =
        "SELECT COUNT(*) FROM customers WHERE status = 'ACTIVE'";

    private static final String SQL_ACTIVE_USERS =
        "SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'";

    // ─────────────────────────────────────────────────────────────
    // Query helpers
    // ─────────────────────────────────────────────────────────────

    private String querySingle(String sql, String param) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, param);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String val = rs.getString(1);
                        // Format currency values
                        if (sql.contains("SUM")) {
                            try {
                                double d = Double.parseDouble(val);
                                String symbol = AppConfig.getInstance().getCurrencySymbol();
                                return String.format("%s %,.2f", symbol, d);
                            } catch (NumberFormatException ignored) {}
                        }
                        return val != null ? val : "0";
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Dashboard query failed: {}", e.getMessage());
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
        return "—";
    }

    private String querySingleNoParam(String sql) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String val = rs.getString(1);
                    return val != null ? val : "0";
                }
            }
        } catch (Exception e) {
            log.warn("Dashboard query failed: {}", e.getMessage());
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
        return "—";
    }

    private String queryExpiring() {
        Connection conn = null;
        try {
            String days = AppConfig.getInstance().get("inventory.expiryWarningDays", "30");
            String sql  = "SELECT COUNT(*) FROM products " +
                          "WHERE status = 'ACTIVE' AND expiry_date IS NOT NULL " +
                          "AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL " + days + " DAY) " +
                          "AND expiry_date >= CURDATE()";
            conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) {
            log.warn("Expiry dashboard query failed: {}", e.getMessage());
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
        return "—";
    }
}
