package com.gp.grocerypos.controller.inventory;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.model.Product;
import com.gp.grocerypos.service.InventoryService;
import com.gp.grocerypos.service.InventoryServiceImpl;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Stock Adjustment / Damage dialog.
 * <p>
 * In adjustment mode: the admin enters the new absolute stock level.
 * In damage mode: the admin enters the quantity to write off.
 */
public class StockAdjustDialogController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(StockAdjustDialogController.class);

    @FXML private Label     titleLabel;
    @FXML private Label     productLabel;
    @FXML private Label     currentStockLabel;
    @FXML private Label     quantityLabel;
    @FXML private TextField quantityField;
    @FXML private TextArea  reasonField;
    @FXML private Label     errorLabel;
    @FXML private Button    btnSave;
    @FXML private Button    btnCancel;

    private final InventoryService inventoryService = new InventoryServiceImpl();
    private final String currency = AppConfig.getInstance().getCurrencySymbol();

    private Product product;
    private boolean isDamage;
    private boolean saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        errorLabel.setVisible(false);

        // Decimal-only filter
        quantityField.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*(\\.\\d*)?")) quantityField.setText(old);
        });
    }

    /**
     * Called by the opener to configure the dialog.
     *
     * @param product   the product to adjust
     * @param isDamage  true = damage/write-off mode, false = adjustment mode
     */
    public void init(Product product, boolean isDamage) {
        this.product  = product;
        this.isDamage = isDamage;

        titleLabel.setText(isDamage ? "Record Damage / Write-off" : "Adjust Stock");
        productLabel.setText(product.getName()
            + (product.getBarcode() != null ? " [" + product.getBarcode() + "]" : ""));
        currentStockLabel.setText(
            product.getCurrentStock().stripTrailingZeros().toPlainString()
            + " " + (product.getUnit() != null ? product.getUnit().getLabel() : ""));

        if (isDamage) {
            quantityLabel.setText("Quantity to Write Off *");
            quantityField.setPromptText("Enter quantity damaged");
        } else {
            quantityLabel.setText("New Stock Quantity *");
            quantityField.setPromptText("Enter the corrected stock level");
            // Pre-fill with current stock so admin just adjusts the number
            quantityField.setText(product.getCurrentStock()
                .stripTrailingZeros().toPlainString());
        }
    }

    public boolean isSaved() { return saved; }

    @FXML
    private void handleSave() {
        clearError();

        String qtyText = quantityField.getText().trim();
        String reason  = reasonField.getText().trim();

        if (qtyText.isBlank()) {
            showError("Please enter a quantity.");
            return;
        }

        BigDecimal qty;
        try {
            qty = new BigDecimal(qtyText);
        } catch (NumberFormatException e) {
            showError("Please enter a valid number.");
            return;
        }

        try {
            if (isDamage) {
                inventoryService.recordDamage(product.getId(), qty, reason);
            } else {
                inventoryService.adjustStock(product.getId(), qty, reason);
            }
            saved = true;
            closeDialog();
        } catch (POSException e) {
            showError(e.getUserMessage());
        } catch (Exception e) {
            showError("An unexpected error occurred. Please try again.");
            log.error("Stock adjustment error: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleCancel() { closeDialog(); }

    private void closeDialog() {
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
