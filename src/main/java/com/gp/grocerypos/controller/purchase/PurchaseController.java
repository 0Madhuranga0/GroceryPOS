package com.gp.grocerypos.controller.purchase;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.model.Purchase;
import com.gp.grocerypos.model.Supplier;
import com.gp.grocerypos.service.PurchaseService;
import com.gp.grocerypos.service.PurchaseServiceImpl;
import com.gp.grocerypos.service.SessionContext;
import com.gp.grocerypos.service.SupplierService;
import com.gp.grocerypos.service.SupplierServiceImpl;
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
 * Controller for the Purchase list screen.
 * Shows all purchases with date-range filter and supplier filter.
 */
public class PurchaseController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(PurchaseController.class);
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private DatePicker               fromDatePicker;
    @FXML private DatePicker               toDatePicker;
    @FXML private ComboBox<Supplier>       supplierFilter;
    @FXML private TextField                searchField;
    @FXML private TableView<Purchase>      tableView;
    @FXML private TableColumn<Purchase, String> colNumber;
    @FXML private TableColumn<Purchase, String> colDate;
    @FXML private TableColumn<Purchase, String> colSupplier;
    @FXML private TableColumn<Purchase, String> colTotal;
    @FXML private TableColumn<Purchase, String> colUser;
    @FXML private TableColumn<Purchase, String> colStatus;
    @FXML private Button                   btnNew;
    @FXML private Button                   btnView;
    @FXML private Label                    statusLabel;

    private final PurchaseService purchaseService = new PurchaseServiceImpl();
    private final SupplierService supplierService = new SupplierServiceImpl();
    private final String          currency        = AppConfig.getInstance().getCurrencySymbol();

    private ObservableList<Purchase> masterList;
    private FilteredList<Purchase>   filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupFilters();
        setupPermissions();
        loadData();
        log.debug("PurchaseController initialised.");
    }

    private void setupColumns() {
        colNumber.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getPurchaseNumber()));
        colDate.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getPurchaseDate() != null
                ? d.getValue().getPurchaseDate().format(DT_FMT) : ""));
        colSupplier.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getSupplierName()));
        colTotal.setCellValueFactory(d -> {
            BigDecimal total = d.getValue().getTotalAmount();
            return new SimpleStringProperty(total != null
                ? currency + " " + String.format("%,.2f", total) : "");
        });
        colUser.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getUserName()));
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getStatus().name()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("COMPLETED".equals(s)
                    ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                    : "-fx-text-fill: #c62828; -fx-font-weight: bold;");
            }
        });
    }

    private void setupFilters() {
        // Default range: last 30 days
        toDatePicker.setValue(LocalDate.now());
        fromDatePicker.setValue(LocalDate.now().minusDays(30));

        // Populate supplier filter
        supplierFilter.getItems().add(null);
        supplierService.getAllSuppliers().forEach(s -> supplierFilter.getItems().add(s));
        supplierFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Supplier s, boolean empty) {
                super.updateItem(s, empty);
                setText(s == null ? "All Suppliers" : s.getName());
            }
        });
        supplierFilter.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Supplier s, boolean empty) {
                super.updateItem(s, empty);
                setText(s == null ? "All Suppliers" : s.getName());
            }
        });
        supplierFilter.setValue(null);

        masterList   = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(masterList, p -> true);
        tableView.setItems(filteredList);

        searchField.textProperty().addListener((obs, old, val) -> applySearch(val));
        supplierFilter.setOnAction(e -> loadData());
    }

    private void setupPermissions() {
        boolean can = SessionContext.getInstance().hasPermission("PURCHASE_CREATE");
        btnNew.setDisable(!can);
    }

    private void loadData() {
        try {
            Supplier sup  = supplierFilter.getValue();
            LocalDate from = fromDatePicker.getValue();
            LocalDate to   = toDatePicker.getValue();

            java.util.List<Purchase> list;
            if (sup != null) {
                list = purchaseService.getPurchasesBySupplier(sup.getId());
            } else if (from != null && to != null) {
                list = purchaseService.getPurchasesByDateRange(from, to);
            } else {
                list = purchaseService.getAllPurchases();
            }
            masterList.setAll(list);
            setStatus("Showing " + masterList.size() + " purchases.");
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    private void applySearch(String val) {
        String lower = val == null ? "" : val.toLowerCase();
        filteredList.setPredicate(p ->
            lower.isBlank()
            || p.getPurchaseNumber().toLowerCase().contains(lower)
            || p.getSupplierName().toLowerCase().contains(lower)
            || (p.getSupplierInvoice() != null
                && p.getSupplierInvoice().toLowerCase().contains(lower))
        );
    }

    @FXML
    private void handleFilter() { loadData(); }

    @FXML
    private void handleNew() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/purchase/NewPurchase.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("New Purchase");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setMinWidth(900);
            stage.setMinHeight(650);
            stage.showAndWait();

            NewPurchaseController ctrl = loader.getController();
            if (ctrl.isSaved()) {
                loadData();
                setStatus("Purchase saved successfully.");
            }
        } catch (IOException e) {
            log.error("Failed to open NewPurchase: {}", e.getMessage(), e);
            AlertUtil.showError("Error", "Could not open the new purchase screen.");
        }
    }

    @FXML
    private void handleView() {
        Purchase sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.showWarning("No Selection", "Please select a purchase to view.");
            return;
        }
        openDetailDialog(sel);
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadData();
    }

    private void openDetailDialog(Purchase purchase) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/purchase/PurchaseDetail.fxml"));
            Parent root = loader.load();
            PurchaseDetailController ctrl = loader.getController();
            ctrl.setPurchase(purchase);
            Stage dialog = new Stage();
            dialog.setTitle("Purchase Detail — " + purchase.getPurchaseNumber());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(true);
            dialog.showAndWait();
        } catch (IOException e) {
            log.error("Failed to open PurchaseDetail: {}", e.getMessage(), e);
            AlertUtil.showError("Error", "Could not open the purchase detail.");
        }
    }

    private void setStatus(String msg) { statusLabel.setText(msg); }
}
