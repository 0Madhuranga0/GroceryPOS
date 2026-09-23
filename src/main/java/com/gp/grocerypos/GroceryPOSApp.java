package com.gp.grocerypos;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.database.DatabaseConnection;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * JavaFX Application entry point for GroceryPOS.
 * <p>
 * Startup sequence:
 * <ol>
 *   <li>Load {@code AppConfig} (reads app.properties)</li>
 *   <li>Configure the log directory system property for Logback</li>
 *   <li>Test the database connection — abort with a clear message if it fails</li>
 *   <li>Load the Login screen</li>
 *   <li>Show the primary stage</li>
 * </ol>
 * On shutdown, the JDBC connection pool is closed cleanly.
 */
public class GroceryPOSApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(GroceryPOSApp.class);

    // Minimum window dimensions
    private static final double MIN_WIDTH  = 1024;
    private static final double MIN_HEIGHT = 700;

    // -------------------------------------------------------------------------
    // main() — JavaFX launcher bootstrap
    // -------------------------------------------------------------------------

    /**
     * Application entry point.
     * <p>
     * We keep a plain {@code main()} launcher here so that the app can be
     * started from both the IDE and a fat JAR without needing a module-info.
     */
    public static void main(String[] args) {
        launch(args);
    }

    // -------------------------------------------------------------------------
    // JavaFX lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void init() {
        // This runs on the launcher thread, before the JavaFX thread starts.
        // Use it for non-UI initialisation so the UI is snappy once it appears.
        AppConfig config = AppConfig.getInstance();

        // Tell Logback where to write its log files
        System.setProperty("log.dir", config.getLogDir());

        log.info("==========================================================");
        log.info("  {} v{} — starting up",
                config.getAppName(), config.getAppVersion());
        log.info("==========================================================");
    }

    @Override
    public void start(Stage primaryStage) {
        AppConfig config = AppConfig.getInstance();

        // -- Database connectivity check ------------------------------------
        if (!DatabaseConnection.getInstance().testConnection()) {
            showFatalError(
                "Database Connection Failed",
                "Could not connect to the database.\n\n" +
                "Please check:\n" +
                "  • MySQL server is running\n" +
                "  • Connection settings in config/app.properties\n\n" +
                "Host : " + config.getDatabaseConfig().getHost() + "\n" +
                "Port : " + config.getDatabaseConfig().getPort() + "\n" +
                "DB   : " + config.getDatabaseConfig().getDbName()
            );
            Platform.exit();
            return;
        }

        log.info("Database connection verified.");

        // -- Load login screen ---------------------------------------------
        try {
            Parent root = loadFxml("/fxml/auth/Login.fxml");

            Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);

            // Apply global stylesheet
            URL css = getClass().getResource("/css/styles.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }

            primaryStage.setTitle(config.getAppName() + " — Login");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            primaryStage.setResizable(true);

            // Clean shutdown on window close
            primaryStage.setOnCloseRequest(event -> handleShutdown());

            primaryStage.show();
            log.info("Login screen displayed.");

        } catch (IOException e) {
            log.error("Failed to load Login.fxml: {}", e.getMessage(), e);
            showFatalError("Startup Error",
                "The application could not start because a required UI file is missing.\n\n" +
                "Details: " + e.getMessage());
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        handleShutdown();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Loads an FXML file from the classpath resources.
     *
     * @param classpathPath path relative to resources root, e.g. "/fxml/auth/Login.fxml"
     * @return the loaded root {@link Parent}
     * @throws IOException if the file cannot be loaded
     */
    public static Parent loadFxml(String classpathPath) throws IOException {
        URL resource = GroceryPOSApp.class.getResource(classpathPath);
        if (resource == null) {
            throw new IOException("FXML resource not found on classpath: " + classpathPath);
        }
        return FXMLLoader.load(resource);
    }

    /**
     * Loads an FXML file and returns both the root node and the controller.
     * Useful when the caller needs to pass data to the controller before display.
     *
     * @param classpathPath path relative to resources root
     * @return a configured {@link FXMLLoader} (call {@code getController()} on it)
     * @throws IOException if the file cannot be loaded
     */
    public static FXMLLoader createLoader(String classpathPath) throws IOException {
        URL resource = GroceryPOSApp.class.getResource(classpathPath);
        if (resource == null) {
            throw new IOException("FXML resource not found on classpath: " + classpathPath);
        }
        FXMLLoader loader = new FXMLLoader(resource);
        loader.load();
        return loader;
    }

    private void handleShutdown() {
        log.info("Application shutdown initiated.");
        try {
            DatabaseConnection.getInstance().shutdown();
            log.info("Connection pool closed.");
        } catch (Exception e) {
            log.warn("Error during connection pool shutdown: {}", e.getMessage());
        }
        log.info("GroceryPOS shut down cleanly.");
    }

    private void showFatalError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
