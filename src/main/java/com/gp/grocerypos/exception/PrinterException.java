package com.gp.grocerypos.exception;

/**
 * Thrown when a hardware printing operation fails.
 * <p>
 * Printer failures must never abort a completed sale.
 * The service layer should catch {@code PrinterException}, log it,
 * and offer the cashier a re-print option rather than rolling back
 * the transaction.
 * <p>
 * Examples:
 * <ul>
 *   <li>Printer is offline or not available</li>
 *   <li>Paper jam / out of paper</li>
 *   <li>Communication error with the printer port</li>
 * </ul>
 */
public class PrinterException extends POSException {

    public PrinterException(String message) {
        super(message, "Printer is not available. The sale has been saved. Please re-print the receipt.");
    }

    public PrinterException(String message, Throwable cause) {
        super(message, "Printer is not available. The sale has been saved. Please re-print the receipt.", cause);
    }

    public PrinterException(String message, String userMessage) {
        super(message, userMessage);
    }

    public PrinterException(String message, String userMessage, Throwable cause) {
        super(message, userMessage, cause);
    }
}
