package com.gp.grocerypos.controller.supplier;

import com.gp.grocerypos.model.Supplier;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Supplier list screen.
 */
public class SupplierController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SupplierController.class);

    @FXML private TextField                    searchField;
    @FXML private TableView<Supplier>          tableView;
    @FXML private TableColumn<Supplier, Integer> colId;
    @FXML private TableColumn<Supplier, String>  colName;
    @FXML private TableColumn<Supplier, String>  colContact;
    @FXML private TableColumn<Supplier, String>  colPhone;
    @FXML private TableColumn<Supplier, String>  colEmail;
    @FXML private TableColumn<Supplier, String>  colStatus;
    @FXML private Button                       btnAdd;
    @FXML private Button                       btnEdit;
    @FXML private Button                       btnToggleStatus;
    @FXML private Label                        statusLabel;

    private final SupplierService supplierService = new SupplierServiceImpl();
    private ObservableList<Supplier> masterList;
    private FilteredList<Supplier>   filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupSearch();
        setupButtonPermissions();
        loadData();
        log.debug("SupplierController initialised.");
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contactPerson"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
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
            filteredList.setPredicate(s ->
                lower.isBlank()
                || s.getName().toLowerCase().contains(lower)
                || (s.getContactPerson() != null && s.getContactPerson().toLowerCase().contains(lower))
                || (s.getPhone()         != null && s.getPhone().contains(val))
            );
        });
    }

    private void setupButtonPermissions() {
        boolean can = SessionContext.getInstance().hasPermission("SUPPLIER_MANAGE");
        btnAdd.setDisable(!can);
        btnEdit.setDisable(!can);
        btnToggleStatus.setDisable(!can);
    }

    private void loadData() {
        try {
            masterList.setAll(supplierService.getAllSuppliers());
            setStatus("Loaded " + masterList.size() + " suppliers.");
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    @FXML private void handleAdd()     { openDialog(null); }

    @FXML
    private void handleEdit() {
        Supplier sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertUtil.showWarning("No Selection", "Please select a supplier to edit."); return; }
        openDialog(sel);
    }

    @FXML
    private void handleToggleStatus() {
        Supplier sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertUtil.showWarning("No Selection", "Please select a supplier first."); return; }
        boolean isActive = sel.isActive();
        String action = isActive ? "deactivate" : "activate";
        if (!AlertUtil.showConfirm("Confirm", "Are you sure you want to " + action
                + " '" + sel.getName() + "'?")) return;
        try {
            if (isActive) supplierService.deactivateSupplier(sel.getId());
            else          supplierService.activateSupplier(sel.getId());
            loadData();
            setStatus("Supplier '" + sel.getName() + "' " + action + "d.");
        } catch (Exception e) { AlertUtil.showException("Error", e); }
    }

    @FXML private void handleRefresh() { searchField.clear(); loadData(); }

    private void openDialog(Supplier supplier) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/supplier/SupplierDialog.fxml"));
            Parent root = loader.load();
            SupplierDialogController ctrl = loader.getController();
            ctrl.setSupplier(supplier);

            Stage dialog = new Stage();
            dialog.setTitle(supplier == null ? "Add Supplier" : "Edit Supplier");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

            if (ctrl.isSaved()) {
                loadData();
                setStatus(supplier == null ? "Supplier added." : "Supplier updated.");
            }
        } catch (IOException e) {
            log.error("Failed to open SupplierDialog: {}", e.getMessage(), e);
            AlertUtil.showError("Error", "Could not open the supplier dialog.");
        }
    }

    private void setStatus(String msg) { statusLabel.setText(msg); }
}
