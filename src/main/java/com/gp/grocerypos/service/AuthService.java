package com.gp.grocerypos.service;

import com.gp.grocerypos.model.User;

/**
 * Authentication service contract.
 * <p>
 * Responsible for verifying credentials, establishing the session, and
 * clearing it on logout. All business rules around authentication live here —
 * never in a controller.
 */
public interface AuthService {

    /**
     * Attempts to log in with the given credentials.
     * <p>
     * On success:
     * <ul>
     *   <li>Verifies the BCrypt password hash</li>
     *   <li>Checks the account is ACTIVE</li>
     *   <li>Loads the user's role permissions</li>
     *   <li>Populates {@link SessionContext}</li>
     *   <li>Records the last-login timestamp</li>
     * </ul>
     *
     * @param username plain-text username
     * @param password plain-text password (never stored)
     * @return the authenticated {@link User}
     * @throws com.gp.grocerypos.exception.AuthException if credentials are
     *         invalid, account is inactive, or any other auth failure
     */
    User login(String username, String password);

    /**
     * Logs out the current user by clearing {@link SessionContext}.
     * Safe to call even when no session is active.
     */
    void logout();

    /**
     * Changes the password for the currently logged-in user.
     *
     * @param currentPassword the user's existing password (for verification)
     * @param newPassword      the new password to set
     * @throws com.gp.grocerypos.exception.AuthException   if currentPassword is wrong
     * @throws com.gp.grocerypos.exception.ValidationException if newPassword fails rules
     */
    void changePassword(String currentPassword, String newPassword);

    /**
     * Changes the password for any user — admin operation.
     * Does NOT require the current password.
     *
     * @param userId      the user whose password to reset
     * @param newPassword the new password
     * @throws com.gp.grocerypos.exception.AuthException if the caller lacks USER_MANAGE permission
     */
    void resetPassword(int userId, String newPassword);
}
