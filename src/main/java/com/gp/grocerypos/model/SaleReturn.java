package com.gp.grocerypos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sales return / refund transaction.
 * <p>
 * A return always references an original {@link Sale}.
 * The original sale is never deleted or modified — a new return record
 * is created and the original sale status is set to RETURNED.
 * <p>
 * Named {@code SaleReturn} to avoid conflict with the Java keyword {@code return}.
 */
public class SaleReturn {

    public enum Status { COMPLETED, CANCELLED }

    private int           id;
    private String        returnNumber;
    private Sale          originalSale;
    private User          user;            // who processed the return
    private LocalDateTime returnDate;
    private BigDecimal    totalRefund;
    private String        reason;
    private Status        status;
    private LocalDateTime createdAt;

    private List<ReturnItem> items = new ArrayList<>();

    public SaleReturn() {
        this.status      = Status.COMPLETED;
        this.totalRefund = BigDecimal.ZERO;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int           getId()                { return id; }
    public void          setId(int id)          { this.id = id; }

    public String        getReturnNumber()                         { return returnNumber; }
    public void          setReturnNumber(String returnNumber)      { this.returnNumber = returnNumber; }

    public Sale          getOriginalSale()                     { return originalSale; }
    public void          setOriginalSale(Sale originalSale)    { this.originalSale = originalSale; }

    public User          getUser()              { return user; }
    public void          setUser(User user)     { this.user = user; }

    public LocalDateTime getReturnDate()                         { return returnDate; }
    public void          setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }

    public BigDecimal    getTotalRefund()                          { return totalRefund; }
    public void          setTotalRefund(BigDecimal totalRefund)    { this.totalRefund = totalRefund; }

    public String        getReason()                 { return reason; }
    public void          setReason(String reason)    { this.reason = reason; }

    public Status        getStatus()                { return status; }
    public void          setStatus(Status status)   { this.status = status; }

    public LocalDateTime getCreatedAt()                          { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    public List<ReturnItem> getItems()                           { return items; }
    public void             setItems(List<ReturnItem> items)     { this.items = items; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public int    getSaleId()       { return originalSale != null ? originalSale.getId()            : 0; }
    public String getInvoiceNumber(){ return originalSale != null ? originalSale.getInvoiceNumber() : ""; }
    public int    getUserId()       { return user         != null ? user.getId()                    : 0; }
    public String getUserName()     { return user         != null ? user.getFullName()               : ""; }

    public void addItem(ReturnItem item) {
        items.add(item);
        recalculate();
    }

    /** Recomputes totalRefund from the sum of all item refundAmounts. */
    public void recalculate() {
        totalRefund = items.stream()
            .map(ReturnItem::getRefundAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        return "SaleReturn{number='" + returnNumber
             + "', invoice='" + getInvoiceNumber()
             + "', refund=" + totalRefund + "}";
    }
}
