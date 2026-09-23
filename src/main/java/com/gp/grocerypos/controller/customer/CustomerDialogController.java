package com.gp.grocerypos.controller.customer;

import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.model.Customer;
import com.gp.grocerypos.service.CustomerService;
import com.gp.grocerypos.service.CustomerServiceImpl;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class CustomerDialogController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CustomerDialogController.class);

    @FXML private Label            titleLabel;
    @FXML private TextField        nameField;
    @FXML private TextField        phoneField;
    @FXML private TextField        emailField;
    @FXML private TextArea         addressField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label            errorLabel;
    @FXML private Button           btnSave;
    @FXML private Button           btnCancel;

    private final CustomerService customerService = new CustomerServiceImpl();
    private Customer editingCustomer;
    private boolean  saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusCombo.getItems().addAll("ACTIVE", "INACTIVE");
        statusCombo.setValue("ACTIVE");
        errorLabel.setVisible(false);
    }

    public void setCustomer(Customer customer) {
        this.editingCustomer = customer;
        if (customer == null) {
            titleLabel.setText("Add Customer");
        } else {
            titleLabel.setText("Edit Customer");
            nameField.setText(orEmpty(customer.getName()));
            phoneField.setText(orEmpty(customer.getPhone()));
            emailField.setText(orEmpty(customer.getEmail()));
            addressField.setText(orEmpty(customer.getAddress()));
            statusCombo.setValue(customer.getStatus().name());
        }
    }

    /** Allow external callers (e.g. POS quick-add) to get the saved customer. */
    public boolean isSaved() { return saved; }

    @FXML
    private void handleSave() {
        clearError();
        Customer c = editingCustomer != null ? editingCustomer : new Customer();
        c.setName(nameField.getText());
        c.setPhone(phoneField.getText());
        c.setEmail(emailField.getText());
        c.setAddress(addressField.getText());
        c.setStatus(Customer.Status.valueOf(statusCombo.getValue()));

        try {
            if (editingCustomer == null) customerService.createCustomer(c);
            else                        customerService.updateCustomer(c);
            saved = true;
            closeDialog();
        } catch (POSException e) {
            showError(e.getUserMessage());
        } catch (Exception e) {
            showError("An error occurred. Please try again.");
            log.error("Customer dialog error: {}", e.getMessage(), e);
        }
    }

    @FXML private void handleCancel() { closeDialog(); }

    private void closeDialog() { ((Stage) btnCancel.getScene().getWindow()).close(); }
    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void clearError()          { errorLabel.setText(""); errorLabel.setVisible(false); }
    private String orEmpty(String s)   { return s != null ? s : ""; }
}
