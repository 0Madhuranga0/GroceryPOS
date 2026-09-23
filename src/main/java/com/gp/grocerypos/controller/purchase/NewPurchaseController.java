package com.gp.grocerypos.controller.purchase;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.model.*;
import com.gp.grocerypos.service.*;
import com.gp.grocerypos.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the New Purchase entry screen.
 * The cashier/admin selects a supplier, adds products with quantity and
 * unit cost, then saves the purchase transaction.
 */
public class NewPurchaseController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(NewPurchaseController.class);

    // ── Header fields ─────────────────────────────────────────────────────────
    @FXML private ComboBox<Supplier>  supplierCombo;
    @FXML private TextField           supplierInvoiceField;
    @FXML private TextArea            notesField;

    // ── Item entry row ────────────────────────────────────────────────────────
    @FXML private TextField           barcodeField;
    @FXML private Label               productNameLabel;
    @FXML private TextField           qtyField;
    @FXML private TextField           unitCostField;
    @FXML private Button              btnAddItem;

    // ── Cart table ────────────────────────────────────────────────────────────
    @FXML private TableView<PurchaseItem>          cartTable;
    @FXML private TableColumn<PurchaseItem, String> cartColProduct;
    @FXML private TableColumn<PurchaseItem, String> cartColQty;
    @FXML private TableColumn<PurchaseItem, String> cartColUnit;
    @FXML private TableColumn<PurchaseItem, String> cartColUnitCost;
    @FXML private TableColumn<PurchaseItem, String> cartColTotal;

    // ── Totals + actions ──────────────────────────────────────────────────────
    @FXML private Label  totalLabel;
    @FXML private Label  errorLabel;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final PurchaseService purchaseService = new PurchaseServiceImpl();
    private final SupplierService supplierService = new SupplierServiceImpl();
    private final ProductService  productService  = new ProductServiceImpl();
    private final String          currency        = AppConfig.getInstance().getCurrencySymbol();

    private final ObservableList<PurchaseItem> cartItems = FXCollections.observableArrayList();
    private Product currentProduct;
    private boolean saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSupplierCombo();
        setupCartTable();
        setupInputFilters();
        errorLabel.setVisible(false);

        // Barcode lookup on Enter
        barcodeField.setOnAction(e -> handleBarcodeSearch());
    }

    public boolean isSaved() { return saved; }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupSupplierCombo() {
        try {
            supplierService.getActiveSuppliers()
                .forEach(s -> supplierCombo.getItems().add(s));
            supplierCombo.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Supplier s, boolean empty) {
                    super.updateItem(s, empty);
                    setText(s == null ? "-- Select Supplier --" : s.getName());
                }
            });
            supplierCombo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Supplier s, boolean empty) {
                    super.updateItem(s, empty);
                    setText(s == null ? "" : s.getName());
                }
            });
        } catch (Exception e) {
            log.warn("Failed to load suppliers: {}", e.getMessage());
        }
    }

    private void setupCartTable() {
        cartColProduct.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getProductName()));
        cartColQty.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getQuantity()
                .stripTrailingZeros().toPlainString()));
        cartColUnit.setCellValueFactory(d -> {
            Product p = d.getValue().getProduct();
            return new SimpleStringProperty(p != null && p.getUnit() != null
                ? p.getUnit().getLabel() : "");
        });
        cartColUnitCost.setCellValueFactory(d ->
            new SimpleStringProperty(currency + " " +
                String.format("%,.2f", d.getValue().getUnitCost())));
        cartColTotal.setCellValueFactory(d ->
            new SimpleStringProperty(currency + " " +
                String.format("%,.2f", d.getValue().getTotalCost())));

        cartTable.setItems(cartItems);
        cartItems.addListener(
            (javafx.collections.ListChangeListener<PurchaseItem>) c -> updateTotal());
    }

    private void setupInputFilters() {
        qtyField.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) qtyField.setText(old);
        });
        unitCostField.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) unitCostField.setText(old);
        });
    }

    // ── Barcode / product search ───────────────────────────────────────────────

    @FXML
    private void handleBarcodeSearch() {
        String barcode = barcodeField.getText().trim();
        if (barcode.isBlank()) return;

        try {
            currentProduct = productService.getProductByBarcode(barcode)
                .orElse(null);
            if (currentProduct == null) {
                productNameLabel.setText("Product not found.");
                productNameLabel.setStyle("-fx-text-fill: #c62828;");
                currentProduct = null;
            } else {
                productNameLabel.setText(currentProduct.getName());
                productNameLabel.setStyle("-fx-text-fill: #2e7d32;");
                // Pre-fill unit cost with current purchase price
                unitCostField.setText(
                    currentProduct.getPurchasePrice().stripTrailingZeros().toPlainString());
                qtyField.setText("1");
                qtyField.requestFocus();
            }
        } catch (Exception e) {
            productNameLabel.setText("Error searching product.");
            productNameLabel.setStyle("-fx-text-fill: #c62828;");
        }
    }

    @FXML
    private void handleSearchProduct() {
        // Text-based product search dialog fallback
        String name = barcodeField.getText().trim();
        if (name.isBlank()) {
            AlertUtil.showWarning("Search", "Enter a barcode or product name to search.");
            return;
        }
        java.util.List<Product> results = productService.searchProducts(name);
        if (results.isEmpty()) {
            productNameLabel.setText("No products found.");
            productNameLabel.setStyle("-fx-text-fill: #c62828;");
            return;
        }
        if (results.size() == 1) {
            currentProduct = results.get(0);
            barcodeField.setText(currentProduct.getBarcode() != null
                ? currentProduct.getBarcode() : currentProduct.getName());
            productNameLabel.setText(currentProduct.getName());
            productNameLabel.setStyle("-fx-text-fill: #2e7d32;");
            unitCostField.setText(
                currentProduct.getPurchasePrice().stripTrailingZeros().toPlainString());
            qtyField.setText("1");
        } else {
            // Let the user pick from a choice dialog
            ChoiceDialog<Product> dialog = new ChoiceDialog<>(results.get(0), results);
            dialog.setTitle("Select Product");
            dialog.setHeaderText("Multiple products found. Please select one:");
            dialog.setContentText("Product:");
            dialog.showAndWait().ifPresent(p -> {
                currentProduct = p;
                barcodeField.setText(p.getBarcode() != null ? p.getBarcode() : p.getName());
                productNameLabel.setText(p.getName());
                productNameLabel.setStyle("-fx-text-fill: #2e7d32;");
                unitCostField.setText(
                    p.getPurchasePrice().stripTrailingZeros().toPlainString());
                qtyField.setText("1");
            });
        }
    }

    // ── Cart management ───────────────────────────────────────────────────────

    @FXML
    private void handleAddItem() {
        clearError();
        if (currentProduct == null) {
            showError("Please search for a product first.");
            return;
        }
        BigDecimal qty, cost;
        try {
            qty  = new BigDecimal(qtyField.getText().trim());
            cost = new BigDecimal(unitCostField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Please enter valid quantity and unit cost.");
            return;
        }
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            showError("Quantity must be greater than zero.");
            return;
        }
        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            showError("Unit cost must be zero or greater.");
            return;
        }

        // Check if this product is already in the cart — merge if so
        for (PurchaseItem existing : cartItems) {
            if (existing.getProductId() == currentProduct.getId()) {
                existing.setQuantity(existing.getQuantity().add(qty));
                existing.setUnitCost(cost);
                existing.recalculate();
                cartTable.refresh();
                updateTotal();
                resetItemEntry();
                return;
            }
        }

        PurchaseItem item = new PurchaseItem(currentProduct, qty, cost);
        cartItems.add(item);
        resetItemEntry();
    }

    @FXML
    private void handleRemoveItem() {
        PurchaseItem sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.showWarning("No Selection", "Please select an item to remove.");
            return;
        }
        cartItems.remove(sel);
        updateTotal();
    }

    // ── Save / Cancel ─────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        clearError();
        Purchase purchase = new Purchase();
        purchase.setSupplier(supplierCombo.getValue());
        purchase.setSupplierInvoice(supplierInvoiceField.getText().trim());
        purchase.setNotes(notesField.getText().trim());
        purchase.setItems(new java.util.ArrayList<>(cartItems));

        try {
            purchaseService.savePurchase(purchase);
            saved = true;
            AlertUtil.showInfo("Purchase Saved",
                "Purchase " + purchase.getPurchaseNumber()
                + " saved successfully.\nTotal: "
                + currency + " " + String.format("%,.2f", purchase.getTotalAmount()));
            closeWindow();
        } catch (POSException e) {
            showError(e.getUserMessage());
        } catch (Exception e) {
            showError("An unexpected error occurred. Please try again.");
            log.error("Save purchase error: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleCancel() {
        if (!cartItems.isEmpty()) {
            if (!AlertUtil.showConfirm("Discard Purchase",
                    "Discard this purchase? All entered items will be lost.")) return;
        }
        closeWindow();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void resetItemEntry() {
        barcodeField.clear();
        qtyField.clear();
        unitCostField.clear();
        productNameLabel.setText("");
        currentProduct = null;
        barcodeField.requestFocus();
    }

    private void updateTotal() {
        BigDecimal total = cartItems.stream()
            .map(PurchaseItem::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalLabel.setText(currency + " " + String.format("%,.2f", total));
    }

    private void closeWindow() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }
}
