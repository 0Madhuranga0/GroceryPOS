package com.gp.grocerypos.model;

import java.math.BigDecimal;

/**
 * Represents a single line item in a supplier purchase.
 * One {@link Purchase} contains one or more PurchaseItems.
 */
public class PurchaseItem {

    private int        id;
    private int        purchaseId;
    private Product    product;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;

    public PurchaseItem() {}

    public PurchaseItem(Product product, BigDecimal quantity, BigDecimal unitCost) {
        this.product   = product;
        this.quantity  = quantity;
        this.unitCost  = unitCost;
        this.totalCost = quantity.multiply(unitCost);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int        getId()                  { return id; }
    public void       setId(int id)            { this.id = id; }

    public int        getPurchaseId()                    { return purchaseId; }
    public void       setPurchaseId(int purchaseId)      { this.purchaseId = purchaseId; }

    public Product    getProduct()                  { return product; }
    public void       setProduct(Product product)   { this.product = product; }

    public BigDecimal getQuantity()                      { return quantity; }
    public void       setQuantity(BigDecimal quantity)   { this.quantity = quantity; }

    public BigDecimal getUnitCost()                      { return unitCost; }
    public void       setUnitCost(BigDecimal unitCost)   { this.unitCost = unitCost; }

    public BigDecimal getTotalCost()                      { return totalCost; }
    public void       setTotalCost(BigDecimal totalCost)  { this.totalCost = totalCost; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public int getProductId() {
        return product != null ? product.getId() : 0;
    }

    public String getProductName() {
        return product != null ? product.getName() : "";
    }

    /** Recalculates totalCost from quantity × unitCost. */
    public void recalculate() {
        if (quantity != null && unitCost != null) {
            totalCost = quantity.multiply(unitCost);
        }
    }

    @Override
    public String toString() {
        return "PurchaseItem{product=" + getProductName()
             + ", qty=" + quantity + ", unitCost=" + unitCost + "}";
    }
}
