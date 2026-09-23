package com.gp.grocerypos.controller.pos;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.model.Sale;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Payment dialog shown when the cashier clicks "Pay".
 * <p>
 * Supports Cash and Card payment methods. The dialog shows the grand total,
 * accepts the tendered cash amount, and computes change in real time.
 * On confirmation the selected method and cash amount are stored on the
 * Sale object so POSController can call SaleService.completeSale().
 */
public class PaymentDialogController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Label             lblGrandTotal;
    @FXML private Label             lblInvoiceInfo;
    @FXML private ToggleGroup       paymentToggle;
    @FXML private RadioButton       rbCash;
    @FXML private RadioButton       rbCard;
    @FXML private TextField         cashField;
    @FXML private Label             lblChange;
    @FXML private Label             lblChangeCaption;
    @FXML private Label             errorLabel;
    @FXML private Button            btnConfirm;
    @FXML private Button            btnCancel;

    private final String currency = AppConfig.getInstance().getCurrencySymbol();

    private Sale    sale;
    private boolean confirmed = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        errorLabel.setVisible(false);

        // Show/hide cash field based on payment method
        rbCash.setOnAction(e -> setCashMode(true));
        rbCard.setOnAction(e -> setCashMode(false));

        // Real-time change calculation
        cashField.textProperty().addListener((obs, old, val) -> calculateChange());

        // Default: Cash
        rbCash.setSelected(true);
        setCashMode(true);
    }

    // ── Called by POSController ───────────────────────────────────────────────

    public void setSale(Sale sale) {
        this.sale = sale;
        BigDecimal total = sale.getGrandTotal();
        lblGrandTotal.setText(currency + " " + String.format("%,.2f", total));
        lblInvoiceInfo.setText("Items: " + sale.getItems().size()
            + "   Customer: " + sale.getCustomerName());

        // Pre-fill cash with exact amount as convenience
        cashField.setText(String.format("%.2f", total));
        calculateChange();
    }

    public boolean isConfirmed() { return confirmed; }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML
    private void handleConfirm() {
        clearError();

        String method = rbCash.isSelected() ? "CASH" : "CARD";

        if ("CASH".equals(method)) {
            BigDecimal tendered;
            try {
                tendered = new BigDecimal(cashField.getText().trim());
            } catch (NumberFormatException e) {
                showError("Please enter a valid cash amount.");
                cashField.requestFocus();
                return;
            }
            if (tendered.compareTo(sale.getGrandTotal()) < 0) {
                showError("Cash amount is less than the total. "
                    + "Required: " + currency + " "
                    + String.format("%,.2f", sale.getGrandTotal()));
                return;
            }
            sale.setCashReceived(tendered);
            sale.setChangeAmount(tendered.subtract(sale.getGrandTotal())
                .max(BigDecimal.ZERO));
        } else {
            // Card — cash received = grand total, change = 0
            sale.setCashReceived(sale.getGrandTotal());
            sale.setChangeAmount(BigDecimal.ZERO);
        }

        sale.setPaymentMethod(method);
        confirmed = true;
        closeDialog();
    }

    @FXML
    private void handleCancel() { closeDialog(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setCashMode(boolean isCash) {
        cashField.setDisable(!isCash);
        lblChange.setVisible(isCash);
        lblChangeCaption.setVisible(isCash);
        if (isCash) {
            cashField.requestFocus();
            cashField.selectAll();
            calculateChange();
        } else {
            lblChange.setText(currency + " 0.00");
        }
    }

    private void calculateChange() {
        if (sale == null || cashField.isDisable()) return;
        try {
            BigDecimal tendered = new BigDecimal(cashField.getText().trim());
            BigDecimal change   = tendered.subtract(sale.getGrandTotal())
                .setScale(2, RoundingMode.HALF_UP);
            if (change.compareTo(BigDecimal.ZERO) >= 0) {
                lblChange.setText(currency + " " + String.format("%,.2f", change));
                lblChange.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;"
                    + " -fx-text-fill: #2e7d32;");
            } else {
                lblChange.setText("Short: " + currency + " "
                    + String.format("%,.2f", change.negate()));
                lblChange.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;"
                    + " -fx-text-fill: #c62828;");
            }
        } catch (NumberFormatException e) {
            lblChange.setText(currency + " —");
            lblChange.setStyle("-fx-font-size: 18px; -fx-text-fill: #78909c;");
        }
    }

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
