package com.gp.grocerypos.controller.sales;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.model.*;
import com.gp.grocerypos.service.*;
import com.gp.grocerypos.util.AlertUtil;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the Returns / Refunds screen.
 * <p>
 * Workflow:
 * <ol>
 *   <li>Admin enters invoice number and clicks Search.</li>
 *   <li>Original sale details are displayed.</li>
 *   <li>Items are shown in a table with checkboxes (select to return) and
 *       editable return-quantity fields.</li>
 *   <li>Admin sets a reason and clicks Process Return.</li>
 *   <li>ReturnService handles the full transaction.</li>
 * </ol>
 */
public class ReturnController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ReturnController.class);
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private TextField invoiceField;

    // Sale info panel
    @FXML private Label lblInvoice;
    @FXML private Label lblDate;
    @FXML private Label lblCustomer;
    @FXML private Label lblTotal;
    @FXML private Label lblSaleStatus;

    // Items selection table
    @FXML private TableView<ReturnRowModel>              itemsTable;
    @FXML private TableColumn<ReturnRowModel, Boolean>   colSelect;
    @FXML private TableColumn<ReturnRowModel, String>    colProduct;
    @FXML private TableColumn<ReturnRowModel, String>    colSoldQty;
    @FXML private TableColumn<ReturnRowModel, String>    colReturnQty;
    @FXML private TableColumn<ReturnRowModel, String>    colUnitPrice;
    @FXML private TableColumn<ReturnRowModel, String>    colRefund;
    @FXML private TableColumn<ReturnRowModel, Boolean>   colRestock;

    @FXML private TextArea returnReasonField;
    @FXML private Label    lblTotalRefund;
    @FXML private Label    errorLabel;
    @FXML private Button   btnProcess;

    private final SaleService   saleService   = new SaleServiceImpl();
    private final ReturnService returnService = new ReturnServiceImpl();
    private final String        currency      = AppConfig.getInstance().getCurrencySymbol();

    private Sale loadedSale;
    private final List<ReturnRowModel> rowModels = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        clearSalePanel();
        errorLabel.setVisible(false);
        btnProcess.setDisable(true);
        log.debug("ReturnController initialised.");
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupTable() {
        // Checkbox column for "select this item to return"
        colSelect.setCellValueFactory(d -> d.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);
        itemsTable.setEditable(true);

        colProduct.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getProductName()));
        colSoldQty.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getSoldQty()
                .stripTrailingZeros().toPlainString()));

        // Return quantity — editable TextField cell
        colReturnQty.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getReturnQty()
                .stripTrailingZeros().toPlainString()));
        colReturnQty.setCellFactory(col -> new EditableDecimalCell());
        colReturnQty.setEditable(true);
        colReturnQty.setOnEditCommit(e -> {
            try {
                BigDecimal newQty = new BigDecimal(e.getNewValue().trim());
                e.getRowValue().setReturnQty(newQty);
                updateTotalRefund();
            } catch (NumberFormatException ex) {
                itemsTable.refresh();
            }
        });

        colUnitPrice.setCellValueFactory(d ->
            new SimpleStringProperty(currency + " " +
                String.format("%,.2f", d.getValue().getUnitPrice())));

        colRefund.setCellValueFactory(d -> {
            ReturnRowModel row = d.getValue();
            BigDecimal refund = row.isSelected()
                ? row.getReturnQty().multiply(row.getUnitPrice())
                : BigDecimal.ZERO;
            return new SimpleStringProperty(currency + " " +
                String.format("%,.2f", refund));
        });

        // Checkbox column for "restock this item"
        colRestock.setCellValueFactory(d -> d.getValue().restockProperty());
        colRestock.setCellFactory(CheckBoxTableCell.forTableColumn(colRestock));
        colRestock.setEditable(true);
    }

    // ── Invoice search ────────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        clearError();
        String invoice = invoiceField.getText().trim();
        if (invoice.isBlank()) {
            showError("Please enter an invoice number.");
            return;
        }

        saleService.getSaleByInvoiceNumber(invoice).ifPresentOrElse(
            sale -> {
                if (sale.getStatus() == Sale.Status.VOIDED) {
                    showError("Cannot process a return for a voided sale ("
                        + invoice + ").");
                    clearSalePanel();
                    return;
                }
                loadedSale = sale;
                populateSalePanel(sale);
                populateItemsTable(sale);
                btnProcess.setDisable(false);
            },
            () -> {
                showError("Invoice not found: " + invoice);
                clearSalePanel();
            }
        );
    }

    // ── Populate helpers ──────────────────────────────────────────────────────

    private void populateSalePanel(Sale sale) {
        lblInvoice.setText(sale.getInvoiceNumber());
        lblDate.setText(sale.getSaleDate() != null
            ? sale.getSaleDate().format(DT_FMT) : "");
        lblCustomer.setText(sale.getCustomerName());
        lblTotal.setText(currency + " " +
            String.format("%,.2f", sale.getGrandTotal()));
        lblSaleStatus.setText(sale.getStatus().name());
        lblSaleStatus.setStyle(sale.getStatus() == Sale.Status.COMPLETED
            ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
            : "-fx-text-fill: #e65100; -fx-font-weight: bold;");
    }

    private void populateItemsTable(Sale sale) {
        rowModels.clear();
        // Re-load full items from DB
        List<SaleItem> items = saleService.getSaleById(sale.getId())
            .map(Sale::getItems)
            .orElse(sale.getItems());

        for (SaleItem item : items) {
            ReturnRowModel row = new ReturnRowModel(item);
            row.selectedProperty().addListener((obs, old, val) -> updateTotalRefund());
            rowModels.add(row);
        }
        itemsTable.setItems(FXCollections.observableArrayList(rowModels));
        lblTotalRefund.setText(currency + " 0.00");
    }

    private void clearSalePanel() {
        lblInvoice.setText("—");
        lblDate.setText("—");
        lblCustomer.setText("—");
        lblTotal.setText("—");
        lblSaleStatus.setText("—");
        lblSaleStatus.setStyle("");
        rowModels.clear();
        itemsTable.setItems(FXCollections.observableArrayList());
        lblTotalRefund.setText(currency + " 0.00");
        btnProcess.setDisable(true);
        loadedSale = null;
    }

    // ── Total calculation ─────────────────────────────────────────────────────

    private void updateTotalRefund() {
        BigDecimal total = rowModels.stream()
            .filter(ReturnRowModel::isSelected)
            .map(r -> r.getReturnQty().multiply(r.getUnitPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalRefund.setText(currency + " " + String.format("%,.2f", total));
        itemsTable.refresh(); // refresh refund column
    }

    // ── Process return ────────────────────────────────────────────────────────

    @FXML
    private void handleProcess() {
        clearError();
        if (loadedSale == null) return;

        List<ReturnRowModel> selected = rowModels.stream()
            .filter(ReturnRowModel::isSelected).toList();

        if (selected.isEmpty()) {
            showError("Please select at least one item to return.");
            return;
        }

        String reason = returnReasonField.getText().trim();

        if (!AlertUtil.showConfirm("Confirm Return",
                "Process return for invoice " + loadedSale.getInvoiceNumber() + "?\n"
                + "Selected items: " + selected.size() + "\n"
                + "Total Refund: " + lblTotalRefund.getText())) return;

        // Build SaleReturn object
        SaleReturn sr = new SaleReturn();
        sr.setOriginalSale(loadedSale);
        sr.setReason(reason.isBlank() ? null : reason);

        for (ReturnRowModel row : selected) {
            ReturnItem ri = new ReturnItem(
                row.getSaleItem(),
                row.getReturnQty(),
                row.isRestock()
            );
            sr.addItem(ri);
        }

        try {
            SaleReturn completed = returnService.processReturn(sr);
            AlertUtil.showInfo("Return Processed",
                "Return " + completed.getReturnNumber() + " saved.\n"
                + "Total Refund: " + currency + " "
                + String.format("%,.2f", completed.getTotalRefund()));
            clearAfterReturn();
        } catch (POSException e) {
            showError(e.getUserMessage());
        } catch (Exception e) {
            showError("An unexpected error occurred. Please try again.");
            log.error("Return processing error: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleClear() {
        invoiceField.clear();
        returnReasonField.clear();
        clearSalePanel();
        clearError();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearAfterReturn() {
        invoiceField.clear();
        returnReasonField.clear();
        clearSalePanel();
        clearError();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner model class for the items table row
    // ─────────────────────────────────────────────────────────────────────────

    public static class ReturnRowModel {
        private final SaleItem      saleItem;
        private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
        private final SimpleBooleanProperty restock  = new SimpleBooleanProperty(true);
        private BigDecimal returnQty;

        public ReturnRowModel(SaleItem saleItem) {
            this.saleItem  = saleItem;
            this.returnQty = saleItem.getQuantity(); // default: return all
        }

        public SaleItem   getSaleItem()     { return saleItem; }
        public String     getProductName()  { return saleItem.getProductName(); }
        public BigDecimal getSoldQty()      { return saleItem.getQuantity(); }
        public BigDecimal getUnitPrice()    { return saleItem.getUnitPrice(); }
        public BigDecimal getReturnQty()    { return returnQty; }
        public void       setReturnQty(BigDecimal q) { this.returnQty = q; }
        public boolean    isSelected()      { return selected.get(); }
        public boolean    isRestock()       { return restock.get(); }

        public SimpleBooleanProperty selectedProperty() { return selected; }
        public SimpleBooleanProperty restockProperty()  { return restock; }
    }

    // ── Editable decimal table cell ───────────────────────────────────────────

    private static class EditableDecimalCell extends TableCell<ReturnRowModel, String> {
        private final TextField textField = new TextField();

        EditableDecimalCell() {
            textField.setOnAction(e -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, old, isFocused) -> {
                if (!isFocused) commitEdit(textField.getText());
            });
        }

        @Override
        public void startEdit() {
            super.startEdit();
            textField.setText(getItem());
            setGraphic(textField);
            setText(null);
            textField.requestFocus();
            textField.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null); setGraphic(null);
            } else if (isEditing()) {
                textField.setText(item);
                setGraphic(textField); setText(null);
            } else {
                setText(item); setGraphic(null);
            }
        }
    }
}
