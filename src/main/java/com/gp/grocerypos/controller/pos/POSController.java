package com.gp.grocerypos.controller.pos;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.dao.SettingDAO;
import com.gp.grocerypos.dao.SettingDAOImpl;
import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.exception.StockException;
import com.gp.grocerypos.model.*;
import com.gp.grocerypos.service.*;
import com.gp.grocerypos.util.AlertUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the POS Billing screen.
 * <p>
 * Workflow:
 * <ol>
 *   <li>Barcode field is auto-focused — scanner sends barcode + ENTER.</li>
 *   <li>Product is looked up and added to the cart.</li>
 *   <li>Cart totals update in real time.</li>
 *   <li>Cashier presses F10 or clicks Pay to open the Payment dialog.</li>
 *   <li>On payment confirmation, SaleService.completeSale() is called.</li>
 *   <li>Receipt summary is displayed and cart is cleared.</li>
 * </ol>
 * The cashier can operate entirely by keyboard:
 *   ENTER on barcode field = add product,
 *   F10 = open payment,
 *   ESC = cancel / clear cart.
 */
public class POSController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(POSController.class);

    // ── FXML fields ───────────────────────────────────────────────────────────
    // Header
    @FXML private Label  lblCashier;
    @FXML private Label  lblDateTime;

    // Customer area
    @FXML private TextField       customerSearchField;
    @FXML private Label           lblCustomerName;
    @FXML private Button          btnClearCustomer;

    // Barcode / product entry
    @FXML private TextField  barcodeField;
    @FXML private Label      lblProductInfo;

    // Cart table
    @FXML private TableView<SaleItem>           cartTable;
    @FXML private TableColumn<SaleItem, String>  colBarcode;
    @FXML private TableColumn<SaleItem, String>  colProductName;
    @FXML private TableColumn<SaleItem, String>  colQty;
    @FXML private TableColumn<SaleItem, String>  colUnitPrice;
    @FXML private TableColumn<SaleItem, String>  colDiscount;
    @FXML private TableColumn<SaleItem, String>  colTotal;

    // Totals panel
    @FXML private Label  lblSubtotal;
    @FXML private Label  lblDiscount;
    @FXML private Label  lblTax;
    @FXML private javafx.scene.layout.HBox lblTaxRow;
    @FXML private Label  lblGrandTotal;

    // Action buttons
    @FXML private Button btnPay;
    @FXML private Button btnRemove;
    @FXML private Button btnClearCart;

    // Status / message
    @FXML private Label  lblStatus;
    @FXML private Label  lblLastSale;

    // ── Services ──────────────────────────────────────────────────────────────
    private final ProductService  productService  = new ProductServiceImpl();
    private final CustomerService customerService = new CustomerServiceImpl();
    private final SaleService     saleService     = new SaleServiceImpl();
    private final SettingDAO      settingDAO      = new SettingDAOImpl();

    private final String  currency    = AppConfig.getInstance().getCurrencySymbol();
    private final boolean taxEnabled;
    private final BigDecimal taxRate;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();
    private Customer selectedCustomer = null;

    // ── Constructor (load tax settings once) ──────────────────────────────────
    {
        taxEnabled = Boolean.parseBoolean(settingDAO.getValue("tax_enabled", "false"));
        BigDecimal rate = BigDecimal.ZERO;
        if (taxEnabled) {
            try {
                rate = new BigDecimal(settingDAO.getValue("tax_rate", "0"))
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                log.warn("Invalid tax_rate setting");
            }
        }
        taxRate = rate;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupClock();
        setupKeyboardShortcuts();
        updateTaxRowVisibility();

        // Show logged-in cashier
        User cashier = SessionContext.getInstance().getCurrentUser();
        lblCashier.setText("Cashier: " + cashier.getFullName());

        // Discount permission guard
        boolean canDiscount = SessionContext.getInstance().hasPermission("DISCOUNT_APPLY");

        // Focus barcode field
        Platform.runLater(() -> barcodeField.requestFocus());

        updateTotals();
        setStatus("Ready. Scan a barcode or enter a product name.");
        log.debug("POSController initialised.");
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupTable() {
        colBarcode.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getBarcode()));
        colProductName.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getProductName()));
        colQty.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getQuantity()
                .stripTrailingZeros().toPlainString()));
        colUnitPrice.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getUnitPrice())));
        colDiscount.setCellValueFactory(d -> {
            BigDecimal disc = d.getValue().getDiscountAmount();
            return new SimpleStringProperty(
                disc != null && disc.compareTo(BigDecimal.ZERO) > 0
                    ? fmt(disc) : "—");
        });
        colTotal.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getTotalPrice())));

        // Highlight the row on selection
        cartTable.setItems(cartItems);
    }

    private void setupClock() {
        // Update clock every second
        javafx.animation.Timeline clock = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1), e ->
                    lblDateTime.setText(
                        java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter
                                .ofPattern("dd/MM/yyyy  HH:mm:ss")))
            )
        );
        clock.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clock.play();
    }

    private void setupKeyboardShortcuts() {
        // F10 = Pay
        barcodeField.getScene(); // scene may not be ready yet; use Platform.runLater
        Platform.runLater(() -> {
            if (barcodeField.getScene() != null) {
                barcodeField.getScene().setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case F10 -> handlePay();
                        case ESCAPE -> handleClearCart();
                        case DELETE -> handleRemoveItem();
                        default -> {}
                    }
                });
            }
        });
    }

    private void updateTaxRowVisibility() {
        if (lblTaxRow != null) {
            lblTaxRow.setVisible(taxEnabled);
            lblTaxRow.setManaged(taxEnabled);
        }
        if (lblTax != null) {
            lblTax.setVisible(taxEnabled);
            lblTax.setManaged(taxEnabled);
        }
    }

    // ── Barcode scan ──────────────────────────────────────────────────────────

    /**
     * Triggered when Enter is pressed in the barcode field.
     * Looks up the product and adds it to the cart.
     */
    @FXML
    private void handleBarcodeEnter() {
        String input = barcodeField.getText().trim();
        if (input.isBlank()) return;

        clearProductInfo();

        Optional<Product> found = productService.getProductByBarcode(input);

        // If no barcode match, try name search
        if (found.isEmpty()) {
            var results = productService.searchProducts(input);
            if (results.size() == 1) {
                found = Optional.of(results.get(0));
            } else if (results.size() > 1) {
                // Show picker
                ChoiceDialog<Product> picker =
                    new ChoiceDialog<>(results.get(0), results);
                picker.setTitle("Select Product");
                picker.setHeaderText("Multiple products found:");
                picker.setContentText("Product:");
                found = picker.showAndWait();
            }
        }

        if (found.isEmpty()) {
            setProductInfo("Product not found: " + input, true);
            barcodeField.selectAll();
            return;
        }

        Product product = found.get();

        if (!product.isActive()) {
            setProductInfo("Product is inactive: " + product.getName(), true);
            barcodeField.clear();
            return;
        }

        // Check stock
        boolean allowNeg = Boolean.parseBoolean(
            settingDAO.getValue("allow_negative_stock", "false"));
        if (!allowNeg && product.isOutOfStock()) {
            setProductInfo("Out of stock: " + product.getName(), true);
            barcodeField.clear();
            return;
        }

        addToCart(product, BigDecimal.ONE);
        setProductInfo("Added: " + product.getName(), false);
        barcodeField.clear();
        barcodeField.requestFocus();
    }

    // ── Cart management ───────────────────────────────────────────────────────

    private void addToCart(Product product, BigDecimal quantity) {
        // If product already in cart, increment quantity
        for (SaleItem existing : cartItems) {
            if (existing.getProductId() == product.getId()) {
                existing.setQuantity(existing.getQuantity().add(quantity));
                existing.recalculate();
                cartTable.refresh();
                updateTotals();
                return;
            }
        }
        // New cart entry
        SaleItem item = new SaleItem(
            product, quantity, product.getSellingPrice(), BigDecimal.ZERO);
        cartItems.add(item);
        updateTotals();
        // Select the new row
        cartTable.getSelectionModel().selectLast();
    }

    @FXML
    private void handleRemoveItem() {
        SaleItem sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            cartItems.remove(sel);
            updateTotals();
            barcodeField.requestFocus();
        }
    }

    @FXML
    private void handleClearCart() {
        if (cartItems.isEmpty()) return;
        if (AlertUtil.showConfirm("Clear Cart", "Remove all items from the cart?")) {
            cartItems.clear();
            selectedCustomer = null;
            lblCustomerName.setText("Walk-in customer");
            updateTotals();
            barcodeField.requestFocus();
        }
    }

    /** Inline quantity edit — double-click a row to change quantity. */
    @FXML
    private void handleQtyEdit() {
        SaleItem sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        TextInputDialog dialog = new TextInputDialog(
            sel.getQuantity().stripTrailingZeros().toPlainString());
        dialog.setTitle("Edit Quantity");
        dialog.setHeaderText("Change quantity for: " + sel.getProductName());
        dialog.setContentText("Quantity:");
        dialog.showAndWait().ifPresent(val -> {
            try {
                BigDecimal newQty = new BigDecimal(val.trim());
                if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
                    cartItems.remove(sel);
                } else {
                    sel.setQuantity(newQty);
                    sel.recalculate();
                    cartTable.refresh();
                }
                updateTotals();
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Invalid Input", "Please enter a valid number.");
            }
        });
        barcodeField.requestFocus();
    }

    // ── Customer ──────────────────────────────────────────────────────────────

    @FXML
    private void handleCustomerSearch() {
        String query = customerSearchField.getText().trim();
        if (query.isBlank()) return;

        // Try phone first (exact), then name search
        Optional<Customer> found = customerService.getCustomerByPhone(query);
        if (found.isEmpty()) {
            var results = customerService.searchCustomers(query);
            if (results.size() == 1) {
                found = Optional.of(results.get(0));
            } else if (results.size() > 1) {
                ChoiceDialog<Customer> picker =
                    new ChoiceDialog<>(results.get(0), results);
                picker.setTitle("Select Customer");
                picker.setHeaderText("Multiple customers found:");
                picker.setContentText("Customer:");
                found = picker.showAndWait();
            }
        }

        if (found.isPresent()) {
            selectedCustomer = found.get();
            lblCustomerName.setText(selectedCustomer.getName()
                + (selectedCustomer.getPhone() != null
                    ? " — " + selectedCustomer.getPhone() : ""));
            customerSearchField.clear();
        } else {
            lblCustomerName.setText("Customer not found.");
        }
        barcodeField.requestFocus();
    }

    @FXML
    private void handleClearCustomer() {
        selectedCustomer = null;
        lblCustomerName.setText("Walk-in customer");
        customerSearchField.clear();
        barcodeField.requestFocus();
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    @FXML
    private void handlePay() {
        if (cartItems.isEmpty()) {
            AlertUtil.showWarning("Empty Cart", "Please add products before processing payment.");
            return;
        }

        // Build sale object
        Sale sale = buildSale();

        // Open payment dialog
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/pos/PaymentDialog.fxml"));
            Parent root = loader.load();
            PaymentDialogController payCtrl = loader.getController();
            payCtrl.setSale(sale);

            Stage dialog = new Stage();
            dialog.setTitle("Payment");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

            if (!payCtrl.isConfirmed()) {
                barcodeField.requestFocus();
                return;
            }
        } catch (IOException e) {
            log.error("Failed to open PaymentDialog: {}", e.getMessage(), e);
            AlertUtil.showError("Error", "Could not open the payment dialog.");
            return;
        }

        // Process sale
        try {
            Sale completed = saleService.completeSale(sale);
            showSaleSuccess(completed);
            clearAfterSale();
        } catch (StockException e) {
            AlertUtil.showException("Insufficient Stock", e);
        } catch (POSException e) {
            AlertUtil.showException("Sale Failed", e);
        } catch (Exception e) {
            log.error("Unexpected error completing sale: {}", e.getMessage(), e);
            AlertUtil.showError("Sale Failed",
                "An unexpected error occurred. Please try again.");
        }
    }

    // ── Post-sale ─────────────────────────────────────────────────────────────

    private void showSaleSuccess(Sale sale) {
        String msg = "Sale Complete!\n"
            + "Invoice: " + sale.getInvoiceNumber() + "\n"
            + "Total:   " + currency + " "
            + String.format("%,.2f", sale.getGrandTotal()) + "\n"
            + "Method:  " + sale.getPaymentMethod() + "\n";

        if ("CASH".equals(sale.getPaymentMethod())
                && sale.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
            msg += "Change:  " + currency + " "
                + String.format("%,.2f", sale.getChangeAmount());
        }

        lblLastSale.setText("Last: " + sale.getInvoiceNumber()
            + " — " + currency + " "
            + String.format("%,.2f", sale.getGrandTotal()));

        AlertUtil.showInfo("Sale Complete", msg);
    }

    private void clearAfterSale() {
        cartItems.clear();
        selectedCustomer = null;
        lblCustomerName.setText("Walk-in customer");
        customerSearchField.clear();
        updateTotals();
        setStatus("Ready. Scan a barcode or enter a product name.");
        Platform.runLater(() -> barcodeField.requestFocus());
    }

    // ── Totals ────────────────────────────────────────────────────────────────

    private void updateTotals() {
        // Build a temporary sale to leverage recalculateTotals
        Sale temp = buildSale();
        temp.recalculateTotals(taxRate, taxEnabled);

        lblSubtotal.setText(fmt(temp.getSubtotal()));
        lblDiscount.setText(fmt(temp.getDiscountAmount()));
        lblGrandTotal.setText(currency + " " +
            String.format("%,.2f", temp.getGrandTotal()));

        if (taxEnabled) {
            lblTax.setText(fmt(temp.getTaxAmount()));
        }

        // Update pay button state
        btnPay.setDisable(cartItems.isEmpty());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Sale buildSale() {
        Sale sale = new Sale();
        sale.setCustomer(selectedCustomer);
        sale.setItems(new java.util.ArrayList<>(cartItems));
        sale.recalculateTotals(taxRate, taxEnabled);
        return sale;
    }

    private String fmt(BigDecimal val) {
        return (val != null) ? currency + " " + String.format("%,.2f", val) : currency + " 0.00";
    }

    private void setStatus(String msg) {
        if (lblStatus != null) lblStatus.setText(msg);
    }

    private void setProductInfo(String msg, boolean isError) {
        if (lblProductInfo != null) {
            lblProductInfo.setText(msg);
            lblProductInfo.setStyle(isError
                ? "-fx-text-fill: #c62828;"
                : "-fx-text-fill: #2e7d32;");
        }
    }

    private void clearProductInfo() {
        if (lblProductInfo != null) {
            lblProductInfo.setText("");
        }
    }
}
