package com.gp.grocerypos.controller.purchase;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.model.Purchase;
import com.gp.grocerypos.model.PurchaseItem;
import com.gp.grocerypos.service.PurchaseService;
import com.gp.grocerypos.service.PurchaseServiceImpl;
import com.gp.grocerypos.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller for the Purchase detail read-only dialog.
 */
public class PurchaseDetailController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(PurchaseDetailController.class);
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");

    @FXML private Label  lblNumber;
    @FXML private Label  lblDate;
    @FXML private Label  lblSupplier;
    @FXML private Label  lblInvoice;
    @FXML private Label  lblUser;
    @FXML private Label  lblStatus;
    @FXML private Label  lblNotes;

    @FXML private TableView<PurchaseItem>          itemsTable;
    @FXML private TableColumn<PurchaseItem, String> colProduct;
    @FXML private TableColumn<PurchaseItem, String> colQty;
    @FXML private TableColumn<PurchaseItem, String> colUnit;
    @FXML private TableColumn<PurchaseItem, String> colUnitCost;
    @FXML private TableColumn<PurchaseItem, String> colTotal;

    @FXML private Label  lblGrandTotal;
    @FXML private Button btnClose;

    private final PurchaseService purchaseService = new PurchaseServiceImpl();
    private final String          currency        = AppConfig.getInstance().getCurrencySymbol();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colProduct.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getProductName()));
        colQty.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getQuantity()
                .stripTrailingZeros().toPlainString()));
        colUnit.setCellValueFactory(d -> {
            var p = d.getValue().getProduct();
            return new SimpleStringProperty(
                p != null && p.getUnit() != null ? p.getUnit().getLabel() : "");
        });
        colUnitCost.setCellValueFactory(d ->
            new SimpleStringProperty(currency + " " +
                String.format("%,.2f", d.getValue().getUnitCost())));
        colTotal.setCellValueFactory(d ->
            new SimpleStringProperty(currency + " " +
                String.format("%,.2f", d.getValue().getTotalCost())));
    }

    public void setPurchase(Purchase purchase) {
        // Reload from DB to ensure items are populated
        Purchase full = purchaseService.getPurchaseById(purchase.getId())
            .orElse(purchase);

        lblNumber.setText(full.getPurchaseNumber());
        lblDate.setText(full.getPurchaseDate() != null
            ? full.getPurchaseDate().format(DT_FMT) : "");
        lblSupplier.setText(full.getSupplierName());
        lblInvoice.setText(full.getSupplierInvoice() != null
            ? full.getSupplierInvoice() : "—");
        lblUser.setText(full.getUserName());
        lblStatus.setText(full.getStatus().name());
        lblNotes.setText(full.getNotes() != null ? full.getNotes() : "—");

        itemsTable.getItems().setAll(full.getItems());

        BigDecimal total = full.getTotalAmount() != null
            ? full.getTotalAmount() : BigDecimal.ZERO;
        lblGrandTotal.setText(currency + " " + String.format("%,.2f", total));
    }

    @FXML
    private void handleClose() {
        ((Stage) btnClose.getScene().getWindow()).close();
    }
}
