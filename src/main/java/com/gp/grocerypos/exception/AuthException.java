package com.gp.grocerypos.exception;

/**
 * Thrown when an authentication or authorisation check fails.
 * <p>
 * Examples:
 * <ul>
 *   <li>Invalid username or password at login</li>
 *   <li>Account is inactive / disabled</li>
 *   <li>User does not have the required permission for an operation</li>
 *   <li>Session has expired</li>
 * </ul>
 */
public class AuthException extends POSException {

    /** Indicates the reason category for the auth failure. */
    public enum Reason {
        INVALID_CREDENTIALS,
        ACCOUNT_INACTIVE,
        PERMISSION_DENIED,
        SESSION_EXPIRED
    }

    private final Reason reason;

    public AuthException(String message, Reason reason) {
        super(message, resolveUserMessage(reason));
        this.reason = reason;
    }

    public AuthException(String message, String userMessage, Reason reason) {
        super(message, userMessage);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    private static String resolveUserMessage(Reason reason) {
        return switch (reason) {
            case INVALID_CREDENTIALS -> "Invalid username or password.";
            case ACCOUNT_INACTIVE    -> "This account has been disabled. Please contact an administrator.";
            case PERMISSION_DENIED   -> "You do not have permission to perform this action.";
            case SESSION_EXPIRED     -> "Your session has expired. Please log in again.";
        };
    }
}
