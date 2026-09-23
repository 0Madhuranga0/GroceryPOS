package com.gp.grocerypos.service;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.dao.ProductDAO;
import com.gp.grocerypos.dao.ProductDAOImpl;
import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.exception.ValidationException;
import com.gp.grocerypos.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductDAO productDAO;

    public ProductServiceImpl() {
        this.productDAO = new ProductDAOImpl();
    }

    public ProductServiceImpl(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    @Override
    public List<Product> getActiveProducts() {
        return productDAO.findAllActive();
    }

    @Override
    public List<Product> getProductsByCategory(int categoryId) {
        return productDAO.findByCategory(categoryId);
    }

    @Override
    public List<Product> getProductsBySupplier(int supplierId) {
        return productDAO.findBySupplier(supplierId);
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) return productDAO.findAllActive();
        return productDAO.search(keyword.trim());
    }

    @Override
    public List<Product> getLowStockProducts() {
        return productDAO.findLowStock();
    }

    @Override
    public List<Product> getExpiringSoonProducts() {
        int days = Integer.parseInt(
            AppConfig.getInstance().get("inventory.expiryWarningDays", "30"));
        return productDAO.findExpiringSoon(days);
    }

    @Override
    public List<Product> getExpiredProducts() {
        return productDAO.findExpired();
    }

    @Override
    public Product getProductById(int id) {
        return productDAO.findById(id)
                .orElseThrow(() -> new POSException(
                        "Product not found id=" + id, "Product not found."));
    }

    @Override
    public Optional<Product> getProductByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) return Optional.empty();
        return productDAO.findByBarcode(barcode.trim());
    }

    @Override
    public int countActiveProducts() {
        return productDAO.countActive();
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @Override
    public Product createProduct(Product product) {
        SessionContext.getInstance().requirePermission("PRODUCT_CREATE");
        validate(product, 0);
        productDAO.insert(product);
        log.info("Product created: '{}' id={}", product.getName(), product.getId());
        return product;
    }

    @Override
    public Product updateProduct(Product product) {
        SessionContext.getInstance().requirePermission("PRODUCT_EDIT");

        // Price changes require an additional permission
        Product existing = getProductById(product.getId());
        if (product.getSellingPrice().compareTo(existing.getSellingPrice()) != 0
                || product.getPurchasePrice().compareTo(existing.getPurchasePrice()) != 0) {
            SessionContext.getInstance().requirePermission("PRODUCT_PRICE_CHANGE");
        }

        validate(product, product.getId());
        productDAO.update(product);
        log.info("Product updated: id={} name='{}'", product.getId(), product.getName());
        return product;
    }

    @Override
    public void deactivateProduct(int id) {
        SessionContext.getInstance().requirePermission("PRODUCT_EDIT");
        productDAO.deactivate(id);
        log.info("Product deactivated id={}", id);
    }

    @Override
    public void activateProduct(int id) {
        SessionContext.getInstance().requirePermission("PRODUCT_EDIT");
        productDAO.activate(id);
        log.info("Product activated id={}", id);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(Product p, int excludeId) {
        List<String> errors = new ArrayList<>();

        if (p.getName() == null || p.getName().isBlank()) {
            errors.add("Product name is required.");
        } else if (p.getName().trim().length() > 150) {
            errors.add("Product name must not exceed 150 characters.");
        }

        if (p.getBarcode() != null && !p.getBarcode().isBlank()) {
            boolean exists = excludeId > 0
                    ? productDAO.barcodeExistsExcluding(p.getBarcode().trim(), excludeId)
                    : productDAO.barcodeExists(p.getBarcode().trim());
            if (exists) {
                errors.add("A product with barcode '" + p.getBarcode().trim() + "' already exists.");
            }
        }

        if (p.getSellingPrice() == null || p.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Selling price must be zero or greater.");
        }

        if (p.getPurchasePrice() == null || p.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Purchase price must be zero or greater.");
        }

        if (p.getCurrentStock() == null || p.getCurrentStock().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Current stock must be zero or greater.");
        }

        if (p.getMinimumStock() == null || p.getMinimumStock().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Minimum stock must be zero or greater.");
        }

        if (p.getUnit() == null) {
            errors.add("Product unit is required.");
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
