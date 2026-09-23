package com.gp.grocerypos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a single stock movement event for a product.
 * Every change to product stock — whether from a sale, purchase,
 * return, damage, or manual adjustment — must create one of these records.
 * This is the inventory audit trail.
 */
public class InventoryMovement {

    /**
     * All possible reasons a stock quantity can change.
     * Matches the ENUM defined in the inventory_movements table.
     */
    public enum MovementType {
        PURCHASE,       // stock received from a supplier purchase
        SALE,           // stock reduced by a completed sale
        RETURN_IN,      // stock restored from a sales return (item back to shelf)
        RETURN_OUT,     // stock reduced when a purchase return goes back to supplier
        ADJUSTMENT,     // manual stock correction by admin
        DAMAGE,         // stock written off due to damage / spoilage
        TRANSFER_IN,    // stock received from another location (future use)
        TRANSFER_OUT    // stock sent to another location (future use)
    }

    private int           id;
    private Product       product;        // full product object (lazy — may only have id+name)
    private MovementType  movementType;
    private BigDecimal    quantity;       // always positive; sign is implied by movementType
    private BigDecimal    previousStock;
    private BigDecimal    newStock;
    private Integer       referenceId;   // nullable FK to sales.id or purchases.id
    private String        referenceType; // "SALE", "PURCHASE", "RETURN", "MANUAL"
    private String        reason;
    private User          user;          // who performed the movement
    private LocalDateTime createdAt;

    public InventoryMovement() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int           getId()              { return id; }
    public void          setId(int id)        { this.id = id; }

    public Product       getProduct()                 { return product; }
    public void          setProduct(Product product)  { this.product = product; }

    public MovementType  getMovementType()                       { return movementType; }
    public void          setMovementType(MovementType type)      { this.movementType = type; }

    public BigDecimal    getQuantity()                       { return quantity; }
    public void          setQuantity(BigDecimal quantity)    { this.quantity = quantity; }

    public BigDecimal    getPreviousStock()                           { return previousStock; }
    public void          setPreviousStock(BigDecimal previousStock)   { this.previousStock = previousStock; }

    public BigDecimal    getNewStock()                       { return newStock; }
    public void          setNewStock(BigDecimal newStock)    { this.newStock = newStock; }

    public Integer       getReferenceId()                        { return referenceId; }
    public void          setReferenceId(Integer referenceId)     { this.referenceId = referenceId; }

    public String        getReferenceType()                          { return referenceType; }
    public void          setReferenceType(String referenceType)      { this.referenceType = referenceType; }

    public String        getReason()                 { return reason; }
    public void          setReason(String reason)    { this.reason = reason; }

    public User          getUser()              { return user; }
    public void          setUser(User user)     { this.user = user; }

    public LocalDateTime getCreatedAt()                          { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    // ── Convenience helpers ───────────────────────────────────────────────────

    public int getProductId() { return product != null ? product.getId() : 0; }
    public int getUserId()    { return user    != null ? user.getId()    : 0; }

    /**
     * Returns true if this movement type increases stock.
     */
    public boolean isStockIncrease() {
        return movementType == MovementType.PURCHASE
            || movementType == MovementType.RETURN_IN
            || movementType == MovementType.TRANSFER_IN
            || (movementType == MovementType.ADJUSTMENT
                && newStock != null && previousStock != null
                && newStock.compareTo(previousStock) > 0);
    }

    @Override
    public String toString() {
        return "InventoryMovement{type=" + movementType
             + ", product=" + (product != null ? product.getName() : "?")
             + ", qty=" + quantity + "}";
    }
}
