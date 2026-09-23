package com.gp.grocerypos.service;

import com.gp.grocerypos.model.InventoryMovement;
import com.gp.grocerypos.model.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Business logic contract for inventory management.
 * <p>
 * Every stock change — sale, purchase, return, damage, or manual adjustment —
 * must go through this service so that an {@link InventoryMovement} audit
 * record is always created alongside the stock update.
 * <p>
 * The service enforces:
 * <ul>
 *   <li>Permission checks before any mutation</li>
 *   <li>The "allow negative stock" setting from the database</li>
 *   <li>Atomic stock update + movement insert (caller must supply a
 *       connection when inside a larger transaction)</li>
 * </ul>
 */
public interface InventoryService {

    // ── Stock queries ─────────────────────────────────────────────────────────

    List<Product> getLowStockProducts();
    List<Product> getExpiringSoonProducts();
    List<Product> getExpiredProducts();

    // ── Movement queries ──────────────────────────────────────────────────────

    List<InventoryMovement> getMovementsByProduct(int productId);
    List<InventoryMovement> getMovementsByDateRange(LocalDate from, LocalDate to);
    List<InventoryMovement> getRecentMovements(int limit);

    // ── Manual stock adjustment (admin only) ──────────────────────────────────

    /**
     * Records a manual stock adjustment.
     * <p>
     * The {@code newQuantity} is the absolute stock level the admin wants
     * to set. The difference between current and new stock is calculated
     * internally and recorded as an ADJUSTMENT movement.
     *
     * @param productId   the product to adjust
     * @param newQuantity the desired absolute stock level (must be >= 0)
     * @param reason      mandatory reason for the audit trail
     * @throws com.gp.grocerypos.exception.ValidationException if newQuantity < 0 or reason blank
     * @throws com.gp.grocerypos.exception.AuthException       if caller lacks INVENTORY_ADJUST
     */
    InventoryMovement adjustStock(int productId, BigDecimal newQuantity, String reason);

    /**
     * Records a damage/write-off event (reduces stock).
     *
     * @param productId the product that was damaged
     * @param quantity  the quantity to write off (positive value)
     * @param reason    mandatory reason for the audit trail
     */
    InventoryMovement recordDamage(int productId, BigDecimal quantity, String reason);

    // ── Internal methods (called by SaleService / PurchaseService) ────────────

    /**
     * Reduces stock for a sale line item and records a SALE movement.
     * Intended to be called within a sale transaction — no permission check.
     *
     * @param productId  product being sold
     * @param quantity   quantity sold (positive)
     * @param saleId     the sale this movement references
     * @return the recorded InventoryMovement
     */
    InventoryMovement recordSaleMovement(int productId, BigDecimal quantity, int saleId);

    /**
     * Increases stock for a purchase line item and records a PURCHASE movement.
     * Intended to be called within a purchase transaction — no permission check.
     *
     * @param productId  product received
     * @param quantity   quantity received (positive)
     * @param purchaseId the purchase this movement references
     * @return the recorded InventoryMovement
     */
    InventoryMovement recordPurchaseMovement(int productId, BigDecimal quantity, int purchaseId);

    /**
     * Restores stock for a return and records a RETURN_IN movement.
     *
     * @param productId product being returned to stock
     * @param quantity  quantity returned (positive)
     * @param returnId  the return transaction this movement references
     * @return the recorded InventoryMovement
     */
    InventoryMovement recordReturnMovement(int productId, BigDecimal quantity, int returnId);
}
