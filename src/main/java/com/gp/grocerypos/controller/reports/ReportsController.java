package com.gp.grocerypos.controller.reports;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.service.ReportService;
import com.gp.grocerypos.service.ReportServiceImpl;
import com.gp.grocerypos.util.AlertUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.JasperPrint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the Reports launcher screen.
 * <p>
 * The user selects a report from the left panel, fills in any parameters
 * (date range, etc.), then clicks Run, Export PDF, or Print.
 * Heavy report generation runs on a background thread to keep the UI responsive.
 */
public class ReportsController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ReportsController.class);
    private static final DateTimeFormatter SQL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Report definition ─────────────────────────────────────────────────────
    private record ReportDef(String id, String title, boolean needsDates) {}

    private static final ReportDef[] REPORTS = {
        new ReportDef(ReportService.REPORT_DAILY_SALES,       "Daily / Period Sales",     true),
        new ReportDef(ReportService.REPORT_SALES_BY_PRODUCT,  "Sales by Product",         true),
        new ReportDef(ReportService.REPORT_SALES_BY_CATEGORY, "Sales by Category",        true),
        new ReportDef(ReportService.REPORT_SALES_BY_CASHIER,  "Sales by Cashier",         true),
        new ReportDef(ReportService.REPORT_CURRENT_STOCK,     "Current Stock",            false),
        new ReportDef(ReportService.REPORT_LOW_STOCK,         "Low Stock Alert",          false),
        new ReportDef(ReportService.REPORT_PURCHASE_HISTORY,  "Purchase History",         true),
    };

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private ListView<String>  reportList;
    @FXML private Label             lblDescription;
    @FXML private DatePicker        fromDatePicker;
    @FXML private DatePicker        toDatePicker;
    @FXML private Label             lblFromDate;
    @FXML private Label             lblToDate;
    @FXML private Button            btnRun;
    @FXML private Button            btnExportPdf;
    @FXML private Button            btnPrint;
    @FXML private ProgressIndicator spinner;
    @FXML private Label             statusLabel;

    private final ReportService reportService = new ReportServiceImpl();
    private JasperPrint         lastPrint;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Populate report list
        for (ReportDef def : REPORTS) {
            reportList.getItems().add(def.title());
        }
        reportList.getSelectionModel().selectFirst();

        // Default dates: last 30 days
        toDatePicker.setValue(LocalDate.now());
        fromDatePicker.setValue(LocalDate.now().minusDays(30));

        // Update UI when a different report is selected
        reportList.getSelectionModel().selectedIndexProperty().addListener(
            (obs, old, idx) -> onReportSelected(idx.intValue()));

        spinner.setVisible(false);
        btnExportPdf.setDisable(true);
        btnPrint.setDisable(true);

        onReportSelected(0);
        log.debug("ReportsController initialised.");
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML
    private void handleRun() {
        int idx = reportList.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        ReportDef def = REPORTS[idx];
        generateReport(def, false);
    }

    @FXML
    private void handleExportPdf() {
        if (lastPrint == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report to PDF");
        chooser.setInitialFileName(getSelectedReportId() + "_"
            + LocalDate.now() + ".pdf");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        // Default to backup dir or desktop
        String backupDir = AppConfig.getInstance().getBackupDir();
        File dir = new File(backupDir);
        if (!dir.exists()) dir = new File(System.getProperty("user.home"), "Desktop");
        chooser.setInitialDirectory(dir.exists() ? dir : null);

        File target = chooser.showSaveDialog(btnExportPdf.getScene().getWindow());
        if (target == null) return;

        setBusy(true, "Exporting PDF...");
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                reportService.exportToPdf(lastPrint, target);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false, "PDF exported: " + target.getName());
            if (AlertUtil.showConfirm("Export Complete",
                    "Report exported to:\n" + target.getAbsolutePath()
                    + "\n\nOpen the file now?")) {
                openFile(target);
            }
        });
        task.setOnFailed(e -> {
            setBusy(false, "Export failed.");
            Throwable ex = task.getException();
            AlertUtil.showException("Export Failed", ex);
        });
        daemonThread(task);
    }

    @FXML
    private void handlePrint() {
        if (lastPrint == null) return;
        setBusy(true, "Sending to printer...");
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                reportService.printReport(lastPrint, true);
                return null;
            }
        };
        task.setOnSucceeded(e -> setBusy(false, "Print job sent."));
        task.setOnFailed(e -> {
            setBusy(false, "Print failed.");
            AlertUtil.showException("Print Failed", task.getException());
        });
        daemonThread(task);
    }

    // ── Report generation ─────────────────────────────────────────────────────

    private void generateReport(ReportDef def, boolean exportDirectly) {
        Map<String, Object> params = buildParams(def);
        if (params == null) return; // validation failed

        setBusy(true, "Generating " + def.title() + "...");
        lastPrint = null;
        btnExportPdf.setDisable(true);
        btnPrint.setDisable(true);

        Task<JasperPrint> task = new Task<>() {
            @Override protected JasperPrint call() {
                return reportService.generateReport(def.id(), params);
            }
        };

        task.setOnSucceeded(e -> {
            lastPrint = task.getValue();
            setBusy(false, def.title() + " — " + lastPrint.getPages().size() + " page(s) generated.");
            btnExportPdf.setDisable(false);
            btnPrint.setDisable(false);
            openViewer(lastPrint, def.title());
        });

        task.setOnFailed(e -> {
            setBusy(false, "Failed to generate report.");
            Throwable ex = task.getException();
            String msg = (ex instanceof POSException pe) ? pe.getUserMessage() : ex.getMessage();
            AlertUtil.showError("Report Error", msg);
            log.error("Report generation failed: {}", ex.getMessage(), ex);
        });

        daemonThread(task);
    }

    // ── Viewer ────────────────────────────────────────────────────────────────

    private void openViewer(JasperPrint print, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/reports/ReportViewer.fxml"));
            Parent root = loader.load();
            ReportViewerController ctrl = loader.getController();
            ctrl.setReport(print, title);

            Stage stage = new Stage();
            stage.setTitle("Report Viewer — " + title);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 820, 700));
            stage.setResizable(true);
            stage.show();
        } catch (Exception e) {
            log.error("Failed to open ReportViewer: {}", e.getMessage(), e);
            AlertUtil.showError("Viewer Error",
                "Could not open the report viewer.\nThe report was generated successfully — use Export PDF.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void onReportSelected(int idx) {
        if (idx < 0 || idx >= REPORTS.length) return;
        ReportDef def = REPORTS[idx];

        boolean needsDates = def.needsDates();
        fromDatePicker.setDisable(!needsDates);
        toDatePicker.setDisable(!needsDates);
        lblFromDate.setDisable(!needsDates);
        lblToDate.setDisable(!needsDates);

        lblDescription.setText(getDescription(def.id()));
        lastPrint = null;
        btnExportPdf.setDisable(true);
        btnPrint.setDisable(true);
        statusLabel.setText("Select a report and click Run.");
    }

    private Map<String, Object> buildParams(ReportDef def) {
        Map<String, Object> params = new HashMap<>();
        params.put("P_TITLE", def.title());

        if (def.needsDates()) {
            LocalDate from = fromDatePicker.getValue();
            LocalDate to   = toDatePicker.getValue();
            if (from == null || to == null) {
                AlertUtil.showWarning("Date Required",
                    "Please select both From and To dates for this report.");
                return null;
            }
            if (from.isAfter(to)) {
                AlertUtil.showWarning("Invalid Range",
                    "From date must be before To date.");
                return null;
            }
            params.put("P_DATE_FROM", from.format(SQL_DATE));
            params.put("P_DATE_TO",   to.format(SQL_DATE));
        } else {
            // Non-date reports still need dummy values to prevent JR parameter errors
            params.put("P_DATE_FROM", LocalDate.now().minusYears(10).format(SQL_DATE));
            params.put("P_DATE_TO",   LocalDate.now().format(SQL_DATE));
        }
        return params;
    }

    private String getSelectedReportId() {
        int idx = reportList.getSelectionModel().getSelectedIndex();
        return (idx >= 0) ? REPORTS[idx].id() : "report";
    }

    private void setBusy(boolean busy, String message) {
        spinner.setVisible(busy);
        btnRun.setDisable(busy);
        statusLabel.setText(message);
    }

    private void openFile(File file) {
        try {
            java.awt.Desktop.getDesktop().open(file);
        } catch (Exception e) {
            log.warn("Could not open file: {}", e.getMessage());
        }
    }

    private void daemonThread(Task<?> task) {
        Thread t = new Thread(task, "report-thread");
        t.setDaemon(true);
        t.start();
    }

    private String getDescription(String reportId) {
        return switch (reportId) {
            case ReportService.REPORT_DAILY_SALES ->
                "Lists all completed sales within the selected date range with invoice details, totals and payment method.";
            case ReportService.REPORT_SALES_BY_PRODUCT ->
                "Groups sales by product showing quantity sold, revenue and profit for each product.";
            case ReportService.REPORT_SALES_BY_CATEGORY ->
                "Summarises sales grouped by product category with revenue and profit per category.";
            case ReportService.REPORT_SALES_BY_CASHIER ->
                "Shows each cashier's total sales, transaction count, cash and card breakdown.";
            case ReportService.REPORT_CURRENT_STOCK ->
                "Lists all active products with their current stock level, minimum stock and total inventory value.";
            case ReportService.REPORT_LOW_STOCK ->
                "Highlights products at or below the minimum stock level, sorted by shortage quantity.";
            case ReportService.REPORT_PURCHASE_HISTORY ->
                "Lists all supplier purchases within the date range with totals and status.";
            default -> "";
        };
    }
}
