package com.gp.grocerypos.util;

import com.gp.grocerypos.exception.POSException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Convenience wrappers for JavaFX Alert dialogs.
 * Keeps controller code clean — one-liners instead of 5-line Alert setups.
 */
public final class AlertUtil {

    private AlertUtil() {}

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Returns true if the user clicks Yes/OK. */
    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                message, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(title);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    /**
     * Shows the user-friendly message from any POSException.
     * Falls back to a generic message for unexpected exceptions.
     */
    public static void showException(String title, Throwable e) {
        String message = (e instanceof POSException pe)
                ? pe.getUserMessage()
                : "An unexpected error occurred. Please try again.";
        showError(title, message);
    }
}
