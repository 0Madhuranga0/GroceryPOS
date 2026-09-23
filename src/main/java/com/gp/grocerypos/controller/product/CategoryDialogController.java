package com.gp.grocerypos.controller.product;

import com.gp.grocerypos.model.Category;
import com.gp.grocerypos.service.CategoryService;
import com.gp.grocerypos.service.CategoryServiceImpl;
import com.gp.grocerypos.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Category add/edit modal dialog.
 */
public class CategoryDialogController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CategoryDialogController.class);

    @FXML private Label             titleLabel;
    @FXML private TextField         nameField;
    @FXML private TextArea          descriptionField;
    @FXML private ComboBox<String>  statusCombo;
    @FXML private Label             errorLabel;
    @FXML private Button            btnSave;
    @FXML private Button            btnCancel;

    private final CategoryService categoryService = new CategoryServiceImpl();
    private Category editingCategory; // null = add mode
    private boolean  saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusCombo.getItems().addAll("ACTIVE", "INACTIVE");
        statusCombo.setValue("ACTIVE");
        errorLabel.setVisible(false);
    }

    // ── Called by opener ──────────────────────────────────────────────────────

    public void setCategory(Category category) {
        this.editingCategory = category;
        if (category == null) {
            titleLabel.setText("Add Category");
        } else {
            titleLabel.setText("Edit Category");
            nameField.setText(category.getName());
            descriptionField.setText(category.getDescription() != null
                    ? category.getDescription() : "");
            statusCombo.setValue(category.getStatus().name());
        }
    }

    public boolean isSaved() { return saved; }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        clearError();
        String name = nameField.getText();
        String desc = descriptionField.getText();
        Category.Status status = Category.Status.valueOf(statusCombo.getValue());

        try {
            if (editingCategory == null) {
                categoryService.createCategory(name, desc);
            } else {
                categoryService.updateCategory(editingCategory.getId(), name, desc, status);
            }
            saved = true;
            closeDialog();
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void closeDialog() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    private void showError(Throwable e) {
        String msg = (e instanceof com.gp.grocerypos.exception.POSException pe)
                ? pe.getUserMessage() : "An error occurred. Please try again.";
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        log.warn("Category dialog error: {}", e.getMessage());
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }
}
