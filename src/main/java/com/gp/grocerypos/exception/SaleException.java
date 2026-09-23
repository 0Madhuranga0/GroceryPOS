package com.gp.grocerypos.exception;

/**
 * Thrown when a sale transaction cannot be completed.
 * <p>
 * Examples:
 * <ul>
 *   <li>Cart is empty</li>
 *   <li>Payment amount is insufficient</li>
 *   <li>The sale transaction was rolled back due to a database error</li>
 *   <li>An attempt was made to void an already-voided sale</li>
 * </ul>
 */
public class SaleException extends POSException {

    public SaleException(String message) {
        super(message, "The sale could not be completed. " + message);
    }

    public SaleException(String message, String userMessage) {
        super(message, userMessage);
    }

    public SaleException(String message, Throwable cause) {
        super(message, "The sale could not be completed. Please try again.", cause);
    }

    public SaleException(String message, String userMessage, Throwable cause) {
        super(message, userMessage, cause);
    }
}
