package com.gp.grocerypos.service;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.dao.*;
import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.*;
import com.gp.grocerypos.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleServiceImpl implements SaleService {

    private static final Logger log = LoggerFactory.getLogger(SaleServiceImpl.class);

    private final SaleDAO          saleDAO;
    private final ProductDAO       productDAO;
    private final SettingDAO       settingDAO;
    private final InventoryService inventoryService;

    public SaleServiceImpl() {
        this.saleDAO          = new SaleDAOImpl();
        this.productDAO       = new ProductDAOImpl();
        this.settingDAO       = new SettingDAOImpl();
        this.inventoryService = new InventoryServiceImpl();
    }

    public SaleServiceImpl(SaleDAO saleDAO, ProductDAO productDAO,
                           SettingDAO settingDAO, InventoryService inventoryService) {
        this.saleDAO          = saleDAO;
        this.productDAO       = productDAO;
        this.settingDAO       = settingDAO;
        this.inventoryService = inventoryService;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override public List<Sale> getAllSales()                          { return saleDAO.findAll(); }
    @Override public List<Sale> getSalesByDateRange(LocalDate f, LocalDate t) { return saleDAO.findByDateRange(f, t); }
    @Override public List<Sale> getSalesByUser(int userId)            { return saleDAO.findByUser(userId); }
    @Override public List<Sale> getSalesByCustomer(int customerId)    { return saleDAO.findByCustomer(customerId); }
    @Override public Optional<Sale> getSaleById(int id)               { return saleDAO.findById(id); }
    @Override public Optional<Sale> getSaleByInvoiceNumber(String n)  { return saleDAO.findByInvoiceNumber(n); }
    @Override public int  countTodaySales()                           { return saleDAO.countToday(); }
    @Override public BigDecimal sumTodaySales()                       { return saleDAO.sumTodaySales(); }

    // ── Core sale transaction ─────────────────────────────────────────────────

    @Override
    public Sale completeSale(Sale sale) {
        SessionContext.getInstance().requirePermission("SALE_CREATE");

        // ── Step 1: Validate ───────────────────────────────────────────────
        validateCart(sale);

        // ── Step 2: Apply tax ──────────────────────────────────────────────
        boolean taxEnabled = Boolean.parseBoolean(
            settingDAO.getValue("tax_enabled", "false"));
        BigDecimal taxRate = BigDecimal.ZERO;
        if (taxEnabled) {
            try {
                taxRate = new BigDecimal(settingDAO.getValue("tax_rate", "0"))
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                log.warn("Invalid tax_rate setting — using 0");
            }
        }
        sale.recalculateTotals(taxRate, taxEnabled);

        // ── Step 3: Generate invoice number ───────────────────────────────
        String prefix  = settingDAO.getValue("invoice_prefix", "INV-");
        int    counter = settingDAO.incrementAndGet("invoice_counter");
        int    length  = Integer.parseInt(
            settingDAO.getValue("invoice_number_length", "6"));
        String invoice = prefix + String.format("%0" + length + "d", counter);
        sale.setInvoiceNumber(invoice);
        sale.setSaleDate(LocalDateTime.now());
        sale.setUser(SessionContext.getInstance().getCurrentUser());
        sale.setStatus(Sale.Status.COMPLETED);

        // ── Step 4: Pre-validate stock (before opening transaction) ───────
        boolean allowNeg = Boolean.parseBoolean(
            settingDAO.getValue("allow_negative_stock", "false"));

        if (!allowNeg) {
            for (SaleItem item : sale.getItems()) {
                Product p = productDAO.findById(item.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(
                        String.valueOf(item.getProductId())));
                if (!p.isActive()) {
                    throw new SaleException(
                        "Product '" + p.getName() + "' is not active.",
                        "Product '" + p.getName() + "' is inactive and cannot be sold.");
                }
                if (p.getCurrentStock().compareTo(item.getQuantity()) < 0) {
                    throw new StockException(p.getId(), p.getName(),
                        p.getCurrentStock(), item.getQuantity());
                }
            }
        }

        // ── Step 5: Database transaction ──────────────────────────────────
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            saleDAO.insertHeader(sale, conn);
            saleDAO.insertItems(sale.getItems(), sale.getId(), conn);
            saleDAO.insertPayment(sale.getId(),
                sale.getPaymentMethod(), sale.getGrandTotal(), conn);

            conn.commit();
            log.info("Sale committed: {} id={} total={}",
                invoice, sale.getId(), sale.getGrandTotal());

        } catch (SaleException | StockException e) {
            rollback(conn, counter);
            throw e;
        } catch (Exception e) {
            rollback(conn, counter);
            throw new SaleException(
                "Sale transaction failed: " + e.getMessage(),
                "The sale could not be completed. Please try again.", e);
        } finally {
            resetAutoCommit(conn);
            DatabaseConnection.getInstance().releaseConnection(conn);
        }

        // ── Step 6: Post-commit stock updates ─────────────────────────────
        // These run after the sale record exists. Each is atomic at SQL level.
        List<String> stockErrors = new ArrayList<>();
        for (SaleItem item : sale.getItems()) {
            try {
                inventoryService.recordSaleMovement(
                    item.getProductId(), item.getQuantity(), sale.getId());
            } catch (Exception e) {
                log.error("Stock update failed after sale id={} product id={}: {}",
                    sale.getId(), item.getProductId(), e.getMessage(), e);
                stockErrors.add(item.getProductName() + ": " + e.getMessage());
            }
        }

        if (!stockErrors.isEmpty()) {
            log.warn("Sale {} saved but {} stock update(s) failed: {}",
                invoice, stockErrors.size(), stockErrors);
        }

        log.info("Sale complete: {} customer={} items={} total={}",
            invoice, sale.getCustomerName(),
            sale.getItems().size(), sale.getGrandTotal());

        return sale;
    }

    // ── Void ──────────────────────────────────────────────────────────────────

    @Override
    public void voidSale(int saleId) {
        SessionContext.getInstance().requirePermission("SALE_VOID");

        Sale existing = saleDAO.findById(saleId)
            .orElseThrow(() -> new SaleException(
                "Sale not found id=" + saleId, "Sale not found."));

        if (existing.getStatus() == Sale.Status.VOIDED) {
            throw new SaleException(
                "Sale already voided id=" + saleId, "This sale is already voided.");
        }
        saleDAO.voidSale(saleId);
        log.info("Sale voided id={} by user '{}'", saleId,
            SessionContext.getInstance().getCurrentUser().getUsername());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateCart(Sale sale) {
        List<String> errors = new ArrayList<>();

        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            errors.add("The cart is empty. Please add products before completing the sale.");
        } else {
            for (int i = 0; i < sale.getItems().size(); i++) {
                SaleItem item = sale.getItems().get(i);
                if (item.getProductId() == 0) {
                    errors.add("Item #" + (i + 1) + ": product is not set.");
                }
                if (item.getQuantity() == null
                        || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Item #" + (i + 1) + ": quantity must be greater than zero.");
                }
                if (item.getUnitPrice() == null
                        || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Item #" + (i + 1) + ": unit price must be zero or greater.");
                }
            }
        }

        if (sale.getPaymentMethod() == null || sale.getPaymentMethod().isBlank()) {
            errors.add("Payment method is required.");
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);
    }

    private void rollback(Connection conn, int counter) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (Exception rb) {
                log.error("Sale rollback failed: {}", rb.getMessage());
            }
        }
        // Try to reverse the invoice counter
        try {
            settingDAO.setValue("invoice_counter", String.valueOf(counter - 1));
        } catch (Exception ignored) {}
    }

    private void resetAutoCommit(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
        }
    }
}
