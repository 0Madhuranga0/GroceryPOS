package com.gp.grocerypos.exception;

/**
 * Thrown when a database operation fails.
 * <p>
 * Wraps {@link java.sql.SQLException} with a user-friendly message
 * that does not expose SQL details or connection credentials.
 * <p>
 * Examples:
 * <ul>
 *   <li>Connection could not be established</li>
 *   <li>A query failed due to a constraint violation</li>
 *   <li>A transaction could not be committed</li>
 * </ul>
 */
public class DatabaseException extends POSException {

    public DatabaseException(String message) {
        super(message, "A database error occurred. Please try again or contact support.");
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, "A database error occurred. Please try again or contact support.", cause);
    }

    public DatabaseException(String message, String userMessage) {
        super(message, userMessage);
    }

    public DatabaseException(String message, String userMessage, Throwable cause) {
        super(message, userMessage, cause);
    }
}
