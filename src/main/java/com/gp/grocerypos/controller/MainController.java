package com.gp.grocerypos.controller;

import com.gp.grocerypos.GroceryPOSApp;
import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.service.AuthService;
import com.gp.grocerypos.service.AuthServiceImpl;
import com.gp.grocerypos.service.SessionContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the main application shell (Main.fxml).
 * <p>
 * The shell is a {@link BorderPane}:
 * <ul>
 *   <li>LEFT  — sidebar navigation (VBox of nav buttons)</li>
 *   <li>TOP   — header bar (store name, user info, logout)</li>
 *   <li>CENTER — content area (StackPane — swaps module views)</li>
 * </ul>
 * Every navigation button calls {@link #loadView(String)} which replaces
 * the center pane content without reloading the shell.
 */
public class MainController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    // ── FXML injected fields ──────────────────────────────────────────────────
    @FXML private BorderPane  rootPane;
    @FXML private StackPane   contentArea;

    // Header
    @FXML private Label  storeNameLabel;
    @FXML private Label  userNameLabel;
    @FXML private Label  userRoleLabel;

    // Sidebar nav buttons
    @FXML private Button navDashboard;
    @FXML private Button navPOS;
    @FXML private Button navProducts;
    @FXML private Button navInventory;
    @FXML private Button navPurchases;
    @FXML private Button navSuppliers;
    @FXML private Button navCustomers;
    @FXML private Button navSales;
    @FXML private Button navReturns;
    @FXML private Button navReports;
    @FXML private Button navUsers;
    @FXML private Button navSettings;

    private final AuthService authService = new AuthServiceImpl();
    private Button            activeNavButton;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SessionContext session = SessionContext.getInstance();

        // Populate header
        AppConfig config = AppConfig.getInstance();
        storeNameLabel.setText(config.get("store.name", config.getAppName()));

        if (session.isLoggedIn()) {
            userNameLabel.setText(session.getCurrentUser().getFullName());
            userRoleLabel.setText(session.getCurrentRoleName());
        }

        // Apply permission-based visibility to admin-only nav items
        applyPermissions();

        // Default view: Dashboard
        setActiveButton(navDashboard);
        loadView("/fxml/dashboard/Dashboard.fxml");

        log.debug("MainController initialised.");
    }

    // ── Navigation handlers ───────────────────────────────────────────────────

    @FXML private void handleNavDashboard()  { setActiveButton(navDashboard);  loadView("/fxml/dashboard/Dashboard.fxml"); }
    @FXML private void handleNavPOS()        { setActiveButton(navPOS);         loadView("/fxml/pos/POS.fxml"); }
    @FXML private void handleNavProducts()   { setActiveButton(navProducts);    loadView("/fxml/product/ProductList.fxml"); }
    @FXML private void handleNavInventory()  { setActiveButton(navInventory);   loadView("/fxml/inventory/Inventory.fxml"); }
    @FXML private void handleNavPurchases()  { setActiveButton(navPurchases);   loadView("/fxml/purchase/PurchaseList.fxml"); }
    @FXML private void handleNavSuppliers()  { setActiveButton(navSuppliers);   loadView("/fxml/supplier/SupplierList.fxml"); }
    @FXML private void handleNavCustomers()  { setActiveButton(navCustomers);   loadView("/fxml/customer/CustomerList.fxml"); }
    @FXML private void handleNavSales()      { setActiveButton(navSales);        loadView("/fxml/sales/SalesList.fxml"); }
    @FXML private void handleNavReturns()    { setActiveButton(navReturns);       loadView("/fxml/sales/Returns.fxml"); }
    @FXML private void handleNavReports()    { setActiveButton(navReports);       loadView("/fxml/reports/Reports.fxml"); }
    @FXML private void handleNavUsers()      { setActiveButton(navUsers);        loadView("/fxml/users/UserList.fxml"); }
    @FXML private void handleNavSettings()   { setActiveButton(navSettings);     loadView("/fxml/settings/Settings.fxml"); }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to log out?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Confirm Logout");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                authService.logout();
                navigateToLogin();
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Loads an FXML view into the center content area.
     * Falls back to a placeholder label if the FXML file doesn't exist yet
     * (useful during incremental development).
     */
    private void loadView(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                // Module not built yet — show a placeholder
                contentArea.getChildren().setAll(buildPlaceholder(fxmlPath));
                return;
            }
            Parent view = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(view);
            log.debug("Loaded view: {}", fxmlPath);
        } catch (IOException e) {
            log.error("Failed to load view '{}': {}", fxmlPath, e.getMessage(), e);
            contentArea.getChildren().setAll(buildPlaceholder(fxmlPath));
        }
    }

    /** Highlights the active sidebar button and removes highlight from previous. */
    private void setActiveButton(Button button) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-button-active");
        }
        activeNavButton = button;
        if (button != null) {
            button.getStyleClass().add("nav-button-active");
        }
    }

    /** Hides nav items the current user has no permission to access. */
    private void applyPermissions() {
        SessionContext session = SessionContext.getInstance();

        // Admin-only items
        setNavVisible(navProducts,  session.hasPermission("PRODUCT_CREATE") || session.hasPermission("PRODUCT_EDIT"));
        setNavVisible(navInventory, session.hasPermission("INVENTORY_VIEW"));
        setNavVisible(navPurchases, session.hasPermission("PURCHASE_VIEW"));
        setNavVisible(navSuppliers, session.hasPermission("SUPPLIER_MANAGE"));
        setNavVisible(navReports,   session.hasPermission("REPORT_VIEW"));
        setNavVisible(navUsers,     session.hasPermission("USER_MANAGE"));
        setNavVisible(navSettings,  session.hasPermission("SETTINGS_MANAGE"));
        // Returns requires RETURN_PROCESS permission
        setNavVisible(navReturns,   session.hasPermission("RETURN_PROCESS"));
    }

    private void setNavVisible(Button button, boolean visible) {
        button.setVisible(visible);
        button.setManaged(visible);
    }

    /** Returns a simple placeholder node for unbuilt modules. */
    private javafx.scene.Node buildPlaceholder(String fxmlPath) {
        Label label = new Label("Module coming soon\n(" + fxmlPath + ")");
        label.setStyle("-fx-font-size: 18px; -fx-text-fill: #95a5a6; -fx-text-alignment: center;");
        label.setAlignment(javafx.geometry.Pos.CENTER);
        return label;
    }

    /** Replaces the current scene with the login screen. */
    private void navigateToLogin() {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Parent loginRoot = GroceryPOSApp.loadFxml("/fxml/auth/Login.fxml");
            javafx.scene.Scene scene = new javafx.scene.Scene(loginRoot,
                    stage.getScene().getWidth(), stage.getScene().getHeight());
            URL css = getClass().getResource("/css/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle(AppConfig.getInstance().getAppName() + " — Login");
        } catch (IOException e) {
            log.error("Failed to navigate to login: {}", e.getMessage(), e);
        }
    }
}
