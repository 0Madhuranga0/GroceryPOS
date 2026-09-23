package com.gp.grocerypos.exception;

/**
 * Thrown when a database backup or restore operation fails.
 * <p>
 * Examples:
 * <ul>
 *   <li>The backup directory is not writable</li>
 *   <li>The {@code mysqldump} executable was not found</li>
 *   <li>A restore operation failed mid-way</li>
 * </ul>
 */
public class BackupException extends POSException {

    public BackupException(String message) {
        super(message, "Backup operation failed. " + message);
    }

    public BackupException(String message, Throwable cause) {
        super(message, "Backup operation failed. Please check the backup directory and try again.", cause);
    }

    public BackupException(String message, String userMessage) {
        super(message, userMessage);
    }

    public BackupException(String message, String userMessage, Throwable cause) {
        super(message, userMessage, cause);
    }
}
