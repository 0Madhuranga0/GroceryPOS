package com.gp.grocerypos.service;

import com.gp.grocerypos.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Business logic contract for product management.
 */
public interface ProductService {

    List<Product> getAllProducts();
    List<Product> getActiveProducts();
    List<Product> getProductsByCategory(int categoryId);
    List<Product> getProductsBySupplier(int supplierId);
    List<Product> searchProducts(String keyword);
    List<Product> getLowStockProducts();
    List<Product> getExpiringSoonProducts();
    List<Product> getExpiredProducts();

    Product getProductById(int id);
    Optional<Product> getProductByBarcode(String barcode);

    /**
     * Validates and saves a new product.
     * @throws com.gp.grocerypos.exception.ValidationException if data is invalid
     * @throws com.gp.grocerypos.exception.AuthException if caller lacks permission
     */
    Product createProduct(Product product);

    /**
     * Validates and updates an existing product.
     */
    Product updateProduct(Product product);

    void deactivateProduct(int id);
    void activateProduct(int id);

    int countActiveProducts();
}
