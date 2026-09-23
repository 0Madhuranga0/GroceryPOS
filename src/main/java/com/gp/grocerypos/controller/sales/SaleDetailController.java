package com.gp.grocerypos.controller.sales;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.model.Sale;
import com.gp.grocerypos.model.SaleItem;
import com.gp.grocerypos.service.SaleService;
import com.gp.grocerypos.service.SaleServiceImpl;
import com.gp.grocerypos.service.SessionContext;
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
 * Controller for the Sale Detail read-only dialog.
 * Also allows voiding the sale directly from the detail view.
 */
public class SaleDetailController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SaleDetailController.class);
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");

    @FXML private Label lblInvoice;
    @FXML private Label lblDate;
    @FXML private Label lblCustomer;
    @FXML private Label lblCashier;
    @FXML private Label lblPayment;
    @FXML private Label lblStatus;
    @FXML private Label lblNotes;

    @FXML private TableView<SaleItem>           itemsTable;
    @FXML private TableColumn<SaleItem, String>  colProduct;
    @FXML private TableColumn<SaleItem, String>  colQty;
    @FXML private TableColumn<SaleItem, String>  colUnitPrice;
    @FXML private TableColumn<SaleItem, String>  colDiscount;
    @FXML private TableColumn<SaleItem, String>  colTotal;

    @FXML private Label lblSubtotal;
    @FXML private Label lblDiscount;
    @FXML private Label lblTax;
    @FXML private Label lblGrandTotal;
    @FXML private Label lblCashReceived;
    @FXML private Label lblChange;

    @FXML private Button btnVoid;
    @FXML private Button btnClose;

    private final SaleService saleService = new SaleServiceImpl();
    private final String      currency    = AppConfig.getInstance().getCurrencySymbol();
    private Sale    sale;
    private boolean saleModified = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colProduct.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getProductName()));
        colQty.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getQuantity()
                .stripTrailingZeros().toPlainString()));
        colUnitPrice.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getUnitPrice())));
        colDiscount.setCellValueFactory(d -> {
            BigDecimal disc = d.getValue().getDiscountAmount();
            return new SimpleStringProperty(
                disc != null && disc.compareTo(BigDecimal.ZERO) > 0 ? fmt(disc) : "—");
        });
        colTotal.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getTotalPrice())));
    }

    public void setSale(Sale sale) {
        // Reload to ensure items are populated
        this.sale = saleService.getSaleById(sale.getId()).orElse(sale);
        populate();
    }

    public boolean wasSaleModified() { return saleModified; }

    // ── Populate ──────────────────────────────────────────────────────────────

    private void populate() {
        lblInvoice.setText(sale.getInvoiceNumber());
        lblDate.setText(sale.getSaleDate() != null
            ? sale.getSaleDate().format(DT_FMT) : "");
        lblCustomer.setText(sale.getCustomerName());
        lblCashier.setText(sale.getUserName());
        lblPayment.setText(sale.getPaymentMethod());
        lblStatus.setText(sale.getStatus().name());
        lblNotes.setText(sale.getNotes() != null ? sale.getNotes() : "—");

        // Style status label
        lblStatus.setStyle(switch (sale.getStatus()) {
            case COMPLETED -> "-fx-text-fill: #2e7d32; -fx-font-weight: bold;";
            case VOIDED    -> "-fx-text-fill: #c62828; -fx-font-weight: bold;";
            case RETURNED  -> "-fx-text-fill: #e65100; -fx-font-weight: bold;";
        });

        itemsTable.getItems().setAll(sale.getItems());

        lblSubtotal.setText(fmt(sale.getSubtotal()));
        lblDiscount.setText(fmt(sale.getDiscountAmount()));
        lblTax.setText(fmt(sale.getTaxAmount()));
        lblGrandTotal.setText(fmt(sale.getGrandTotal()));
        lblCashReceived.setText(fmt(sale.getCashReceived()));
        lblChange.setText(fmt(sale.getChangeAmount()));

        // Void button only for COMPLETED sales and users with SALE_VOID permission
        boolean canVoid = sale.getStatus() == Sale.Status.COMPLETED
            && SessionContext.getInstance().hasPermission("SALE_VOID");
        btnVoid.setDisable(!canVoid);
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML
    private void handleVoid() {
        if (!AlertUtil.showConfirm("Void Sale",
                "Void invoice " + sale.getInvoiceNumber() + "?\n"
                + "This cannot be undone. Stock will NOT be reversed automatically.")) return;
        try {
            saleService.voidSale(sale.getId());
            saleModified = true;
            sale.setStatus(Sale.Status.VOIDED);
            populate();
            AlertUtil.showInfo("Sale Voided", "Invoice "
                + sale.getInvoiceNumber() + " has been voided.");
        } catch (Exception e) {
            AlertUtil.showException("Void Failed", e);
        }
    }

    @FXML private void handleClose() {
        ((Stage) btnClose.getScene().getWindow()).close();
    }

    private String fmt(BigDecimal val) {
        return (val != null)
            ? currency + " " + String.format("%,.2f", val)
            : currency + " 0.00";
    }
}
