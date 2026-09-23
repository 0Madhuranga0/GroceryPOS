package com.gp.grocerypos.service;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.dao.InventoryMovementDAO;
import com.gp.grocerypos.dao.InventoryMovementDAOImpl;
import com.gp.grocerypos.dao.ProductDAO;
import com.gp.grocerypos.dao.ProductDAOImpl;
import com.gp.grocerypos.dao.SettingDAO;
import com.gp.grocerypos.dao.SettingDAOImpl;
import com.gp.grocerypos.exception.ValidationException;
import com.gp.grocerypos.model.InventoryMovement;
import com.gp.grocerypos.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final ProductDAO           productDAO;
    private final InventoryMovementDAO movementDAO;
    private final SettingDAO           settingDAO;

    public InventoryServiceImpl() {
        this.productDAO  = new ProductDAOImpl();
        this.movementDAO = new InventoryMovementDAOImpl();
        this.settingDAO  = new SettingDAOImpl();
    }

    // constructor for testing
    public InventoryServiceImpl(ProductDAO productDAO,
                                InventoryMovementDAO movementDAO,
                                SettingDAO settingDAO) {
        this.productDAO  = productDAO;
        this.movementDAO = movementDAO;
        this.settingDAO  = settingDAO;
    }

    // ── Stock queries ─────────────────────────────────────────────────────────

    @Override
    public List<Product> getLowStockProducts() {
        return productDAO.findLowStock();
    }

    @Override
    public List<Product> getExpiringSoonProducts() {
        int days = Integer.parseInt(
            AppConfig.getInstance().get("inventory.expiryWarningDays", "30"));
        return productDAO.findExpiringSoon(days);
    }

    @Override
    public List<Product> getExpiredProducts() {
        return productDAO.findExpired();
    }

    // ── Movement queries ──────────────────────────────────────────────────────

    @Override
    public List<InventoryMovement> getMovementsByProduct(int productId) {
        return movementDAO.findByProduct(productId);
    }

    @Override
    public List<InventoryMovement> getMovementsByDateRange(LocalDate from, LocalDate to) {
        return movementDAO.findByDateRange(from, to);
    }

    @Override
    public List<InventoryMovement> getRecentMovements(int limit) {
        return movementDAO.findRecent(limit);
    }

    // ── Manual adjustment ─────────────────────────────────────────────────────

    @Override
    public InventoryMovement adjustStock(int productId, BigDecimal newQuantity, String reason) {
        SessionContext.getInstance().requirePermission("INVENTORY_ADJUST");

        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("New stock quantity must be zero or greater.");
        }
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("A reason is required for stock adjustments.");
        }

        Product product = productDAO.findById(productId)
            .orElseThrow(() -> new ValidationException("Product not found id=" + productId));

        BigDecimal previousStock = product.getCurrentStock();
        BigDecimal delta         = newQuantity.subtract(previousStock);

        // Set the absolute value directly
        productDAO.setStock(productId, newQuantity);

        InventoryMovement movement = buildMovement(
            product, InventoryMovement.MovementType.ADJUSTMENT,
            delta.abs(), previousStock, newQuantity,
            null, "MANUAL", reason
        );
        movementDAO.insert(movement);

        log.info("Stock adjusted for product id={} '{}': {} → {} (reason: {})",
                productId, product.getName(), previousStock, newQuantity, reason);
        return movement;
    }

    @Override
    public InventoryMovement recordDamage(int productId, BigDecimal quantity, String reason) {
        SessionContext.getInstance().requirePermission("INVENTORY_ADJUST");

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Damage quantity must be greater than zero.");
        }
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("A reason is required for damage records.");
        }

        Product product = productDAO.findById(productId)
            .orElseThrow(() -> new ValidationException("Product not found id=" + productId));

        boolean allowNeg = Boolean.parseBoolean(
            settingDAO.getValue("allow_negative_stock", "false"));

        BigDecimal previousStock = product.getCurrentStock();
        BigDecimal newStock      = productDAO.updateStock(productId, quantity.negate(), allowNeg);

        InventoryMovement movement = buildMovement(
            product, InventoryMovement.MovementType.DAMAGE,
            quantity, previousStock, newStock,
            null, "MANUAL", reason
        );
        movementDAO.insert(movement);

        log.info("Damage recorded for product id={}: -{} (reason: {})", productId, quantity, reason);
        return movement;
    }

    // ── Internal transaction methods ──────────────────────────────────────────

    @Override
    public InventoryMovement recordSaleMovement(int productId, BigDecimal quantity, int saleId) {
        Product product = productDAO.findById(productId)
            .orElseThrow(() -> new ValidationException("Product not found id=" + productId));

        boolean allowNeg = Boolean.parseBoolean(
            settingDAO.getValue("allow_negative_stock", "false"));

        BigDecimal previousStock = product.getCurrentStock();
        BigDecimal newStock      = productDAO.updateStock(productId, quantity.negate(), allowNeg);

        InventoryMovement movement = buildMovement(
            product, InventoryMovement.MovementType.SALE,
            quantity, previousStock, newStock,
            saleId, "SALE", "Sale id=" + saleId
        );
        movementDAO.insert(movement);
        return movement;
    }

    @Override
    public InventoryMovement recordPurchaseMovement(int productId, BigDecimal quantity, int purchaseId) {
        Product product = productDAO.findById(productId)
            .orElseThrow(() -> new ValidationException("Product not found id=" + productId));

        BigDecimal previousStock = product.getCurrentStock();
        BigDecimal newStock      = productDAO.updateStock(productId, quantity, true);

        InventoryMovement movement = buildMovement(
            product, InventoryMovement.MovementType.PURCHASE,
            quantity, previousStock, newStock,
            purchaseId, "PURCHASE", "Purchase id=" + purchaseId
        );
        movementDAO.insert(movement);
        return movement;
    }

    @Override
    public InventoryMovement recordReturnMovement(int productId, BigDecimal quantity, int returnId) {
        Product product = productDAO.findById(productId)
            .orElseThrow(() -> new ValidationException("Product not found id=" + productId));

        BigDecimal previousStock = product.getCurrentStock();
        BigDecimal newStock      = productDAO.updateStock(productId, quantity, true);

        InventoryMovement movement = buildMovement(
            product, InventoryMovement.MovementType.RETURN_IN,
            quantity, previousStock, newStock,
            returnId, "RETURN", "Return id=" + returnId
        );
        movementDAO.insert(movement);
        return movement;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private InventoryMovement buildMovement(Product product,
                                            InventoryMovement.MovementType type,
                                            BigDecimal quantity,
                                            BigDecimal previousStock,
                                            BigDecimal newStock,
                                            Integer referenceId,
                                            String referenceType,
                                            String reason) {
        InventoryMovement m = new InventoryMovement();
        m.setProduct(product);
        m.setMovementType(type);
        m.setQuantity(quantity);
        m.setPreviousStock(previousStock);
        m.setNewStock(newStock);
        m.setReferenceId(referenceId);
        m.setReferenceType(referenceType);
        m.setReason(reason);
        m.setUser(SessionContext.getInstance().getCurrentUser());
        return m;
    }
}
