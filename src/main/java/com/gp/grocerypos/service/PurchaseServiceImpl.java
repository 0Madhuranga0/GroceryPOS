package com.gp.grocerypos.service;

import com.gp.grocerypos.dao.*;
import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.exception.ValidationException;
import com.gp.grocerypos.model.Purchase;
import com.gp.grocerypos.model.PurchaseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PurchaseServiceImpl implements PurchaseService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseServiceImpl.class);

    private final PurchaseDAO      purchaseDAO;
    private final InventoryService inventoryService;
    private final SettingDAO       settingDAO;

    public PurchaseServiceImpl() {
        this.purchaseDAO      = new PurchaseDAOImpl();
        this.inventoryService = new InventoryServiceImpl();
        this.settingDAO       = new SettingDAOImpl();
    }

    public PurchaseServiceImpl(PurchaseDAO purchaseDAO,
                               InventoryService inventoryService,
                               SettingDAO settingDAO) {
        this.purchaseDAO      = purchaseDAO;
        this.inventoryService = inventoryService;
        this.settingDAO       = settingDAO;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    public List<Purchase> getAllPurchases() {
        return purchaseDAO.findAll();
    }

    @Override
    public List<Purchase> getPurchasesBySupplier(int supplierId) {
        return purchaseDAO.findBySupplier(supplierId);
    }

    @Override
    public List<Purchase> getPurchasesByDateRange(LocalDate from, LocalDate to) {
        return purchaseDAO.findByDateRange(from, to);
    }

    @Override
    public Optional<Purchase> getPurchaseById(int id) {
        return purchaseDAO.findById(id);
    }

    // ── Core transaction ──────────────────────────────────────────────────────

    @Override
    public Purchase savePurchase(Purchase purchase) {
        SessionContext.getInstance().requirePermission("PURCHASE_CREATE");

        // ── Validation ─────────────────────────────────────────────────────
        validate(purchase);

        // Stamp the current user and purchase time
        purchase.setUser(SessionContext.getInstance().getCurrentUser());
        purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setStatus(Purchase.Status.COMPLETED);
        purchase.recalculateTotal();

        // Generate purchase number atomically (uses DB transaction internally)
        String prefix  = settingDAO.getValue("purchase_prefix", "PUR-");
        int    counter = settingDAO.incrementAndGet("purchase_counter");
        int    length  = Integer.parseInt(settingDAO.getValue("invoice_number_length", "6"));
        String number  = prefix + String.format("%0" + length + "d", counter);
        purchase.setPurchaseNumber(number);

        // ── Database transaction ────────────────────────────────────────────
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1. INSERT purchase header
            purchaseDAO.insertHeader(purchase, conn);

            // 2. INSERT purchase items
            purchaseDAO.insertItems(purchase.getItems(), purchase.getId(), conn);

            // 3. Commit the purchase records
            conn.commit();

            log.info("Purchase saved: {} id={} total={}",
                    number, purchase.getId(), purchase.getTotalAmount());

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception rb) {
                    log.error("Rollback failed: {}", rb.getMessage());
                }
            }
            // Reverse the counter increment since the purchase failed
            try { settingDAO.setValue("purchase_counter",
                    String.valueOf(counter - 1)); } catch (Exception ignored) {}

            throw new DatabaseException(
                "Purchase transaction failed: " + e.getMessage(),
                "The purchase could not be saved. Please try again.", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                DatabaseConnection.getInstance().releaseConnection(conn);
            }
        }

        // ── Stock updates (outside the main transaction) ────────────────────
        // These run after commit so the purchase record is guaranteed to exist.
        // Each item update is itself atomic at the SQL level.
        List<String> stockErrors = new ArrayList<>();
        for (PurchaseItem item : purchase.getItems()) {
            try {
                inventoryService.recordPurchaseMovement(
                    item.getProductId(),
                    item.getQuantity(),
                    purchase.getId()
                );
                // Also update the purchase price on the product
                ProductDAO productDAO = new ProductDAOImpl();
                productDAO.updatePurchasePrice(item.getProductId(), item.getUnitCost());
            } catch (Exception e) {
                log.error("Stock update failed for product id={}: {}",
                        item.getProductId(), e.getMessage(), e);
                stockErrors.add("Product id=" + item.getProductId()
                        + ": " + e.getMessage());
            }
        }

        if (!stockErrors.isEmpty()) {
            log.warn("Purchase {} saved but {} stock update(s) failed: {}",
                    number, stockErrors.size(), stockErrors);
        }

        return purchase;
    }

    @Override
    public void cancelPurchase(int purchaseId) {
        SessionContext.getInstance().requirePermission("PURCHASE_CREATE");
        purchaseDAO.cancel(purchaseId);
        log.info("Purchase cancelled id={}", purchaseId);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(Purchase purchase) {
        List<String> errors = new ArrayList<>();

        if (purchase.getSupplier() == null || purchase.getSupplierId() == 0) {
            errors.add("Please select a supplier.");
        }

        if (purchase.getItems() == null || purchase.getItems().isEmpty()) {
            errors.add("Please add at least one product to the purchase.");
        } else {
            for (int i = 0; i < purchase.getItems().size(); i++) {
                PurchaseItem item = purchase.getItems().get(i);
                if (item.getProductId() == 0) {
                    errors.add("Item #" + (i + 1) + ": product is not set.");
                }
                if (item.getQuantity() == null
                        || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Item #" + (i + 1) + ": quantity must be greater than zero.");
                }
                if (item.getUnitCost() == null
                        || item.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Item #" + (i + 1) + ": unit cost must be zero or greater.");
                }
            }
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
