package com.gp.grocerypos.controller.sales;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.model.Sale;
import com.gp.grocerypos.service.SaleService;
import com.gp.grocerypos.service.SaleServiceImpl;
import com.gp.grocerypos.service.SessionContext;
import com.gp.grocerypos.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller for the Sales History screen.
 * Provides date-range filtering, live search, view detail, and void sale.
 */
public class SalesController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SalesController.class);
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private DatePicker             fromDatePicker;
    @FXML private DatePicker             toDatePicker;
    @FXML private ComboBox<String>       paymentFilter;
    @FXML private TextField              searchField;
    @FXML private TableView<Sale>        tableView;
    @FXML private TableColumn<Sale, String> colInvoice;
    @FXML private TableColumn<Sale, String> colDate;
    @FXML private TableColumn<Sale, String> colCustomer;
    @FXML private TableColumn<Sale, String> colCashier;
    @FXML private TableColumn<Sale, String> colTotal;
    @FXML private TableColumn<Sale, String> colPayment;
    @FXML private TableColumn<Sale, String> colStatus;
    @FXML private Button                 btnView;
    @FXML private Button                 btnVoid;
    @FXML private Label                  statusLabel;

    private final SaleService saleService = new SaleServiceImpl();
    private final String      currency    = AppConfig.getInstance().getCurrencySymbol();

    private ObservableList<Sale> masterList;
    private FilteredList<Sale>   filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupFilters();
        setupPermissions();
        loadData();
        log.debug("SalesController initialised.");
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupColumns() {
        colInvoice.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getInvoiceNumber()));
        colDate.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getSaleDate() != null
                ? d.getValue().getSaleDate().format(DT_FMT) : ""));
        colCustomer.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCustomerName()));
        colCashier.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getUserName()));
        colTotal.setCellValueFactory(d -> {
            BigDecimal t = d.getValue().getGrandTotal();
            return new SimpleStringProperty(t != null
                ? currency + " " + String.format("%,.2f", t) : "");
        });
        colPayment.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getPaymentMethod()));
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getStatus().name()));

        // Colour-code status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(switch (s) {
                    case "COMPLETED" -> "-fx-text-fill: #2e7d32; -fx-font-weight: bold;";
                    case "VOIDED"    -> "-fx-text-fill: #c62828; -fx-font-weight: bold;";
                    case "RETURNED"  -> "-fx-text-fill: #e65100; -fx-font-weight: bold;";
                    default          -> "";
                });
            }
        });

        tableView.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> {
                boolean isSel = sel != null;
                boolean canVoid = isSel && sel.getStatus() == Sale.Status.COMPLETED
                    && SessionContext.getInstance().hasPermission("SALE_VOID");
                btnVoid.setDisable(!canVoid);
            });
    }

    private void setupFilters() {
        fromDatePicker.setValue(LocalDate.now().minusDays(30));
        toDatePicker.setValue(LocalDate.now());

        paymentFilter.getItems().addAll("ALL", "CASH", "CARD");
        paymentFilter.setValue("ALL");

        masterList   = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(masterList, p -> true);
        tableView.setItems(filteredList);

        searchField.textProperty().addListener((obs, old, val) -> applySearch(val));
    }

    private void setupPermissions() {
        btnVoid.setDisable(true); // enabled only when COMPLETED sale selected
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            LocalDate from = fromDatePicker.getValue();
            LocalDate to   = toDatePicker.getValue();

            java.util.List<Sale> list = (from != null && to != null)
                ? saleService.getSalesByDateRange(from, to)
                : saleService.getAllSales();

            // Apply payment method filter
            String pm = paymentFilter.getValue();
            if (!"ALL".equals(pm)) {
                list = list.stream()
                    .filter(s -> pm.equals(s.getPaymentMethod()))
                    .toList();
            }

            masterList.setAll(list);
            setStatus("Showing " + masterList.size() + " sales.");
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    private void applySearch(String val) {
        String lower = val == null ? "" : val.toLowerCase();
        filteredList.setPredicate(s ->
            lower.isBlank()
            || s.getInvoiceNumber().toLowerCase().contains(lower)
            || s.getCustomerName().toLowerCase().contains(lower)
            || s.getUserName().toLowerCase().contains(lower)
            || (s.getPaymentMethod() != null
                && s.getPaymentMethod().toLowerCase().contains(lower))
        );
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML private void handleFilter()  { loadData(); }

    @FXML
    private void handleView() {
        Sale sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.showWarning("No Selection", "Please select a sale to view.");
            return;
        }
        openDetailDialog(sel);
    }

    @FXML
    private void handleVoid() {
        Sale sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        if (!AlertUtil.showConfirm("Void Sale",
                "Are you sure you want to void sale " + sel.getInvoiceNumber() + "?\n"
                + "This cannot be undone. Stock will NOT be automatically reversed.")) return;

        try {
            saleService.voidSale(sel.getId());
            loadData();
            setStatus("Sale " + sel.getInvoiceNumber() + " voided.");
        } catch (Exception e) {
            AlertUtil.showException("Void Failed", e);
        }
    }

    @FXML private void handleRefresh() { searchField.clear(); loadData(); }

    // ── Detail dialog ─────────────────────────────────────────────────────────

    private void openDetailDialog(Sale sale) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/sales/SaleDetail.fxml"));
            Parent root = loader.load();
            SaleDetailController ctrl = loader.getController();
            ctrl.setSale(sale);

            Stage dialog = new Stage();
            dialog.setTitle("Sale — " + sale.getInvoiceNumber());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(true);
            dialog.showAndWait();

            // Reload in case the sale was voided from within the dialog
            if (ctrl.wasSaleModified()) loadData();
        } catch (IOException e) {
            log.error("Failed to open SaleDetail: {}", e.getMessage(), e);
            AlertUtil.showError("Error", "Could not open the sale detail.");
        }
    }

    private void setStatus(String msg) { statusLabel.setText(msg); }
}
