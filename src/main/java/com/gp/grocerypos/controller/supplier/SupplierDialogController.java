package com.gp.grocerypos.controller.supplier;

import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.model.Supplier;
import com.gp.grocerypos.service.SupplierService;
import com.gp.grocerypos.service.SupplierServiceImpl;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Supplier add/edit modal dialog.
 */
public class SupplierDialogController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SupplierDialogController.class);

    @FXML private Label             titleLabel;
    @FXML private TextField         nameField;
    @FXML private TextField         contactField;
    @FXML private TextField         phoneField;
    @FXML private TextField         emailField;
    @FXML private TextArea          addressField;
    @FXML private TextArea          notesField;
    @FXML private ComboBox<String>  statusCombo;
    @FXML private Label             errorLabel;
    @FXML private Button            btnSave;
    @FXML private Button            btnCancel;

    private final SupplierService supplierService = new SupplierServiceImpl();
    private Supplier editingSupplier;
    private boolean  saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusCombo.getItems().addAll("ACTIVE", "INACTIVE");
        statusCombo.setValue("ACTIVE");
        errorLabel.setVisible(false);
    }

    public void setSupplier(Supplier supplier) {
        this.editingSupplier = supplier;
        if (supplier == null) {
            titleLabel.setText("Add Supplier");
        } else {
            titleLabel.setText("Edit Supplier");
            nameField.setText(supplier.getName());
            contactField.setText(orEmpty(supplier.getContactPerson()));
            phoneField.setText(orEmpty(supplier.getPhone()));
            emailField.setText(orEmpty(supplier.getEmail()));
            addressField.setText(orEmpty(supplier.getAddress()));
            notesField.setText(orEmpty(supplier.getNotes()));
            statusCombo.setValue(supplier.getStatus().name());
        }
    }

    public boolean isSaved() { return saved; }

    @FXML
    private void handleSave() {
        clearError();
        Supplier s = editingSupplier != null ? editingSupplier : new Supplier();
        s.setName(nameField.getText());
        s.setContactPerson(contactField.getText());
        s.setPhone(phoneField.getText());
        s.setEmail(emailField.getText());
        s.setAddress(addressField.getText());
        s.setNotes(notesField.getText());
        s.setStatus(Supplier.Status.valueOf(statusCombo.getValue()));

        try {
            if (editingSupplier == null) {
                supplierService.createSupplier(s);
            } else {
                supplierService.updateSupplier(s);
            }
            saved = true;
            closeDialog();
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void handleCancel() { closeDialog(); }

    private void closeDialog() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    private void showError(Throwable e) {
        String msg = (e instanceof POSException pe)
                ? pe.getUserMessage() : "An error occurred. Please try again.";
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        log.warn("Supplier dialog error: {}", e.getMessage());
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private String orEmpty(String s) { return s != null ? s : ""; }
}
