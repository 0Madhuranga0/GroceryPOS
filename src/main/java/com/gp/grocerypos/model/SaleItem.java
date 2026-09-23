package com.gp.grocerypos.model;

import java.math.BigDecimal;

/**
 * Represents a single line item within a {@link Sale}.
 * All monetary values use {@link BigDecimal}.
 */
public class SaleItem {

    private int        id;
    private int        saleId;
    private Product    product;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;

    public SaleItem() {
        this.discountAmount = BigDecimal.ZERO;
    }

    public SaleItem(Product product, BigDecimal quantity,
                    BigDecimal unitPrice, BigDecimal discountAmount) {
        this.product        = product;
        this.quantity       = quantity;
        this.unitPrice      = unitPrice;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        recalculate();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int        getId()                  { return id; }
    public void       setId(int id)            { this.id = id; }

    public int        getSaleId()                    { return saleId; }
    public void       setSaleId(int saleId)          { this.saleId = saleId; }

    public Product    getProduct()                  { return product; }
    public void       setProduct(Product product)   { this.product = product; }

    public BigDecimal getQuantity()                      { return quantity; }
    public void       setQuantity(BigDecimal quantity)   { this.quantity = quantity; }

    public BigDecimal getUnitPrice()                       { return unitPrice; }
    public void       setUnitPrice(BigDecimal unitPrice)   { this.unitPrice = unitPrice; }

    public BigDecimal getDiscountAmount()                          { return discountAmount; }
    public void       setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    public BigDecimal getTotalPrice()                        { return totalPrice; }
    public void       setTotalPrice(BigDecimal totalPrice)   { this.totalPrice = totalPrice; }

    // ── Convenience helpers ───────────────────────────────────────────────────

    public int    getProductId()   { return product != null ? product.getId()   : 0; }
    public String getProductName() { return product != null ? product.getName() : ""; }
    public String getBarcode()     { return product != null ? product.getBarcode() : ""; }

    /**
     * Recalculates totalPrice = (quantity × unitPrice) − discountAmount.
     * Always call after changing quantity, unitPrice, or discountAmount.
     */
    public void recalculate() {
        if (quantity == null || unitPrice == null) return;
        BigDecimal gross = quantity.multiply(unitPrice);
        BigDecimal disc  = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        totalPrice = gross.subtract(disc).max(BigDecimal.ZERO);
    }

    /** Gross line total before discount (quantity × unit price). */
    public BigDecimal getGrossTotal() {
        if (quantity == null || unitPrice == null) return BigDecimal.ZERO;
        return quantity.multiply(unitPrice);
    }

    @Override
    public String toString() {
        return "SaleItem{product=" + getProductName()
             + ", qty=" + quantity + ", price=" + unitPrice
             + ", total=" + totalPrice + "}";
    }
}
