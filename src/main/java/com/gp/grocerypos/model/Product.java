package com.gp.grocerypos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a product in the grocery store inventory.
 * <p>
 * All monetary values use {@link BigDecimal} — never float or double.
 * Stock quantities use BigDecimal to support fractional units (kg, litre).
 */
public class Product {

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum Status { ACTIVE, INACTIVE }

    public enum Unit {
        PIECE("Piece"),
        KG("Kilogram"),
        GRAM("Gram"),
        LITER("Liter"),
        ML("Milliliter"),
        PACK("Pack"),
        BOX("Box"),
        BOTTLE("Bottle");

        private final String label;
        Unit(String label) { this.label = label; }
        public String getLabel() { return label; }

        @Override
        public String toString() { return label; }
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private int           id;
    private String        barcode;
    private String        name;
    private String        description;
    private Category      category;
    private Supplier      supplier;
    private String        brand;
    private Unit          unit;
    private BigDecimal    purchasePrice;
    private BigDecimal    sellingPrice;
    private BigDecimal    wholesalePrice;
    private BigDecimal    currentStock;
    private BigDecimal    minimumStock;
    private LocalDate     expiryDate;
    private Status        status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {
        this.unit          = Unit.PIECE;
        this.purchasePrice  = BigDecimal.ZERO;
        this.sellingPrice   = BigDecimal.ZERO;
        this.wholesalePrice = BigDecimal.ZERO;
        this.currentStock   = BigDecimal.ZERO;
        this.minimumStock   = BigDecimal.ZERO;
        this.status         = Status.ACTIVE;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int           getId()                { return id; }
    public void          setId(int id)          { this.id = id; }

    public String        getBarcode()                   { return barcode; }
    public void          setBarcode(String barcode)     { this.barcode = barcode; }

    public String        getName()                  { return name; }
    public void          setName(String name)       { this.name = name; }

    public String        getDescription()                    { return description; }
    public void          setDescription(String description)  { this.description = description; }

    public Category      getCategory()                    { return category; }
    public void          setCategory(Category category)   { this.category = category; }

    public Supplier      getSupplier()                    { return supplier; }
    public void          setSupplier(Supplier supplier)   { this.supplier = supplier; }

    public String        getBrand()                 { return brand; }
    public void          setBrand(String brand)     { this.brand = brand; }

    public Unit          getUnit()              { return unit; }
    public void          setUnit(Unit unit)     { this.unit = unit; }

    public BigDecimal    getPurchasePrice()                        { return purchasePrice; }
    public void          setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal    getSellingPrice()                         { return sellingPrice; }
    public void          setSellingPrice(BigDecimal sellingPrice)  { this.sellingPrice = sellingPrice; }

    public BigDecimal    getWholesalePrice()                           { return wholesalePrice; }
    public void          setWholesalePrice(BigDecimal wholesalePrice)  { this.wholesalePrice = wholesalePrice; }

    public BigDecimal    getCurrentStock()                         { return currentStock; }
    public void          setCurrentStock(BigDecimal currentStock)  { this.currentStock = currentStock; }

    public BigDecimal    getMinimumStock()                         { return minimumStock; }
    public void          setMinimumStock(BigDecimal minimumStock)  { this.minimumStock = minimumStock; }

    public LocalDate     getExpiryDate()                       { return expiryDate; }
    public void          setExpiryDate(LocalDate expiryDate)   { this.expiryDate = expiryDate; }

    public Status        getStatus()                { return status; }
    public void          setStatus(Status status)   { this.status = status; }

    public LocalDateTime getCreatedAt()                          { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                          { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime updatedAt)   { this.updatedAt = updatedAt; }

    // ── Convenience helpers ───────────────────────────────────────────────────

    public boolean isActive() {
        return Status.ACTIVE.equals(status);
    }

    /**
     * Returns true when current stock is at or below the minimum stock level.
     */
    public boolean isLowStock() {
        return currentStock != null && minimumStock != null
                && currentStock.compareTo(minimumStock) <= 0;
    }

    /**
     * Returns true when the product has no stock remaining.
     */
    public boolean isOutOfStock() {
        return currentStock == null || currentStock.compareTo(BigDecimal.ZERO) <= 0;
    }

    public int getCategoryId() {
        return category != null ? category.getId() : 0;
    }

    public String getCategoryName() {
        return category != null ? category.getName() : "";
    }

    public int getSupplierId() {
        return supplier != null ? supplier.getId() : 0;
    }

    public String getSupplierName() {
        return supplier != null ? supplier.getName() : "";
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", barcode='" + barcode + "', name='" + name + "'}";
    }
}
