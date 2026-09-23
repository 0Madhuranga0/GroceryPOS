package com.gp.grocerypos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a supplier purchase (goods received).
 * A Purchase has a header record and one or more {@link PurchaseItem} lines.
 * <p>
 * When a purchase is completed:
 * <ol>
 *   <li>The purchase header is saved.</li>
 *   <li>Each PurchaseItem is saved.</li>
 *   <li>Stock is increased for each product.</li>
 *   <li>An InventoryMovement of type PURCHASE is recorded for each item.</li>
 * </ol>
 * All four steps happen inside a single database transaction.
 */
public class Purchase {

    public enum Status { COMPLETED, CANCELLED }

    private int           id;
    private String        purchaseNumber;
    private Supplier      supplier;
    private String        supplierInvoice;
    private User          user;
    private LocalDateTime purchaseDate;
    private BigDecimal    totalAmount;
    private Status        status;
    private String        notes;
    private LocalDateTime createdAt;

    private List<PurchaseItem> items = new ArrayList<>();

    public Purchase() {
        this.status      = Status.COMPLETED;
        this.totalAmount = BigDecimal.ZERO;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int           getId()                    { return id; }
    public void          setId(int id)              { this.id = id; }

    public String        getPurchaseNumber()                         { return purchaseNumber; }
    public void          setPurchaseNumber(String purchaseNumber)    { this.purchaseNumber = purchaseNumber; }

    public Supplier      getSupplier()                    { return supplier; }
    public void          setSupplier(Supplier supplier)   { this.supplier = supplier; }

    public String        getSupplierInvoice()                          { return supplierInvoice; }
    public void          setSupplierInvoice(String supplierInvoice)    { this.supplierInvoice = supplierInvoice; }

    public User          getUser()              { return user; }
    public void          setUser(User user)     { this.user = user; }

    public LocalDateTime getPurchaseDate()                           { return purchaseDate; }
    public void          setPurchaseDate(LocalDateTime purchaseDate) { this.purchaseDate = purchaseDate; }

    public BigDecimal    getTotalAmount()                        { return totalAmount; }
    public void          setTotalAmount(BigDecimal totalAmount)  { this.totalAmount = totalAmount; }

    public Status        getStatus()                { return status; }
    public void          setStatus(Status status)   { this.status = status; }

    public String        getNotes()                 { return notes; }
    public void          setNotes(String notes)     { this.notes = notes; }

    public LocalDateTime getCreatedAt()                          { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    public List<PurchaseItem> getItems()                           { return items; }
    public void               setItems(List<PurchaseItem> items)   { this.items = items; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public void addItem(PurchaseItem item) {
        items.add(item);
        recalculateTotal();
    }

    public void removeItem(PurchaseItem item) {
        items.remove(item);
        recalculateTotal();
    }

    /** Recomputes totalAmount from all item totalCosts. */
    public void recalculateTotal() {
        totalAmount = items.stream()
            .map(PurchaseItem::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getSupplierId() {
        return supplier != null ? supplier.getId() : 0;
    }

    public String getSupplierName() {
        return supplier != null ? supplier.getName() : "";
    }

    public int getUserId() {
        return user != null ? user.getId() : 0;
    }

    public String getUserName() {
        return user != null ? user.getFullName() : "";
    }

    @Override
    public String toString() {
        return "Purchase{number='" + purchaseNumber
             + "', supplier='" + getSupplierName()
             + "', total=" + totalAmount + "}";
    }
}
