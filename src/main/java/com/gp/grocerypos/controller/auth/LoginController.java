package com.gp.grocerypos.controller.auth;

import com.gp.grocerypos.GroceryPOSApp;
import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.exception.AuthException;
import com.gp.grocerypos.exception.DatabaseException;
import com.gp.grocerypos.service.AuthService;
import com.gp.grocerypos.service.AuthServiceImpl;
import com.gp.grocerypos.service.SessionContext;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Login screen.
 * <p>
 * Authentication flow:
 * <ol>
 *   <li>User enters username + password and clicks Login (or presses Enter)</li>
 *   <li>Basic blank-field validation on the UI thread</li>
 *   <li>BCrypt verification is run on a background thread (it is intentionally slow)</li>
 *   <li>On success  — load Main.fxml and swap the scene</li>
 *   <li>On failure  — display a user-friendly error, re-enable the form</li>
 * </ol>
 */
public class LoginController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    // ── FXML fields ───────────────────────────────────────────────────────────
    @FXML private Label             titleLabel;
    @FXML private Label             subtitleLabel;
    @FXML private Label             messageLabel;
    @FXML private TextField         usernameField;
    @FXML private PasswordField     passwordField;
    @FXML private Button            loginButton;
    @FXML private ProgressIndicator spinner;
    @FXML private Label             versionLabel;

    private final AuthService authService = new AuthServiceImpl();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AppConfig config = AppConfig.getInstance();
        titleLabel.setText(config.get("store.name", config.getAppName()));
        subtitleLabel.setText("Sign in to continue");
        versionLabel.setText("v" + config.getAppVersion());
        spinner.setVisible(false);
        clearMessage();

        // Allow Enter key on password field to trigger login
        passwordField.setOnKeyPressed(this::handleKeyPress);
        usernameField.setOnKeyPressed(this::handleKeyPress);

        // Auto-focus username field
        Platform.runLater(() -> usernameField.requestFocus());

        log.debug("LoginController initialised.");
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Client-side blank validation (fast, no DB hit)
        if (username.isEmpty()) {
            showError("Please enter your username.");
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter your password.");
            passwordField.requestFocus();
            return;
        }

        // BCrypt is slow — run authentication on a background thread
        // so the UI stays responsive
        setFormBusy(true);

        Task<Void> loginTask = new Task<>() {
            @Override
            protected Void call() {
                authService.login(username, password);
                return null;
            }
        };

        loginTask.setOnSucceeded(e -> {
            setFormBusy(false);
            navigateToMain();
        });

        loginTask.setOnFailed(e -> {
            setFormBusy(false);
            Throwable cause = loginTask.getException();
            if (cause instanceof AuthException ae) {
                showError(ae.getUserMessage());
            } else if (cause instanceof DatabaseException) {
                showError("Unable to connect to the database. Please try again.");
                log.error("Database error during login: {}", cause.getMessage(), cause);
            } else {
                showError("An unexpected error occurred. Please try again.");
                log.error("Unexpected login error: {}", cause.getMessage(), cause);
            }
            passwordField.clear();
            passwordField.requestFocus();
        });

        Thread thread = new Thread(loginTask, "login-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToMain() {
        try {
            // 1. Get the stage BEFORE loading the new FXML
            Stage stage = (Stage) loginButton.getScene().getWindow();
            double width  = stage.getScene().getWidth();
            double height = stage.getScene().getHeight();

            // 2. Load Main.fxml — initialize() runs here, scene is not yet attached
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Main.fxml"));
            Parent mainRoot = loader.load();

            // 3. Build the scene and attach it to the stage
            Scene scene = new Scene(mainRoot, width, height);
            URL css = getClass().getResource("/css/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            stage.setScene(scene);   // scene is now attached — getScene() works
            stage.setMaximized(true);

            // 4. Set window title now that the scene is live
            SessionContext session = com.gp.grocerypos.service.SessionContext.getInstance();
            stage.setTitle(AppConfig.getInstance().getAppName()
                    + " — " + session.getCurrentUser().getFullName());

            log.info("Navigated to main layout after successful login.");

        } catch (IOException e) {
            log.error("Failed to load Main.fxml: {}", e.getMessage(), e);
            showError("Application error: could not load the main screen.");
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void setFormBusy(boolean busy) {
        loginButton.setDisable(busy);
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);
        spinner.setVisible(busy);
        if (busy) clearMessage();
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        messageLabel.setVisible(true);
    }

    private void showInfo(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #27ae60;");
        messageLabel.setVisible(true);
    }

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.setVisible(false);
    }
}
