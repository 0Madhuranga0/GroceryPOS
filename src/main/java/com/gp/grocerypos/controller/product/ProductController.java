package com.gp.grocerypos.controller.product;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.model.Product;
import com.gp.grocerypos.service.ProductService;
import com.gp.grocerypos.service.ProductServiceImpl;
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
import java.util.ResourceBundle;

/**
 * Controller for the Product list screen.
 * Provides search, add, edit, activate/deactivate, and low-stock filter.
 */
public class ProductController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private TextField                   searchField;
    @FXML private ComboBox<String>            filterCombo;
    @FXML private TableView<Product>          tableView;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String>  colBarcode;
    @FXML private TableColumn<Product, String>  colName;
    @FXML private TableColumn<Product, String>  colCategory;
    @FXML private TableColumn<Product, String>  colUnit;
    @FXML private TableColumn<Product, String>  colSellingPrice;
    @FXML private TableColumn<Product, String>  colStock;
    @FXML private TableColumn<Product, String>  colStatus;
    @FXML private Button                      btnAdd;
    @FXML private Button                      btnEdit;
    @FXML private Button                      btnToggleStatus;
    @FXML private Label                       statusLabel;

    private final ProductService productService = new ProductServiceImpl();
    private final String         currency       = AppConfig.getInstance().getCurrencySymbol();

    private ObservableList<Product> masterList;
    private FilteredList<Product>   filteredList;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilter();
        setupColumns();
        setupSearch();
        setupButtonPermissions();
        loadData("ALL");
        log.debug("ProductController initialised.");
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupFilter() {
        filterCombo.getItems().addAll(
            "ALL", "ACTIVE", "INACTIVE", "LOW STOCK", "EXPIRING SOON", "EXPIRED");
        filterCombo.setValue("ALL");
        filterCombo.setOnAction(e -> loadData(filterCombo.getValue()));
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colBarcode.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        colCategory.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCategoryName()));

        colUnit.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getUnit() != null
                ? data.getValue().getUnit().getLabel() : ""));

        colSellingPrice.setCellValueFactory(data -> {
            BigDecimal price = data.getValue().getSellingPrice();
            return new SimpleStringProperty(price != null
                ? currency + " " + String.format("%,.2f", price) : "");
        });

        colStock.setCellValueFactory(data -> {
            BigDecimal stock = data.getValue().getCurrentStock();
            return new SimpleStringProperty(stock != null
                ? stock.stripTrailingZeros().toPlainString() : "0");
        });

        // Colour-code stock: red if low/out, orange if at minimum
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                Product p = getTableView().getItems().get(getIndex());
                if (p.isOutOfStock()) {
                    setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                } else if (p.isLowStock()) {
                    setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #2e7d32;");
                }
            }
        });

        colStatus.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStatus().name()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                setStyle("ACTIVE".equals(status)
                    ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                    : "-fx-text-fill: #c62828; -fx-font-weight: bold;");
            }
        });

        tableView.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> btnToggleStatus.setText(
                sel == null ? "Deactivate" : (sel.isActive() ? "Deactivate" : "Activate")));
    }

    private void setupSearch() {
        masterList   = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(masterList, p -> true);
        tableView.setItems(filteredList);

        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(p ->
                lower.isBlank()
                || p.getName().toLowerCase().contains(lower)
                || (p.getBarcode() != null && p.getBarcode().contains(val))
                || (p.getBrand()   != null && p.getBrand().toLowerCase().contains(lower))
                || p.getCategoryName().toLowerCase().contains(lower)
            );
        });
    }

    private void setupButtonPermissions() {
        boolean canCreate = SessionContext.getInstance().hasPermission("PRODUCT_CREATE");
        boolean canEdit   = SessionContext.getInstance().hasPermission("PRODUCT_EDIT");
        btnAdd.setDisable(!canCreate);
        btnEdit.setDisable(!canEdit);
        btnToggleStatus.setDisable(!canEdit);
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData(String filter) {
        try {
            java.util.List<Product> products = switch (filter) {
                case "ACTIVE"        -> productService.getActiveProducts();
                case "INACTIVE"      -> productService.getAllProducts().stream()
                                            .filter(p -> !p.isActive()).toList();
                case "LOW STOCK"     -> productService.getLowStockProducts();
                case "EXPIRING SOON" -> productService.getExpiringSoonProducts();
                case "EXPIRED"       -> productService.getExpiredProducts();
                default              -> productService.getAllProducts();
            };
            masterList.setAll(products);
            setStatus("Showing " + masterList.size() + " products.");
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML private void handleAdd() { openDialog(null); }

    @FXML
    private void handleEdit() {
        Product sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.showWarning("No Selection", "Please select a product to edit.");
            return;
        }
        // Reload from DB to get full object (list may have lightweight version)
        try {
            Product full = productService.getProductById(sel.getId());
            openDialog(full);
        } catch (Exception e) {
            AlertUtil.showException("Error", e);
        }
    }

    @FXML
    private void handleToggleStatus() {
        Product sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.showWarning("No Selection", "Please select a product first.");
            return;
        }
        boolean isActive = sel.isActive();
        String action = isActive ? "deactivate" : "activate";
        if (!AlertUtil.showConfirm("Confirm",
                "Are you sure you want to " + action + " '" + sel.getName() + "'?")) return;
        try {
            if (isActive) productService.deactivateProduct(sel.getId());
            else          productService.activateProduct(sel.getId());
            loadData(filterCombo.getValue());
            setStatus("Product '" + sel.getName() + "' " + action + "d.");
        } catch (Exception e) {
            AlertUtil.showException("Error", e);
        }
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadData(filterCombo.getValue());
    }

    // ── Dialog ────────────────────────────────────────────────────────────────

    private void openDialog(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/product/ProductDialog.fxml"));
            Parent root = loader.load();
            ProductDialogController ctrl = loader.getController();
            ctrl.setProduct(product);

            Stage dialog = new Stage();
            dialog.setTitle(product == null ? "Add Product" : "Edit Product");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(true);
            dialog.setMinWidth(600);
            dialog.setMinHeight(700);
            dialog.showAndWait();

            if (ctrl.isSaved()) {
                loadData(filterCombo.getValue());
                setStatus(product == null ? "Product added." : "Product updated.");
            }
        } catch (IOException e) {
            log.error("Failed to open ProductDialog: {}", e.getMessage(), e);
            AlertUtil.showError("Error", "Could not open the product dialog.");
        }
    }

    private void setStatus(String msg) { statusLabel.setText(msg); }
}
