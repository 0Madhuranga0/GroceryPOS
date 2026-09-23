package com.gp.grocerypos.model;

import java.math.BigDecimal;

/**
 * Represents a single product line within a {@link SaleReturn}.
 * Links back to the original {@link SaleItem} so quantities can be validated
 * against what was originally sold.
 */
public class ReturnItem {

    private int        id;
    private int        returnId;
    private SaleItem   originalSaleItem;  // the line item from the original sale
    private Product    product;
    private BigDecimal quantity;          // quantity being returned (must be ≤ original qty)
    private BigDecimal unitPrice;         // price at time of original sale
    private BigDecimal refundAmount;      // quantity × unitPrice
    private boolean    restock;           // true = put back into inventory

    public ReturnItem() {
        this.restock = true;
    }

    public ReturnItem(SaleItem saleItem, BigDecimal quantity, boolean restock) {
        this.originalSaleItem = saleItem;
        this.product          = saleItem.getProduct();
        this.quantity         = quantity;
        this.unitPrice        = saleItem.getUnitPrice();
        this.restock          = restock;
        recalculate();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int        getId()                  { return id; }
    public void       setId(int id)            { this.id = id; }

    public int        getReturnId()                    { return returnId; }
    public void       setReturnId(int returnId)        { this.returnId = returnId; }

    public SaleItem   getOriginalSaleItem()                            { return originalSaleItem; }
    public void       setOriginalSaleItem(SaleItem originalSaleItem)   { this.originalSaleItem = originalSaleItem; }

    public Product    getProduct()                  { return product; }
    public void       setProduct(Product product)   { this.product = product; }

    public BigDecimal getQuantity()                      { return quantity; }
    public void       setQuantity(BigDecimal quantity)   { this.quantity = quantity; }

    public BigDecimal getUnitPrice()                       { return unitPrice; }
    public void       setUnitPrice(BigDecimal unitPrice)   { this.unitPrice = unitPrice; }

    public BigDecimal getRefundAmount()                          { return refundAmount; }
    public void       setRefundAmount(BigDecimal refundAmount)   { this.refundAmount = refundAmount; }

    public boolean    isRestock()                  { return restock; }
    public void       setRestock(boolean restock)  { this.restock = restock; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public int    getProductId()       { return product != null ? product.getId()    : 0; }
    public String getProductName()     { return product != null ? product.getName()  : ""; }
    public int    getSaleItemId()      {
        return originalSaleItem != null ? originalSaleItem.getId() : 0;
    }

    public void recalculate() {
        if (quantity != null && unitPrice != null) {
            refundAmount = quantity.multiply(unitPrice);
        }
    }

    @Override
    public String toString() {
        return "ReturnItem{product=" + getProductName()
             + ", qty=" + quantity + ", refund=" + refundAmount + "}";
    }
}
