package com.gp.grocerypos.controller.reports;

import com.gp.grocerypos.service.ReportService;
import com.gp.grocerypos.service.ReportServiceImpl;
import com.gp.grocerypos.util.AlertUtil;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the embedded report viewer dialog.
 * <p>
 * Uses a JavaFX {@link SwingNode} to embed the JasperReports {@link JRViewer}
 * component inside a JavaFX window. The viewer provides built-in zoom,
 * page navigation, and print functionality.
 */
public class ReportViewerController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ReportViewerController.class);

    @FXML private Label     lblTitle;
    @FXML private StackPane viewerPane;
    @FXML private Button    btnClose;
    @FXML private Button    btnPrint;
    @FXML private Label     lblPageInfo;

    private final ReportService reportService = new ReportServiceImpl();
    private JasperPrint         print;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // nothing — report is set via setReport()
    }

    /**
     * Called by {@link ReportsController} after creating this dialog.
     * Embeds the JRViewer in the SwingNode on the Swing EDT.
     *
     * @param print the filled JasperPrint to display
     * @param title the report title shown in the dialog header
     */
    public void setReport(JasperPrint print, String title) {
        this.print = print;
        lblTitle.setText(title);
        lblPageInfo.setText("Pages: " + print.getPages().size());

        SwingNode swingNode = new SwingNode();
        viewerPane.getChildren().setAll(swingNode);

        // JRViewer is a Swing component — must be created on the Swing EDT
        SwingUtilities.invokeLater(() -> {
            try {
                JRViewer viewer = new JRViewer(print);
                // Remove the JRViewer's own toolbar to keep a cleaner look
                // (our JavaFX buttons handle print/export)
                swingNode.setContent(viewer);
                log.debug("JRViewer embedded for report: {}", title);
            } catch (Exception e) {
                log.error("Failed to create JRViewer: {}", e.getMessage(), e);
                Platform.runLater(() ->
                    AlertUtil.showError("Viewer Error",
                        "Could not display the report preview.\n"
                        + "Use Export PDF to save the report."));
            }
        });
    }

    @FXML
    private void handlePrint() {
        if (print == null) return;
        try {
            reportService.printReport(print, true);
        } catch (Exception e) {
            AlertUtil.showException("Print Failed", e);
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) btnClose.getScene().getWindow()).close();
    }
}
