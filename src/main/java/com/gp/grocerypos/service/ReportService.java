package com.gp.grocerypos.service;

import net.sf.jasperreports.engine.JasperPrint;

import java.io.File;
import java.time.LocalDate;
import java.util.Map;

/**
 * Business logic contract for generating JasperReports reports.
 * <p>
 * All reports follow the same pipeline:
 * <ol>
 *   <li>Compile .jrxml (or load cached .jasper) from the classpath.</li>
 *   <li>Fill the report against the application database connection.</li>
 *   <li>Return a {@link JasperPrint} for in-app preview, PDF export, or printing.</li>
 * </ol>
 */
public interface ReportService {

    // ── Report identifiers (match JRXML file names without extension) ─────────

    String REPORT_DAILY_SALES       = "daily_sales";
    String REPORT_SALES_BY_PRODUCT  = "sales_by_product";
    String REPORT_SALES_BY_CATEGORY = "sales_by_category";
    String REPORT_SALES_BY_CASHIER  = "sales_by_cashier";
    String REPORT_CURRENT_STOCK     = "current_stock";
    String REPORT_LOW_STOCK         = "low_stock";
    String REPORT_PURCHASE_HISTORY  = "purchase_history";

    /**
     * Generates a report and returns a filled {@link JasperPrint}.
     * The caller can then display it in a viewer, export to PDF, or send to printer.
     *
     * @param reportName  one of the REPORT_* constants defined above
     * @param parameters  report parameters (e.g. P_DATE_FROM, P_DATE_TO, P_TITLE)
     * @return a filled JasperPrint ready for display or export
     * @throws com.gp.grocerypos.exception.POSException on any report error
     */
    JasperPrint generateReport(String reportName, Map<String, Object> parameters);

    /**
     * Exports a filled report to a PDF file.
     *
     * @param print      the filled JasperPrint
     * @param outputFile the destination file (will be created or overwritten)
     */
    void exportToPdf(JasperPrint print, File outputFile);

    /**
     * Sends a filled report to the default system printer.
     *
     * @param print the filled JasperPrint
     * @param showPrintDialog true = show the OS print dialog before printing
     */
    void printReport(JasperPrint print, boolean showPrintDialog);

    /**
     * Convenience method: generate and immediately export to PDF.
     *
     * @return the PDF file that was written
     */
    File generateAndExportPdf(String reportName, Map<String, Object> parameters,
                               File outputFile);
}
