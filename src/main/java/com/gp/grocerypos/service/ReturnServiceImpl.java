package com.gp.grocerypos.service;

import com.gp.grocerypos.dao.*;
import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.exception.ValidationException;
import com.gp.grocerypos.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReturnServiceImpl implements ReturnService {

    private static final Logger log = LoggerFactory.getLogger(ReturnServiceImpl.class);

    private final ReturnDAO        returnDAO;
    private final SaleDAO          saleDAO;
    private final SettingDAO       settingDAO;
    private final InventoryService inventoryService;

    public ReturnServiceImpl() {
        this.returnDAO        = new ReturnDAOImpl();
        this.saleDAO          = new SaleDAOImpl();
        this.settingDAO       = new SettingDAOImpl();
        this.inventoryService = new InventoryServiceImpl();
    }

    public ReturnServiceImpl(ReturnDAO returnDAO, SaleDAO saleDAO,
                             SettingDAO settingDAO, InventoryService inventoryService) {
        this.returnDAO        = returnDAO;
        this.saleDAO          = saleDAO;
        this.settingDAO       = settingDAO;
        this.inventoryService = inventoryService;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    public List<SaleReturn> getAllReturns() {
        return returnDAO.findAll();
    }

    @Override
    public List<SaleReturn> getReturnsByDateRange(LocalDate from, LocalDate to) {
        return returnDAO.findByDateRange(from, to);
    }

    @Override
    public List<SaleReturn> getReturnsBySaleId(int saleId) {
        return returnDAO.findBySaleId(saleId);
    }

    @Override
    public Optional<SaleReturn> getReturnById(int id) {
        return returnDAO.findById(id);
    }

    // ── Core return transaction ───────────────────────────────────────────────

    @Override
    public SaleReturn processReturn(SaleReturn saleReturn) {
        SessionContext.getInstance().requirePermission("RETURN_PROCESS");

        // ── Step 1: Load and validate the original sale ────────────────────
        Sale originalSale = saleDAO.findById(saleReturn.getSaleId())
            .orElseThrow(() -> new ValidationException(
                "Original sale not found id=" + saleReturn.getSaleId()));

        if (originalSale.getStatus() == Sale.Status.VOIDED) {
            throw new ValidationException("Cannot process a return for a voided sale.");
        }

        // Load original sale items for quantity validation
        List<SaleItem> originalItems = saleDAO.findItemsBySaleId(originalSale.getId());

        validate(saleReturn, originalItems);

        // ── Step 2: Generate return number ────────────────────────────────
        String prefix  = settingDAO.getValue("return_prefix", "RET-");
        int    counter = settingDAO.incrementAndGet("return_counter");
        int    length  = Integer.parseInt(
            settingDAO.getValue("invoice_number_length", "6"));
        String number  = prefix + String.format("%0" + length + "d", counter);

        saleReturn.setReturnNumber(number);
        saleReturn.setReturnDate(LocalDateTime.now());
        saleReturn.setUser(SessionContext.getInstance().getCurrentUser());
        saleReturn.setStatus(SaleReturn.Status.COMPLETED);
        saleReturn.recalculate();

        // ── Step 3: Database transaction ──────────────────────────────────
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 3a. Insert return header
            returnDAO.insertHeader(saleReturn, conn);

            // 3b. Insert return items
            returnDAO.insertItems(saleReturn.getItems(), saleReturn.getId(), conn);

            // 3c. Mark the original sale as RETURNED
            saleDAO.markReturned(originalSale.getId());

            conn.commit();
            log.info("Return committed: {} id={} refund={}",
                number, saleReturn.getId(), saleReturn.getTotalRefund());

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); }
                catch (Exception rb) { log.error("Return rollback failed: {}", rb.getMessage()); }
            }
            // Reverse counter
            try { settingDAO.setValue("return_counter", String.valueOf(counter - 1)); }
            catch (Exception ignored) {}

            throw new DatabaseException(
                "Return transaction failed: " + e.getMessage(),
                "The return could not be processed. Please try again.", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
                DatabaseConnection.getInstance().releaseConnection(conn);
            }
        }

        // ── Step 4: Post-commit stock restoration ─────────────────────────
        List<String> stockErrors = new ArrayList<>();
        for (ReturnItem item : saleReturn.getItems()) {
            if (!item.isRestock()) continue;
            try {
                inventoryService.recordReturnMovement(
                    item.getProductId(),
                    item.getQuantity(),
                    saleReturn.getId()
                );
            } catch (Exception e) {
                log.error("Stock restore failed for product id={}: {}",
                    item.getProductId(), e.getMessage(), e);
                stockErrors.add(item.getProductName() + ": " + e.getMessage());
            }
        }

        if (!stockErrors.isEmpty()) {
            log.warn("Return {} saved but {} stock restore(s) failed: {}",
                number, stockErrors.size(), stockErrors);
        }

        log.info("Return complete: {} invoice={} items={} refund={}",
            number, saleReturn.getInvoiceNumber(),
            saleReturn.getItems().size(), saleReturn.getTotalRefund());

        return saleReturn;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(SaleReturn sr, List<SaleItem> originalItems) {
        List<String> errors = new ArrayList<>();

        if (sr.getItems() == null || sr.getItems().isEmpty()) {
            errors.add("Please select at least one item to return.");
            throw new ValidationException(errors);
        }

        for (ReturnItem ri : sr.getItems()) {
            if (ri.getProductId() == 0) {
                errors.add("A return item has no product set.");
                continue;
            }
            if (ri.getQuantity() == null || ri.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Return quantity for '"
                    + ri.getProductName() + "' must be greater than zero.");
                continue;
            }
            // Find matching original sale item and check quantity
            SaleItem original = originalItems.stream()
                .filter(oi -> oi.getId() == ri.getSaleItemId())
                .findFirst().orElse(null);

            if (original == null) {
                errors.add("Sale item not found for product '" + ri.getProductName() + "'.");
            } else if (ri.getQuantity().compareTo(original.getQuantity()) > 0) {
                errors.add("Return quantity (" + ri.getQuantity()
                    + ") for '" + ri.getProductName()
                    + "' exceeds original sold quantity ("
                    + original.getQuantity() + ").");
            }
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
