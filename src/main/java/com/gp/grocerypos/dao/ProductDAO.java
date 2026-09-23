package com.gp.grocerypos.dao;

import com.gp.grocerypos.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Data access contract for {@link Product} entities.
 */
public interface ProductDAO {

    Optional<Product> findById(int id);
    Optional<Product> findByBarcode(String barcode);
    List<Product>     findAll();
    List<Product>     findAllActive();
    List<Product>     findByCategory(int categoryId);
    List<Product>     findBySupplier(int supplierId);

    /** Full-text search by name, barcode, or brand (case-insensitive LIKE). */
    List<Product>     search(String keyword);

    /** Returns products where current_stock <= minimum_stock. */
    List<Product>     findLowStock();

    /** Returns products where expiry_date <= CURDATE() + warningDays. */
    List<Product>     findExpiringSoon(int warningDays);

    /** Returns products where expiry_date < CURDATE(). */
    List<Product>     findExpired();

    /** Inserts a new product and returns the generated ID. */
    int  insert(Product product);
    void update(Product product);
    void activate(int id);
    void deactivate(int id);

    /**
     * Atomically updates the stock for a product.
     * Uses optimistic check: fails if current stock would go negative
     * unless allowNegative is true.
     * Returns the new stock value.
     */
    BigDecimal updateStock(int productId, BigDecimal delta, boolean allowNegative);

    /** Directly sets the stock to an absolute value (used for manual adjustments). */
    void setStock(int productId, BigDecimal newStock);

    /** Updates only the purchase price (called when a purchase records a new cost). */
    void updatePurchasePrice(int productId, BigDecimal newPrice);

    boolean barcodeExists(String barcode);
    boolean barcodeExistsExcluding(String barcode, int excludeId);

    /** Total count of active products. */
    int countActive();
}
