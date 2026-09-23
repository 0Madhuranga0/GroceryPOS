package com.gp.grocerypos.exception;

/**
 * Base exception for all GroceryPOS application-level errors.
 * <p>
 * All other custom exceptions extend this class.
 * Service and controller layers should catch {@code POSException} subtypes
 * and translate them into user-friendly messages.
 * <p>
 * Never expose raw SQL messages or stack traces to the end user.
 */
public class POSException extends RuntimeException {

    private final String userMessage;

    /**
     * Creates a POSException with the same technical and user-facing message.
     *
     * @param message the error message (shown in logs and to the user)
     */
    public POSException(String message) {
        super(message);
        this.userMessage = message;
    }

    /**
     * Creates a POSException with separate technical and user-facing messages.
     *
     * @param message     technical message (logged)
     * @param userMessage friendly message shown in the UI
     */
    public POSException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage;
    }

    /**
     * Creates a POSException wrapping another throwable.
     *
     * @param message technical message (logged)
     * @param cause   the underlying cause
     */
    public POSException(String message, Throwable cause) {
        super(message, cause);
        this.userMessage = message;
    }

    /**
     * Creates a POSException with separate messages wrapping another throwable.
     *
     * @param message     technical message (logged)
     * @param userMessage friendly message shown in the UI
     * @param cause       the underlying cause
     */
    public POSException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage;
    }

    /**
     * Returns a user-friendly message safe to display in the UI.
     * This message must never contain SQL details, passwords, or stack traces.
     *
     * @return the user-facing error message
     */
    public String getUserMessage() {
        return userMessage;
    }
}
