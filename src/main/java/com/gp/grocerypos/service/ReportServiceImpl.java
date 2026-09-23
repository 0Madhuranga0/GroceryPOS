package com.gp.grocerypos.service;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.database.DatabaseConnection;
import com.gp.grocerypos.exception.POSException;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    /** Base classpath path for JRXML templates. */
    private static final String REPORTS_PATH = "/reports/";

    /** In-memory cache: reportName → compiled JasperReport */
    private final Map<String, JasperReport> reportCache = new ConcurrentHashMap<>();

    // ── generateReport ────────────────────────────────────────────────────────

    @Override
    public JasperPrint generateReport(String reportName, Map<String, Object> parameters) {
        log.info("Generating report: {} with params: {}", reportName, parameters);

        // Inject store info into every report automatically
        Map<String, Object> params = buildBaseParams(parameters);

        Connection conn = null;
        try {
            JasperReport compiled = getCompiledReport(reportName);
            conn = DatabaseConnection.getInstance().getConnection();
            JasperPrint print = JasperFillManager.fillReport(compiled, params, conn);
            log.info("Report '{}' generated successfully: {} page(s).",
                    reportName, print.getPages().size());
            return print;
        } catch (JRException e) {
            log.error("JasperReports error generating '{}': {}", reportName, e.getMessage(), e);
            throw new POSException(
                "Report generation failed: " + e.getMessage(),
                "Could not generate the report. Please check the report template.",
                e);
        } catch (Exception e) {
            log.error("Unexpected error generating report '{}': {}", reportName, e.getMessage(), e);
            throw new POSException(
                "Report generation failed: " + e.getMessage(),
                "An unexpected error occurred while generating the report.", e);
        } finally {
            DatabaseConnection.getInstance().releaseConnection(conn);
        }
    }

    // ── exportToPdf ───────────────────────────────────────────────────────────

    @Override
    public void exportToPdf(JasperPrint print, File outputFile) {
        try {
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(
                new SimpleOutputStreamExporterOutput(outputFile));
            SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
            config.setMetadataCreator("GroceryPOS v"
                + AppConfig.getInstance().getAppVersion());
            exporter.setConfiguration(config);
            exporter.exportReport();
            log.info("Report exported to PDF: {}", outputFile.getAbsolutePath());
        } catch (JRException e) {
            log.error("PDF export failed: {}", e.getMessage(), e);
            throw new POSException("PDF export failed: " + e.getMessage(),
                "Could not export the report to PDF.", e);
        }
    }

    // ── printReport ───────────────────────────────────────────────────────────

    @Override
    public void printReport(JasperPrint print, boolean showPrintDialog) {
        try {
            JasperPrintManager.printReport(print, showPrintDialog);
            log.info("Report sent to printer (dialog={})", showPrintDialog);
        } catch (JRException e) {
            log.error("Print failed: {}", e.getMessage(), e);
            throw new POSException("Print failed: " + e.getMessage(),
                "Could not print the report. Check that a printer is available.", e);
        }
    }

    // ── generateAndExportPdf ──────────────────────────────────────────────────

    @Override
    public File generateAndExportPdf(String reportName,
                                     Map<String, Object> parameters,
                                     File outputFile) {
        JasperPrint print = generateReport(reportName, parameters);
        exportToPdf(print, outputFile);
        return outputFile;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns a compiled JasperReport from cache or compiles it fresh.
     * JRXML files are loaded from {@code /reports/<name>.jrxml} on the classpath.
     */
    private JasperReport getCompiledReport(String reportName) throws JRException {
        // Return cached version if available
        JasperReport cached = reportCache.get(reportName);
        if (cached != null) return cached;

        String resourcePath = REPORTS_PATH + reportName + ".jrxml";
        InputStream in = getClass().getResourceAsStream(resourcePath);

        if (in == null) {
            throw new POSException(
                "JRXML not found: " + resourcePath,
                "Report template '" + reportName + "' not found.");
        }

        log.debug("Compiling JRXML: {}", resourcePath);
        JasperReport compiled = JasperCompileManager.compileReport(in);
        reportCache.put(reportName, compiled);
        log.debug("JRXML compiled and cached: {}", reportName);
        return compiled;
    }

    /**
     * Builds the base parameter map that every report receives.
     * Merges caller-supplied parameters on top.
     */
    private Map<String, Object> buildBaseParams(Map<String, Object> callerParams) {
        AppConfig config = AppConfig.getInstance();
        Map<String, Object> params = new HashMap<>();

        // Store info — available as $P{STORE_NAME} etc. in every report
        params.put("STORE_NAME",    config.get("store.name",    config.getAppName()));
        params.put("STORE_ADDRESS", config.get("store.address", ""));
        params.put("STORE_PHONE",   config.get("store.phone",   ""));
        params.put("CURRENCY",      config.getCurrencySymbol());
        params.put("APP_VERSION",   config.getAppVersion());

        // Caller params override base params
        if (callerParams != null) {
            params.putAll(callerParams);
        }
        return params;
    }
}
