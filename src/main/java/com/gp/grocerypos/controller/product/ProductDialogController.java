package com.gp.grocerypos.controller.product;

import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.model.Category;
import com.gp.grocerypos.model.Product;
import com.gp.grocerypos.model.Supplier;
import com.gp.grocerypos.service.CategoryService;
import com.gp.grocerypos.service.CategoryServiceImpl;
import com.gp.grocerypos.service.ProductService;
import com.gp.grocerypos.service.ProductServiceImpl;
import com.gp.grocerypos.service.SupplierService;
import com.gp.grocerypos.service.SupplierServiceImpl;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Product add/edit modal dialog.
 * Handles all product fields including category and supplier dropdowns.
 */
public class ProductDialogController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ProductDialogController.class);

    // ── FXML fields ───────────────────────────────────────────────────────────
    @FXML private Label                     titleLabel;
    @FXML private Label                     errorLabel;

    // Basic info
    @FXML private TextField                 barcodeField;
    @FXML private TextField                 nameField;
    @FXML private TextField                 brandField;
    @FXML private TextArea                  descriptionField;

    // Classification
    @FXML private ComboBox<Category>        categoryCombo;
    @FXML private ComboBox<Supplier>        supplierCombo;
    @FXML private ComboBox<Product.Unit>    unitCombo;
    @FXML private ComboBox<String>          statusCombo;

    // Pricing
    @FXML private TextField                 purchasePriceField;
    @FXML private TextField                 sellingPriceField;
    @FXML private TextField                 wholesalePriceField;

    // Stock
    @FXML private TextField                 currentStockField;
    @FXML private TextField                 minimumStockField;

    // Expiry
    @FXML private DatePicker                expiryDatePicker;

    @FXML private Button                    btnSave;
    @FXML private Button                    btnCancel;

    // ── Services ──────────────────────────────────────────────────────────────
    private final ProductService  productService  = new ProductServiceImpl();
    private final CategoryService categoryService = new CategoryServiceImpl();
    private final SupplierService supplierService = new SupplierServiceImpl();

    private Product editingProduct;
    private boolean saved = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        errorLabel.setVisible(false);

        // Populate unit combo
        unitCombo.getItems().addAll(Product.Unit.values());
        unitCombo.setValue(Product.Unit.PIECE);

        // Status combo
        statusCombo.getItems().addAll("ACTIVE", "INACTIVE");
        statusCombo.setValue("ACTIVE");

        // Load categories and suppliers
        loadCombos();

        // Numeric-only listeners for price / stock fields
        addDecimalFilter(purchasePriceField);
        addDecimalFilter(sellingPriceField);
        addDecimalFilter(wholesalePriceField);
        addDecimalFilter(currentStockField);
        addDecimalFilter(minimumStockField);
    }

    // ── Called by opener ──────────────────────────────────────────────────────

    public void setProduct(Product product) {
        this.editingProduct = product;
        if (product == null) {
            titleLabel.setText("Add Product");
            currentStockField.setText("0");
            minimumStockField.setText("0");
            purchasePriceField.setText("0.00");
            sellingPriceField.setText("0.00");
            wholesalePriceField.setText("0.00");
        } else {
            titleLabel.setText("Edit Product");
            barcodeField.setText(orEmpty(product.getBarcode()));
            nameField.setText(orEmpty(product.getName()));
            brandField.setText(orEmpty(product.getBrand()));
            descriptionField.setText(orEmpty(product.getDescription()));

            if (product.getUnit() != null) unitCombo.setValue(product.getUnit());
            statusCombo.setValue(product.getStatus().name());

            purchasePriceField.setText(format(product.getPurchasePrice()));
            sellingPriceField.setText(format(product.getSellingPrice()));
            wholesalePriceField.setText(format(product.getWholesalePrice()));
            currentStockField.setText(format(product.getCurrentStock()));
            minimumStockField.setText(format(product.getMinimumStock()));

            if (product.getExpiryDate() != null) {
                expiryDatePicker.setValue(product.getExpiryDate());
            }

            // Select category in combo
            if (product.getCategory() != null) {
                categoryCombo.getItems().stream()
                    .filter(c -> c != null && c.getId() == product.getCategoryId())
                    .findFirst()
                    .ifPresent(categoryCombo::setValue);
            }

            // Select supplier in combo
            if (product.getSupplier() != null) {
                supplierCombo.getItems().stream()
                    .filter(s -> s != null && s.getId() == product.getSupplierId())
                    .findFirst()
                    .ifPresent(supplierCombo::setValue);
            }
        }
    }

    public boolean isSaved() { return saved; }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        clearError();
        try {
            Product p = buildProduct();
            if (editingProduct == null) {
                productService.createProduct(p);
            } else {
                productService.updateProduct(p);
            }
            saved = true;
            closeDialog();
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void handleCancel() { closeDialog(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void loadCombos() {
        try {
            // Add a blank "none" option at the top of each combo
            List<Category> categories = categoryService.getActiveCategories();
            categoryCombo.getItems().clear();
            categoryCombo.getItems().add(null);           // "none" option
            categoryCombo.getItems().addAll(categories);
            categoryCombo.setButtonCell(new NullableListCell<>("-- Select Category --"));
            categoryCombo.setCellFactory(lv -> new NullableListCell<>("-- Select Category --"));

            List<Supplier> suppliers = supplierService.getActiveSuppliers();
            supplierCombo.getItems().clear();
            supplierCombo.getItems().add(null);
            supplierCombo.getItems().addAll(suppliers);
            supplierCombo.setButtonCell(new NullableListCell<>("-- Select Supplier --"));
            supplierCombo.setCellFactory(lv -> new NullableListCell<>("-- Select Supplier --"));

        } catch (Exception e) {
            log.warn("Failed to load category/supplier combos: {}", e.getMessage());
        }
    }

    private Product buildProduct() {
        Product p = editingProduct != null ? editingProduct : new Product();

        p.setBarcode(emptyToNull(barcodeField.getText()));
        p.setName(nameField.getText());
        p.setBrand(emptyToNull(brandField.getText()));
        p.setDescription(emptyToNull(descriptionField.getText()));
        p.setUnit(unitCombo.getValue());
        p.setStatus(Product.Status.valueOf(statusCombo.getValue()));
        p.setCategory(categoryCombo.getValue());
        p.setSupplier(supplierCombo.getValue());

        p.setPurchasePrice(parseBD(purchasePriceField.getText(), BigDecimal.ZERO));
        p.setSellingPrice(parseBD(sellingPriceField.getText(), BigDecimal.ZERO));
        p.setWholesalePrice(parseBD(wholesalePriceField.getText(), BigDecimal.ZERO));
        p.setCurrentStock(parseBD(currentStockField.getText(), BigDecimal.ZERO));
        p.setMinimumStock(parseBD(minimumStockField.getText(), BigDecimal.ZERO));

        LocalDate expiry = expiryDatePicker.getValue();
        p.setExpiryDate(expiry);

        return p;
    }

    private void closeDialog() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    private void showError(Throwable e) {
        String msg = (e instanceof POSException pe)
                ? pe.getUserMessage() : "An error occurred. Please try again.";
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        log.error("Product dialog error: {}", e.getMessage(), e);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    /** Restricts a TextField to valid decimal input only. */
    private void addDecimalFilter(TextField field) {
        field.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) {
                field.setText(old);
            }
        });
    }

    private BigDecimal parseBD(String text, BigDecimal fallback) {
        try {
            return (text == null || text.isBlank()) ? fallback : new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String format(BigDecimal val) {
        return val != null ? val.stripTrailingZeros().toPlainString() : "0";
    }

    private String orEmpty(String s)  { return s != null ? s : ""; }
    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // ── Custom combo cell that displays placeholder text for null items ────────
    private static class NullableListCell<T> extends ListCell<T> {
        private final String placeholder;
        NullableListCell(String placeholder) { this.placeholder = placeholder; }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(placeholder);
            } else {
                setText(item.toString());
            }
        }
    }
}
