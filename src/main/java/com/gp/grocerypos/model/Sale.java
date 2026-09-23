package com.gp.grocerypos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a completed sale transaction.
 * <p>
 * The sale lifecycle:
 * <ol>
 *   <li>Cart is built in the POS screen (in memory only, no DB writes yet).</li>
 *   <li>{@link com.gp.grocerypos.service.SaleService#completeSale} is called.</li>
 *   <li>A database transaction atomically saves the sale header, items,
 *       payment, reduces stock, and records inventory movements.</li>
 * </ol>
 */
public class Sale {

    public enum Status { COMPLETED, VOIDED, RETURNED }

    private int           id;
    private String        invoiceNumber;
    private Customer      customer;      // nullable — walk-in customers
    private User          user;          // cashier who processed the sale
    private LocalDateTime saleDate;
    private BigDecimal    subtotal;
    private BigDecimal    discountAmount;
    private BigDecimal    taxAmount;
    private BigDecimal    grandTotal;
    private String        paymentMethod;
    private BigDecimal    cashReceived;
    private BigDecimal    changeAmount;
    private Status        status;
    private String        notes;
    private LocalDateTime createdAt;

    private List<SaleItem> items = new ArrayList<>();

    public Sale() {
        this.subtotal       = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.taxAmount      = BigDecimal.ZERO;
        this.grandTotal     = BigDecimal.ZERO;
        this.cashReceived   = BigDecimal.ZERO;
        this.changeAmount   = BigDecimal.ZERO;
        this.status         = Status.COMPLETED;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int           getId()                { return id; }
    public void          setId(int id)          { this.id = id; }

    public String        getInvoiceNumber()                        { return invoiceNumber; }
    public void          setInvoiceNumber(String invoiceNumber)    { this.invoiceNumber = invoiceNumber; }

    public Customer      getCustomer()                    { return customer; }
    public void          setCustomer(Customer customer)   { this.customer = customer; }

    public User          getUser()              { return user; }
    public void          setUser(User user)     { this.user = user; }

    public LocalDateTime getSaleDate()                         { return saleDate; }
    public void          setSaleDate(LocalDateTime saleDate)   { this.saleDate = saleDate; }

    public BigDecimal    getSubtotal()                       { return subtotal; }
    public void          setSubtotal(BigDecimal subtotal)    { this.subtotal = subtotal; }

    public BigDecimal    getDiscountAmount()                           { return discountAmount; }
    public void          setDiscountAmount(BigDecimal discountAmount)  { this.discountAmount = discountAmount; }

    public BigDecimal    getTaxAmount()                        { return taxAmount; }
    public void          setTaxAmount(BigDecimal taxAmount)    { this.taxAmount = taxAmount; }

    public BigDecimal    getGrandTotal()                         { return grandTotal; }
    public void          setGrandTotal(BigDecimal grandTotal)    { this.grandTotal = grandTotal; }

    public String        getPaymentMethod()                          { return paymentMethod; }
    public void          setPaymentMethod(String paymentMethod)      { this.paymentMethod = paymentMethod; }

    public BigDecimal    getCashReceived()                         { return cashReceived; }
    public void          setCashReceived(BigDecimal cashReceived)  { this.cashReceived = cashReceived; }

    public BigDecimal    getChangeAmount()                         { return changeAmount; }
    public void          setChangeAmount(BigDecimal changeAmount)  { this.changeAmount = changeAmount; }

    public Status        getStatus()                { return status; }
    public void          setStatus(Status status)   { this.status = status; }

    public String        getNotes()                 { return notes; }
    public void          setNotes(String notes)     { this.notes = notes; }

    public LocalDateTime getCreatedAt()                          { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    public List<SaleItem> getItems()                           { return items; }
    public void           setItems(List<SaleItem> items)       { this.items = items; }

    // ── Convenience helpers ───────────────────────────────────────────────────

    public int    getCustomerId()   { return customer != null ? customer.getId()   : 0; }
    public String getCustomerName() { return customer != null ? customer.getName() : "Walk-in"; }
    public int    getUserId()       { return user     != null ? user.getId()       : 0; }
    public String getUserName()     { return user     != null ? user.getFullName() : ""; }

    /**
     * Recalculates subtotal, discount, tax and grand total from the items list.
     * Call this whenever items change or a discount/tax rate is applied.
     *
     * @param taxRate   tax rate as a decimal fraction (e.g. 0.15 for 15%), 0 if tax disabled
     * @param taxEnabled whether to apply tax to the grand total
     */
    public void recalculateTotals(BigDecimal taxRate, boolean taxEnabled) {
        // Subtotal = sum of each item's gross (before item-level discounts)
        subtotal = items.stream()
            .map(SaleItem::getGrossTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total item-level discounts
        discountAmount = items.stream()
            .map(SaleItem::getDiscountAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal afterDiscount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        if (taxEnabled && taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount  = afterDiscount.multiply(taxRate)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            taxAmount = BigDecimal.ZERO;
        }

        grandTotal = afterDiscount.add(taxAmount);

        // Change calculation
        if (cashReceived != null && cashReceived.compareTo(grandTotal) >= 0) {
            changeAmount = cashReceived.subtract(grandTotal);
        } else {
            changeAmount = BigDecimal.ZERO;
        }
    }

    @Override
    public String toString() {
        return "Sale{invoice='" + invoiceNumber
             + "', total=" + grandTotal
             + ", customer=" + getCustomerName() + "}";
    }
}
