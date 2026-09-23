package com.gp.grocerypos.controller.settings;

import com.gp.grocerypos.dao.SettingDAO;
import com.gp.grocerypos.dao.SettingDAOImpl;
import com.gp.grocerypos.exception.BackupException;
import com.gp.grocerypos.model.Setting;
import com.gp.grocerypos.service.BackupService;
import com.gp.grocerypos.service.BackupServiceImpl;
import com.gp.grocerypos.util.AlertUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Settings screen.
 * Loads all settings from the database on entry and writes them back on Save.
 * Organised into 5 tabs: Store, Tax, Inventory, Hardware, Backup.
 */
public class SettingsController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    // ── Store tab ─────────────────────────────────────────────────────────────
    @FXML private TextField storeName;
    @FXML private TextField storeAddress;
    @FXML private TextField storePhone;
    @FXML private TextField storeEmail;
    @FXML private TextField receiptFooter;
    @FXML private TextField currencySymbol;
    @FXML private TextField currencyCode;

    // ── Tax tab ───────────────────────────────────────────────────────────────
    @FXML private CheckBox  taxEnabled;
    @FXML private TextField taxRate;

    // ── Inventory tab ─────────────────────────────────────────────────────────
    @FXML private CheckBox  lowStockWarning;
    @FXML private CheckBox  allowNegativeStock;
    @FXML private TextField expiryWarningDays;

    // ── Invoice numbering ─────────────────────────────────────────────────────
    @FXML private TextField invoicePrefix;
    @FXML private TextField invoiceNumberLength;
    @FXML private TextField purchasePrefix;
    @FXML private TextField returnPrefix;

    // ── Hardware tab ──────────────────────────────────────────────────────────
    @FXML private ComboBox<String> printerType;
    @FXML private TextField        printerName;
    @FXML private ComboBox<String> displayType;
    @FXML private TextField        displayPort;
    @FXML private TextField        displayBaudRate;
    @FXML private TextField        displayHost;
    @FXML private TextField        displayTcpPort;

    // ── Backup tab ────────────────────────────────────────────────────────────
    @FXML private TextField           backupDir;
    @FXML private TextField           mysqldumpPath;
    @FXML private ListView<String>    backupList;
    @FXML private Label               backupStatusLabel;
    @FXML private ProgressIndicator   backupSpinner;

    // ── Common ────────────────────────────────────────────────────────────────
    @FXML private Label statusLabel;

    private final SettingDAO    settingDAO    = new SettingDAOImpl();
    private final BackupService backupService = new BackupServiceImpl();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHardwareCombos();
        loadSettings();
        loadBackupList();
        if (backupSpinner != null) backupSpinner.setVisible(false);
        log.debug("SettingsController initialised.");
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private void loadSettings() {
        try {
            // Store
            setText(storeName,          "store_name");
            setText(storeAddress,       "store_address");
            setText(storePhone,         "store_phone");
            setText(storeEmail,         "store_email");
            setText(receiptFooter,      "store_receipt_footer");
            setText(currencySymbol,     "currency_symbol");
            setText(currencyCode,       "currency_code");

            // Tax
            setCheck(taxEnabled,        "tax_enabled");
            setText(taxRate,            "tax_rate");

            // Inventory
            setCheck(lowStockWarning,   "low_stock_warning");
            setCheck(allowNegativeStock,"allow_negative_stock");
            setText(expiryWarningDays,  "expiry_warning_days");

            // Invoicing
            setText(invoicePrefix,       "invoice_prefix");
            setText(invoiceNumberLength, "invoice_number_length");
            setText(purchasePrefix,      "purchase_prefix");
            setText(returnPrefix,        "return_prefix");

            // Hardware
            setCombo(printerType,       "printer_type");
            setText(printerName,        "printer_name");
            setCombo(displayType,       "display_type");
            setText(displayPort,        "display_port");
            setText(displayBaudRate,    "display_baud_rate");
            setText(displayHost,        "display_host");
            setText(displayTcpPort,     "display_tcp_port");

            // Backup
            setText(backupDir,          "backup_dir");
            setText(mysqldumpPath,      "mysqldump_path");

            setStatus("Settings loaded.");
        } catch (Exception e) {
            AlertUtil.showException("Load Error", e);
        }
    }

    private void setupHardwareCombos() {
        if (printerType != null) {
            printerType.getItems().addAll("NONE", "ESCPOS");
            printerType.setValue("NONE");
        }
        if (displayType != null) {
            displayType.getItems().addAll("NONE", "SERIAL", "NETWORK", "SECONDARY_MONITOR");
            displayType.setValue("NONE");
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        try {
            // Store
            save("store_name",             storeName);
            save("store_address",          storeAddress);
            save("store_phone",            storePhone);
            save("store_email",            storeEmail);
            save("store_receipt_footer",   receiptFooter);
            save("currency_symbol",        currencySymbol);
            save("currency_code",          currencyCode);

            // Tax
            saveBoolean("tax_enabled",     taxEnabled);
            save("tax_rate",               taxRate);

            // Inventory
            saveBoolean("low_stock_warning",    lowStockWarning);
            saveBoolean("allow_negative_stock", allowNegativeStock);
            save("expiry_warning_days",          expiryWarningDays);

            // Invoicing
            save("invoice_prefix",         invoicePrefix);
            save("invoice_number_length",  invoiceNumberLength);
            save("purchase_prefix",        purchasePrefix);
            save("return_prefix",          returnPrefix);

            // Hardware
            saveCombo("printer_type",      printerType);
            save("printer_name",           printerName);
            saveCombo("display_type",      displayType);
            save("display_port",           displayPort);
            save("display_baud_rate",      displayBaudRate);
            save("display_host",           displayHost);
            save("display_tcp_port",       displayTcpPort);

            // Backup
            save("backup_dir",             backupDir);
            save("mysqldump_path",         mysqldumpPath);

            setStatus("Settings saved successfully.");
            AlertUtil.showInfo("Settings Saved", "All settings have been saved.");
        } catch (Exception e) {
            AlertUtil.showException("Save Error", e);
        }
    }

    @FXML
    private void handleReset() {
        if (AlertUtil.showConfirm("Reset Settings",
                "Discard unsaved changes and reload settings from the database?")) {
            loadSettings();
        }
    }

    // ── Backup tab handlers ───────────────────────────────────────────────────

    @FXML
    private void handleCreateBackup() {
        setBackupBusy(true, "Creating backup...");

        Task<File> task = new Task<>() {
            @Override protected File call() {
                return backupService.createBackup();
            }
        };
        task.setOnSucceeded(e -> {
            File f = task.getValue();
            setBackupBusy(false, "Backup created: " + f.getName());
            loadBackupList();
            AlertUtil.showInfo("Backup Complete",
                "Backup saved to:\n" + f.getAbsolutePath());
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            setBackupBusy(false, "Backup failed.");
            AlertUtil.showException("Backup Failed", ex);
        });
        daemonThread(task);
    }

    @FXML
    private void handleRestoreBackup() {
        // Show file picker — let admin choose any .sql file
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Backup File to Restore");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("SQL Backup Files", "*.sql"));

        File dir = new File(backupService.getBackupDirectory());
        if (dir.exists()) chooser.setInitialDirectory(dir);

        File backupFile = chooser.showOpenDialog(
            backupDir.getScene().getWindow());
        if (backupFile == null) return;

        if (!AlertUtil.showConfirm("⚠ CONFIRM RESTORE",
                "WARNING: This will OVERWRITE all current database data!\n\n"
                + "Restore from:\n" + backupFile.getName() + "\n\n"
                + "This cannot be undone. Are you absolutely sure?")) return;

        setBackupBusy(true, "Restoring database...");

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                backupService.restoreBackup(backupFile);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            setBackupBusy(false, "Database restored from: " + backupFile.getName());
            AlertUtil.showInfo("Restore Complete",
                "Database restored successfully from:\n" + backupFile.getName()
                + "\n\nPlease restart the application.");
        });
        task.setOnFailed(e -> {
            setBackupBusy(false, "Restore failed.");
            AlertUtil.showException("Restore Failed", task.getException());
        });
        daemonThread(task);
    }

    @FXML
    private void handleBrowseBackupDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Backup Directory");
        File current = new File(backupDir.getText().trim());
        if (current.exists()) chooser.setInitialDirectory(current);
        File selected = chooser.showDialog(backupDir.getScene().getWindow());
        if (selected != null) backupDir.setText(selected.getAbsolutePath());
    }

    @FXML
    private void handleRefreshBackupList() { loadBackupList(); }

    private void loadBackupList() {
        try {
            List<File> files = backupService.listBackups();
            if (backupList != null) {
                backupList.getItems().setAll(
                    files.stream()
                        .map(f -> f.getName() + "  (" + formatSize(f.length()) + ")")
                        .toList()
                );
            }
            if (backupStatusLabel != null) {
                backupStatusLabel.setText(files.isEmpty()
                    ? "No backups found."
                    : files.size() + " backup file(s) found.");
            }
        } catch (Exception e) {
            log.warn("Could not list backups: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setText(TextField field, String key) {
        if (field != null) field.setText(settingDAO.getValue(key, ""));
    }

    private void setCheck(CheckBox box, String key) {
        if (box != null) box.setSelected(
            Boolean.parseBoolean(settingDAO.getValue(key, "false")));
    }

    private void setCombo(ComboBox<String> combo, String key) {
        if (combo != null) {
            String val = settingDAO.getValue(key, "NONE");
            if (combo.getItems().contains(val)) combo.setValue(val);
            else if (!combo.getItems().isEmpty()) combo.setValue(combo.getItems().get(0));
        }
    }

    private void save(String key, TextField field) {
        if (field != null) settingDAO.setValue(key, field.getText().trim());
    }

    private void saveBoolean(String key, CheckBox box) {
        if (box != null) settingDAO.setValue(key, String.valueOf(box.isSelected()));
    }

    private void saveCombo(String key, ComboBox<String> combo) {
        if (combo != null && combo.getValue() != null)
            settingDAO.setValue(key, combo.getValue());
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private void setBackupBusy(boolean busy, String msg) {
        if (backupSpinner != null) backupSpinner.setVisible(busy);
        if (backupStatusLabel != null) backupStatusLabel.setText(msg);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void daemonThread(Task<?> task) {
        Thread t = new Thread(task, "backup-thread");
        t.setDaemon(true);
        t.start();
    }
}
