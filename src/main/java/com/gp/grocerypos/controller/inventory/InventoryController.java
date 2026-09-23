package com.gp.grocerypos.controller.inventory;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.model.InventoryMovement;
import com.gp.grocerypos.model.Product;
import com.gp.grocerypos.service.InventoryService;
import com.gp.grocerypos.service.InventoryServiceImpl;
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
import javafx.scene.control.cell.PropertyValueFactory;
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
 * Controller for the Inventory management screen.
 * Uses a TabPane with four tabs:
 *   1. Stock         — all products with current stock levels
 *   2. Movements     — full inventory audit trail (date-range filterable)
 *   3. Low Stock     — products at or below minimum stock
 *   4. Expiry Alerts — products expiring soon or already expired
 */
public class InventoryController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    // ── Tab 1: Stock ──────────────────────────────────────────────────────────
    @FXML private TextField                   stockSearchField;
    @FXML private TableView<Product>          stockTable;
    @FXML private TableColumn<Product, Integer> sColId;
    @FXML private TableColumn<Product, String>  sColBarcode;
    @FXML private TableColumn<Product, String>  sColName;
    @FXML private TableColumn<Product, String>  sColCategory;
    @FXML private TableColumn<Product, String>  sColUnit;
    @FXML private TableColumn<Product, String>  sColCurrent;
    @FXML private TableColumn<Product, String>  sColMinimum;
    @FXML private TableColumn<Product, String>  sColStatus;
    @FXML private Button                      btnAdjust;
    @FXML private Button                      btnDamage;
    @FXML private Label                       stockStatusLabel;

    // ── Tab 2: Movements ──────────────────────────────────────────────────────
    @FXML private DatePicker                         movFromDate;
    @FXML private DatePicker                         movToDate;
    @FXML private TableView<InventoryMovement>       movTable;
    @FXML private TableColumn<InventoryMovement, String> mColDate;
    @FXML private TableColumn<InventoryMovement, String> mColProduct;
    @FXML private TableColumn<InventoryMovement, String> mColType;
    @FXML private TableColumn<InventoryMovement, String> mColQty;
    @FXML private TableColumn<InventoryMovement, String> mColPrev;
    @FXML private TableColumn<InventoryMovement, String> mColNew;
    @FXML private TableColumn<InventoryMovement, String> mColRef;
    @FXML private TableColumn<InventoryMovement, String> mColUser;
    @FXML private TableColumn<InventoryMovement, String> mColReason;

    // ── Tab 3: Low Stock ──────────────────────────────────────────────────────
    @FXML private TableView<Product>          lowStockTable;
    @FXML private TableColumn<Product, String>  lsColName;
    @FXML private TableColumn<Product, String>  lsColCategory;
    @FXML private TableColumn<Product, String>  lsColCurrent;
    @FXML private TableColumn<Product, String>  lsColMinimum;
    @FXML private TableColumn<Product, String>  lsColUnit;
    @FXML private Label                       lowStockLabel;

    // ── Tab 4: Expiry ─────────────────────────────────────────────────────────
    @FXML private TableView<Product>          expiryTable;
    @FXML private TableColumn<Product, String>  exColName;
    @FXML private TableColumn<Product, String>  exColCategory;
    @FXML private TableColumn<Product, String>  exColStock;
    @FXML private TableColumn<Product, String>  exColExpiry;
    @FXML private TableColumn<Product, String>  exColAlert;
    @FXML private Label                       expiryLabel;

    private final InventoryService inventoryService = new InventoryServiceImpl();
    private final String currency = AppConfig.getInstance().getCurrencySymbol();

    private ObservableList<Product>          stockMaster;
    private FilteredList<Product>            stockFiltered;

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStockTab();
        setupMovementsTab();
        setupLowStockTab();
        setupExpiryTab();
        loadStockData();
        log.debug("InventoryController initialised.");
    }

    // =========================================================================
    // TAB 1 — STOCK
    // =========================================================================

    private void setupStockTab() {
        sColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        sColBarcode.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        sColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        sColCategory.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCategoryName()));
        sColUnit.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getUnit() != null
                ? d.getValue().getUnit().getLabel() : ""));
        sColCurrent.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getCurrentStock())));
        sColMinimum.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getMinimumStock())));
        sColStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getStatus().name()));

        // Colour the current stock cell
        sColCurrent.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                Product p = getTableView().getItems().get(getIndex());
                if (p.isOutOfStock())
                    setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                else if (p.isLowStock())
                    setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold;");
                else
                    setStyle("-fx-text-fill: #2e7d32;");
            }
        });

        stockMaster   = FXCollections.observableArrayList();
        stockFiltered = new FilteredList<>(stockMaster, p -> true);
        stockTable.setItems(stockFiltered);

        stockSearchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            stockFiltered.setPredicate(p ->
                lower.isBlank()
                || p.getName().toLowerCase().contains(lower)
                || (p.getBarcode() != null && p.getBarcode().contains(val))
                || p.getCategoryName().toLowerCase().contains(lower)
            );
        });

        boolean canAdjust = SessionContext.getInstance().hasPermission("INVENTORY_ADJUST");
        btnAdjust.setDisable(!canAdjust);
        btnDamage.setDisable(!canAdjust);
    }

    private void loadStockData() {
        try {
            stockMaster.setAll(inventoryService.getLowStockProducts());
            // Merge with all active products (getLowStock only returns low items)
            // Load all products for the full stock view
            com.gp.grocerypos.dao.ProductDAO dao =
                new com.gp.grocerypos.dao.ProductDAOImpl();
            stockMaster.setAll(dao.findAll());
            stockStatusLabel.setText("Showing " + stockMaster.size() + " products.");
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    @FXML
    private void handleAdjust() {
        Product sel = stockTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.showWarning("No Selection", "Please select a product to adjust.");
            return;
        }
        openAdjustDialog(sel, false);
    }

    @FXML
    private void handleDamage() {
        Product sel = stockTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.showWarning("No Selection", "Please select a product to record damage for.");
            return;
        }
        openAdjustDialog(sel, true);
    }

    @FXML
    private void handleStockRefresh() {
        stockSearchField.clear();
        loadStockData();
    }

    private void openAdjustDialog(Product product, boolean isDamage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/inventory/StockAdjustDialog.fxml"));
            Parent root = loader.load();
            StockAdjustDialogController ctrl = loader.getController();
            ctrl.init(product, isDamage);

            Stage dialog = new Stage();
            dialog.setTitle(isDamage ? "Record Damage" : "Adjust Stock");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

            if (ctrl.isSaved()) {
                loadStockData();
                loadLowStockData();
            }
        } catch (IOException e) {
            log.error("Failed to open StockAdjustDialog: {}", e.getMessage(), e);
            AlertUtil.showError("Error", "Could not open the adjustment dialog.");
        }
    }

    // =========================================================================
    // TAB 2 — MOVEMENTS
    // =========================================================================

    private void setupMovementsTab() {
        mColDate.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCreatedAt() != null
                ? d.getValue().getCreatedAt().format(DT_FMT) : ""));
        mColProduct.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getProduct() != null
                ? d.getValue().getProduct().getName() : ""));
        mColType.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getMovementType() != null
                ? d.getValue().getMovementType().name() : ""));
        mColQty.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getQuantity())));
        mColPrev.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getPreviousStock())));
        mColNew.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getNewStock())));
        mColRef.setCellValueFactory(d -> {
            String ref = d.getValue().getReferenceType();
            Integer id = d.getValue().getReferenceId();
            return new SimpleStringProperty(
                ref != null && id != null ? ref + "-" + id : "");
        });
        mColUser.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getUser() != null
                ? d.getValue().getUser().getFullName() : ""));
        mColReason.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getReason() != null
                ? d.getValue().getReason() : ""));

        // Colour movement type
        mColType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) { setText(null); setStyle(""); return; }
                setText(type);
                setStyle(switch (type) {
                    case "PURCHASE", "RETURN_IN", "TRANSFER_IN" ->
                        "-fx-text-fill: #2e7d32; -fx-font-weight: bold;";
                    case "SALE", "DAMAGE", "TRANSFER_OUT", "RETURN_OUT" ->
                        "-fx-text-fill: #c62828; -fx-font-weight: bold;";
                    default -> "-fx-text-fill: #1565c0; -fx-font-weight: bold;";
                });
            }
        });

        // Default date range: last 30 days
        movToDate.setValue(LocalDate.now());
        movFromDate.setValue(LocalDate.now().minusDays(30));
    }

    @FXML
    private void handleLoadMovements() {
        LocalDate from = movFromDate.getValue();
        LocalDate to   = movToDate.getValue();
        if (from == null || to == null) {
            AlertUtil.showWarning("Date Required", "Please select a date range.");
            return;
        }
        if (from.isAfter(to)) {
            AlertUtil.showWarning("Invalid Range", "From date must be before To date.");
            return;
        }
        try {
            var movements = inventoryService.getMovementsByDateRange(from, to);
            movTable.setItems(FXCollections.observableArrayList(movements));
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    // =========================================================================
    // TAB 3 — LOW STOCK
    // =========================================================================

    private void setupLowStockTab() {
        lsColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        lsColCategory.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCategoryName()));
        lsColCurrent.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getCurrentStock())));
        lsColMinimum.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getMinimumStock())));
        lsColUnit.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getUnit() != null
                ? d.getValue().getUnit().getLabel() : ""));

        lsColCurrent.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                Product p = getTableView().getItems().get(getIndex());
                setStyle(p.isOutOfStock()
                    ? "-fx-text-fill: #c62828; -fx-font-weight: bold;"
                    : "-fx-text-fill: #e65100; -fx-font-weight: bold;");
            }
        });

        loadLowStockData();
    }

    private void loadLowStockData() {
        try {
            var list = inventoryService.getLowStockProducts();
            lowStockTable.setItems(FXCollections.observableArrayList(list));
            lowStockLabel.setText(list.isEmpty()
                ? "No low-stock products." : list.size() + " product(s) need restocking.");
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    @FXML
    private void handleLowStockRefresh() { loadLowStockData(); }

    // =========================================================================
    // TAB 4 — EXPIRY
    // =========================================================================

    private void setupExpiryTab() {
        exColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        exColCategory.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCategoryName()));
        exColStock.setCellValueFactory(d ->
            new SimpleStringProperty(fmt(d.getValue().getCurrentStock())));
        exColExpiry.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getExpiryDate() != null
                ? d.getValue().getExpiryDate().format(D_FMT) : ""));
        exColAlert.setCellValueFactory(d -> {
            LocalDate expiry = d.getValue().getExpiryDate();
            if (expiry == null) return new SimpleStringProperty("");
            if (expiry.isBefore(LocalDate.now())) return new SimpleStringProperty("EXPIRED");
            long days = LocalDate.now().until(expiry).getDays();
            return new SimpleStringProperty("Expires in " + days + " day(s)");
        });

        exColAlert.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                setStyle("EXPIRED".equals(val)
                    ? "-fx-text-fill: #c62828; -fx-font-weight: bold;"
                    : "-fx-text-fill: #e65100; -fx-font-weight: bold;");
            }
        });

        loadExpiryData();
    }

    private void loadExpiryData() {
        try {
            var expired  = inventoryService.getExpiredProducts();
            var expiring = inventoryService.getExpiringSoonProducts();
            var combined = new java.util.ArrayList<Product>(expired);
            // Add expiring-soon items not already in expired list
            expiring.stream()
                .filter(p -> combined.stream().noneMatch(e -> e.getId() == p.getId()))
                .forEach(combined::add);
            expiryTable.setItems(FXCollections.observableArrayList(combined));
            expiryLabel.setText(expired.size() + " expired, "
                + expiring.size() + " expiring soon.");
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    @FXML
    private void handleExpiryRefresh() { loadExpiryData(); }

    // ── Util ──────────────────────────────────────────────────────────────────

    private String fmt(BigDecimal val) {
        return val != null ? val.stripTrailingZeros().toPlainString() : "0";
    }
}
