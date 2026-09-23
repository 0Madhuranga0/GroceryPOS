package com.gp.grocerypos.controller.users;

import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.model.Role;
import com.gp.grocerypos.model.User;
import com.gp.grocerypos.service.UserService;
import com.gp.grocerypos.service.UserServiceImpl;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the User add/edit modal dialog.
 */
public class UserDialogController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(UserDialogController.class);

    @FXML private Label             titleLabel;
    @FXML private TextField         usernameField;
    @FXML private TextField         fullNameField;
    @FXML private TextField         emailField;
    @FXML private TextField         phoneField;
    @FXML private ComboBox<Role>    roleCombo;
    @FXML private ComboBox<String>  statusCombo;
    @FXML private PasswordField     passwordField;
    @FXML private PasswordField     confirmPasswordField;
    @FXML private Label             passwordSectionLabel;
    @FXML private Label             errorLabel;
    @FXML private Button            btnSave;
    @FXML private Button            btnCancel;

    private final UserService userService = new UserServiceImpl();
    private User    editingUser;
    private boolean saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        errorLabel.setVisible(false);

        statusCombo.getItems().addAll("ACTIVE", "INACTIVE");
        statusCombo.setValue("ACTIVE");

        // Load roles
        List<Role> roles = userService.getAllRoles();
        roleCombo.getItems().addAll(roles);
        roleCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Role r, boolean empty) {
                super.updateItem(r, empty);
                setText(r == null ? "-- Select Role --" : r.getName());
            }
        });
        roleCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Role r, boolean empty) {
                super.updateItem(r, empty);
                setText(r == null ? "" : r.getName()
                    + (r.getDescription() != null ? " — " + r.getDescription() : ""));
            }
        });
    }

    public void setUser(User user) {
        this.editingUser = user;
        if (user == null) {
            titleLabel.setText("Add User");
            passwordSectionLabel.setText("Password *");
        } else {
            titleLabel.setText("Edit User");
            passwordSectionLabel.setText("New Password (leave blank to keep current)");
            usernameField.setText(user.getUsername());
            fullNameField.setText(orEmpty(user.getFullName()));
            emailField.setText(orEmpty(user.getEmail()));
            phoneField.setText(orEmpty(user.getPhone()));
            statusCombo.setValue(user.getStatus().name());
            // Select matching role
            roleCombo.getItems().stream()
                .filter(r -> r != null && r.getId() == user.getRole().getId())
                .findFirst()
                .ifPresent(roleCombo::setValue);
        }
    }

    public boolean isSaved() { return saved; }

    @FXML
    private void handleSave() {
        clearError();

        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        // Password validation
        if (editingUser == null && password.isBlank()) {
            showError("Password is required for new users.");
            return;
        }
        if (!password.isBlank() && !password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        // Build user object
        User u = editingUser != null ? editingUser : new User();
        u.setUsername(usernameField.getText().trim());
        u.setFullName(fullNameField.getText().trim());
        u.setEmail(emailField.getText().trim());
        u.setPhone(phoneField.getText().trim());
        u.setRole(roleCombo.getValue());
        u.setStatus(User.Status.valueOf(statusCombo.getValue()));

        try {
            if (editingUser == null) {
                userService.createUser(u, password);
            } else {
                userService.updateUser(u);
                // Handle optional password change
                if (!password.isBlank()) {
                    new com.gp.grocerypos.service.AuthServiceImpl()
                        .resetPassword(u.getId(), password);
                }
            }
            saved = true;
            closeDialog();
        } catch (POSException e) {
            showError(e.getUserMessage());
        } catch (Exception e) {
            showError("An error occurred. Please try again.");
            log.error("User dialog error: {}", e.getMessage(), e);
        }
    }

    @FXML private void handleCancel() { closeDialog(); }

    private void closeDialog() { ((Stage) btnCancel.getScene().getWindow()).close(); }
    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void clearError()          { errorLabel.setText(""); errorLabel.setVisible(false); }
    private String orEmpty(String s)   { return s != null ? s : ""; }
}
