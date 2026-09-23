package com.gp.grocerypos.controller.users;

import com.gp.grocerypos.model.User;
import com.gp.grocerypos.service.AuthService;
import com.gp.grocerypos.service.AuthServiceImpl;
import com.gp.grocerypos.service.UserService;
import com.gp.grocerypos.service.UserServiceImpl;
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
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller for the User Management list screen.
 * Requires USER_MANAGE permission — enforced in UserService.
 */
public class UserController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TextField                    searchField;
    @FXML private TableView<User>              tableView;
    @FXML private TableColumn<User, Integer>   colId;
    @FXML private TableColumn<User, String>    colUsername;
    @FXML private TableColumn<User, String>    colFullName;
    @FXML private TableColumn<User, String>    colRole;
    @FXML private TableColumn<User, String>    colStatus;
    @FXML private TableColumn<User, String>    colLastLogin;
    @FXML private Button                       btnAdd;
    @FXML private Button                       btnEdit;
    @FXML private Button                       btnToggleStatus;
    @FXML private Button                       btnResetPassword;
    @FXML private Label                        statusLabel;

    private final UserService userService = new UserServiceImpl();
    private final AuthService authService = new AuthServiceImpl();

    private ObservableList<User> masterList;
    private FilteredList<User>   filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupSearch();
        loadData();
        log.debug("UserController initialised.");
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRole.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getRoleName()));
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

        colLastLogin.setCellValueFactory(d -> {
            var dt = d.getValue().getLastLogin();
            return new SimpleStringProperty(dt != null ? dt.format(DT) : "Never");
        });

        tableView.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> {
                boolean hasSel = sel != null;
                btnEdit.setDisable(!hasSel);
                btnResetPassword.setDisable(!hasSel);
                btnToggleStatus.setText(hasSel && sel.isActive() ? "Deactivate" : "Activate");
                btnToggleStatus.setDisable(!hasSel);
            });
    }

    private void setupSearch() {
        masterList   = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(masterList, p -> true);
        tableView.setItems(filteredList);

        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val == null ? "" : val.toLowerCase();
            filteredList.setPredicate(u ->
                lower.isBlank()
                || u.getUsername().toLowerCase().contains(lower)
                || u.getFullName().toLowerCase().contains(lower)
                || u.getRoleName().toLowerCase().contains(lower)
            );
        });
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            masterList.setAll(userService.getAllUsers());
            statusLabel.setText("Loaded " + masterList.size() + " users.");
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML private void handleAdd()     { openDialog(null); }

    @FXML
    private void handleEdit() {
        User sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertUtil.showWarning("No Selection", "Please select a user to edit."); return; }
        openDialog(sel);
    }

    @FXML
    private void handleToggleStatus() {
        User sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        boolean isActive = sel.isActive();
        String action = isActive ? "deactivate" : "activate";
        if (!AlertUtil.showConfirm("Confirm",
                "Are you sure you want to " + action + " user '" + sel.getUsername() + "'?")) return;
        try {
            if (isActive) userService.deactivateUser(sel.getId());
            else          userService.activateUser(sel.getId());
            loadData();
        } catch (Exception e) {
            AlertUtil.showException("Error", e);
        }
    }

    @FXML
    private void handleResetPassword() {
        User sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Reset password for: " + sel.getFullName());
        dialog.setContentText("New password (min 6 characters):");
        dialog.showAndWait().ifPresent(newPwd -> {
            if (newPwd.isBlank()) return;
            try {
                authService.resetPassword(sel.getId(), newPwd);
                AlertUtil.showInfo("Password Reset",
                    "Password for '" + sel.getUsername() + "' has been reset.");
            } catch (Exception e) {
                AlertUtil.showException("Reset Failed", e);
            }
        });
    }

    @FXML private void handleRefresh() { searchField.clear(); loadData(); }

    // ── Dialog ────────────────────────────────────────────────────────────────

    private void openDialog(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/users/UserDialog.fxml"));
            Parent root = loader.load();
            UserDialogController ctrl = loader.getController();
            ctrl.setUser(user);

            Stage dialog = new Stage();
            dialog.setTitle(user == null ? "Add User" : "Edit User");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();

            if (ctrl.isSaved()) loadData();
        } catch (IOException e) {
            log.error("Failed to open UserDialog: {}", e.getMessage(), e);
            AlertUtil.showError("Error", "Could not open the user dialog.");
        }
    }
}
