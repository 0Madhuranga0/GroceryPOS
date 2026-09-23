package com.gp.grocerypos.controller.customer;

import com.gp.grocerypos.model.Customer;
import com.gp.grocerypos.service.CustomerService;
import com.gp.grocerypos.service.CustomerServiceImpl;
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
import java.net.URL;
import java.util.ResourceBundle;

public class CustomerController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    @FXML private TextField                     searchField;
    @FXML private TableView<Customer>           tableView;
    @FXML private TableColumn<Customer, Integer> colId;
    @FXML private TableColumn<Customer, String>  colName;
    @FXML private TableColumn<Customer, String>  colPhone;
    @FXML private TableColumn<Customer, String>  colEmail;
    @FXML private TableColumn<Customer, Integer> colPoints;
    @FXML private TableColumn<Customer, String>  colStatus;
    @FXML private Button                        btnAdd;
    @FXML private Button                        btnEdit;
    @FXML private Button                        btnToggleStatus;
    @FXML private Label                         statusLabel;

    private final CustomerService customerService = new CustomerServiceImpl();
    private ObservableList<Customer> masterList;
    private FilteredList<Customer>   filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupSearch();
        setupPermissions();
        loadData();
        log.debug("CustomerController initialised.");
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPoints.setCellValueFactory(new PropertyValueFactory<>("loyaltyPoints"));
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getStatus().name()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("ACTIVE".equals(s)
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
            filteredList.setPredicate(c ->
                lower.isBlank()
                || c.getName().toLowerCase().contains(lower)
                || (c.getPhone() != null && c.getPhone().contains(val))
                || (c.getEmail() != null && c.getEmail().toLowerCase().contains(lower))
            );
        });
    }

    private void setupPermissions() {
        boolean canManage = SessionContext.getInstance().hasPermission("CUSTOMER_MANAGE");
        btnEdit.setDisable(!canManage);
        btnToggleStatus.setDisable(!canManage);
        // Anyone logged in can add customers (needed for POS)
    }

    private void loadData() {
        try {
            masterList.setAll(customerService.getAllCustomers());
            statusLabel.setText("Loaded " + masterList.size() + " customers.");
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    @FXML private void handleAdd() { openDialog(null); }

    @FXML
    private void handleEdit() {
        Customer sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.showWarning("No Selection", "Please select a customer to edit.");
            return;
        }
        openDialog(sel);
    }

    @FXML
    private void handleToggleStatus() {
        Customer sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.showWarning("No Selection", "Please select a customer first.");
            return;
        }
        boolean isActive = sel.isActive();
        String action = isActive ? "deactivate" : "activate";
        if (!AlertUtil.showConfirm("Confirm",
                "Are you sure you want to " + action + " '" + sel.getName() + "'?")) return;
        try {
            if (isActive) customerService.deactivateCustomer(sel.getId());
            else          customerService.activateCustomer(sel.getId());
            loadData();
        } catch (Exception e) {
            AlertUtil.showException("Error", e);
        }
    }

    @FXML private void handleRefresh() { searchField.clear(); loadData(); }

    private void openDialog(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/customer/CustomerDialog.fxml"));
            Parent root = loader.load();
            CustomerDialogController ctrl = loader.getController();
            ctrl.setCustomer(customer);

            Stage dialog = new Stage();
            dialog.setTitle(customer == null ? "Add Customer" : "Edit Customer");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

            if (ctrl.isSaved()) loadData();
        } catch (IOException e) {
            log.error("Failed to open CustomerDialog: {}", e.getMessage(), e);
            AlertUtil.showError("Error", "Could not open the customer dialog.");
        }
    }
}
