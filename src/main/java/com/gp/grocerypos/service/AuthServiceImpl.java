package com.gp.grocerypos.service;

import com.gp.grocerypos.dao.UserDAO;
import com.gp.grocerypos.dao.UserDAOImpl;
import com.gp.grocerypos.exception.AuthException;
import com.gp.grocerypos.exception.ValidationException;
import com.gp.grocerypos.model.Permission;
import com.gp.grocerypos.model.User;
import com.gp.grocerypos.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Default implementation of {@link AuthService}.
 * <p>
 * Login flow:
 * <ol>
 *   <li>Look up the user by username</li>
 *   <li>Verify the BCrypt password hash</li>
 *   <li>Check the account is ACTIVE</li>
 *   <li>Load permissions for the user's role</li>
 *   <li>Populate {@link SessionContext}</li>
 *   <li>Update last_login timestamp (non-fatal if this fails)</li>
 * </ol>
 * <p>
 * The same generic error message is returned for both "user not found" and
 * "wrong password" to avoid username enumeration attacks.
 */
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** Minimum character length enforced when setting a new password. */
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserDAO userDAO;

    public AuthServiceImpl() {
        this.userDAO = new UserDAOImpl();
    }

    /** Constructor for testing — allows injecting a mock DAO. */
    public AuthServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Override
    public User login(String username, String password) {
        log.info("Login attempt for username: '{}'", username);

        // 1. Basic input validation
        if (username == null || username.isBlank()) {
            throw new AuthException(
                "Login failed: username is blank",
                "Please enter your username.",
                AuthException.Reason.INVALID_CREDENTIALS
            );
        }
        if (password == null || password.isEmpty()) {
            throw new AuthException(
                "Login failed: password is empty",
                "Please enter your password.",
                AuthException.Reason.INVALID_CREDENTIALS
            );
        }

        // 2. Look up user by username
        User user = userDAO.findByUsername(username.trim()).orElse(null);

        // 3. Verify password — use same message for "not found" and "wrong password"
        //    to prevent username enumeration
        if (user == null || !HashUtil.verify(password, user.getPasswordHash())) {
            log.warn("Login failed (invalid credentials) for username: '{}'", username);
            throw new AuthException(
                "Login failed: invalid credentials for username='" + username + "'",
                AuthException.Reason.INVALID_CREDENTIALS
            );
        }

        // 4. Check account is active
        if (!user.isActive()) {
            log.warn("Login blocked — account inactive for username: '{}'", username);
            throw new AuthException(
                "Login failed: account inactive for username='" + username + "'",
                AuthException.Reason.ACCOUNT_INACTIVE
            );
        }

        // 5. Load permissions for the user's role
        List<Permission> permissions = userDAO.findPermissionsByRoleId(user.getRole().getId());
        log.debug("Loaded {} permissions for role '{}'", permissions.size(), user.getRoleName());

        // 6. Establish session
        SessionContext.getInstance().login(user, permissions);

        // 7. Record last login timestamp (non-fatal)
        try {
            userDAO.updateLastLogin(user.getId());
        } catch (Exception e) {
            log.warn("Could not update last_login for user id={}: {}", user.getId(), e.getMessage());
        }

        log.info("Login SUCCESS for user: '{}' (role: {})", user.getUsername(), user.getRoleName());
        return user;
    }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    @Override
    public void logout() {
        SessionContext session = SessionContext.getInstance();
        if (session.isLoggedIn()) {
            log.info("User '{}' logged out.", session.getCurrentUser().getUsername());
        }
        session.logout();
    }

    // -------------------------------------------------------------------------
    // changePassword
    // -------------------------------------------------------------------------

    @Override
    public void changePassword(String currentPassword, String newPassword) {
        SessionContext session = SessionContext.getInstance();
        if (!session.isLoggedIn()) {
            throw new AuthException(
                "changePassword called with no active session",
                AuthException.Reason.SESSION_EXPIRED
            );
        }

        User user = session.getCurrentUser();

        // Verify current password
        if (!HashUtil.verify(currentPassword, user.getPasswordHash())) {
            throw new AuthException(
                "changePassword: current password incorrect for user id=" + user.getId(),
                "Current password is incorrect.",
                AuthException.Reason.INVALID_CREDENTIALS
            );
        }

        validateNewPassword(newPassword);

        String newHash = HashUtil.hash(newPassword);
        userDAO.updatePassword(user.getId(), newHash);

        // Update the in-session user object so the hash is current
        user.setPasswordHash(newHash);

        log.info("Password changed successfully for user '{}'.", user.getUsername());
    }

    // -------------------------------------------------------------------------
    // resetPassword
    // -------------------------------------------------------------------------

    @Override
    public void resetPassword(int userId, String newPassword) {
        SessionContext.getInstance().requirePermission("USER_MANAGE");

        validateNewPassword(newPassword);

        String newHash = HashUtil.hash(newPassword);
        userDAO.updatePassword(userId, newHash);

        log.info("Password reset for user id={} by admin '{}'.",
                userId, SessionContext.getInstance().getCurrentUser().getUsername());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validateNewPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException(
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long."
            );
        }
    }
}
