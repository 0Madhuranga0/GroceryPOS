package com.gp.grocerypos.controller.product;

import com.gp.grocerypos.model.Category;
import com.gp.grocerypos.service.CategoryService;
import com.gp.grocerypos.service.CategoryServiceImpl;
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

/**
 * Controller for the Category list screen.
 * Displays all categories in a TableView with search, add, edit, and
 * deactivate actions.
 */
public class CategoryController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private TextField                  searchField;
    @FXML private TableView<Category>        tableView;
    @FXML private TableColumn<Category, Integer> colId;
    @FXML private TableColumn<Category, String>  colName;
    @FXML private TableColumn<Category, String>  colDescription;
    @FXML private TableColumn<Category, String>  colStatus;
    @FXML private Button                     btnAdd;
    @FXML private Button                     btnEdit;
    @FXML private Button                     btnToggleStatus;
    @FXML private Label                      statusLabel;

    private final CategoryService categoryService = new CategoryServiceImpl();
    private ObservableList<Category> masterList;
    private FilteredList<Category>   filteredList;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupSearch();
        setupButtonPermissions();
        loadData();
        log.debug("CategoryController initialised.");
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStatus.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getStatus().name()));

        // Colour-code the status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    setStyle("ACTIVE".equals(status)
                            ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                            : "-fx-text-fill: #c62828; -fx-font-weight: bold;");
                }
            }
        });

        // Update toggle button label when selection changes
        tableView.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> updateToggleButton(selected));
    }

    private void setupSearch() {
        masterList  = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(masterList, p -> true);
        tableView.setItems(filteredList);

        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(c ->
                lower.isBlank()
                || c.getName().toLowerCase().contains(lower)
                || (c.getDescription() != null
                    && c.getDescription().toLowerCase().contains(lower))
            );
        });
    }

    private void setupButtonPermissions() {
        boolean canEdit = SessionContext.getInstance().hasPermission("PRODUCT_EDIT");
        boolean canCreate = SessionContext.getInstance().hasPermission("PRODUCT_CREATE");
        btnAdd.setDisable(!canCreate);
        btnEdit.setDisable(!canEdit);
        btnToggleStatus.setDisable(!canEdit);
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            masterList.setAll(categoryService.getAllCategories());
            setStatus("Loaded " + masterList.size() + " categories.");
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML
    private void handleAdd() {
        openDialog(null);
    }

    @FXML
    private void handleEdit() {
        Category selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("No Selection", "Please select a category to edit.");
            return;
        }
        openDialog(selected);
    }

    @FXML
    private void handleToggleStatus() {
        Category selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("No Selection", "Please select a category first.");
            return;
        }
        boolean isActive = selected.isActive();
        String action = isActive ? "deactivate" : "activate";
        if (!AlertUtil.showConfirm("Confirm", "Are you sure you want to " + action
                + " '" + selected.getName() + "'?")) return;

        try {
            if (isActive) {
                categoryService.deactivateCategory(selected.getId());
            } else {
                categoryService.activateCategory(selected.getId());
            }
            loadData();
            setStatus("Category '" + selected.getName() + "' " + action + "d.");
        } catch (Exception e) {
            AlertUtil.showException("Error", e);
        }
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadData();
    }

    // ── Dialog ────────────────────────────────────────────────────────────────

    private void openDialog(Category category) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/product/CategoryDialog.fxml"));
            Parent root = loader.load();

            CategoryDialogController controller = loader.getController();
            controller.setCategory(category);

            Stage dialog = new Stage();
            dialog.setTitle(category == null ? "Add Category" : "Edit Category");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

            if (controller.isSaved()) {
                loadData();
                setStatus(category == null ? "Category added." : "Category updated.");
            }
        } catch (IOException e) {
            log.error("Failed to open CategoryDialog: {}", e.getMessage(), e);
            AlertUtil.showError("Error", "Could not open the category dialog.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateToggleButton(Category selected) {
        if (selected == null) {
            btnToggleStatus.setText("Deactivate");
        } else {
            btnToggleStatus.setText(selected.isActive() ? "Deactivate" : "Activate");
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
