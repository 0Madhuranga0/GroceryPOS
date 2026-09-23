package com.gp.grocerypos.service;

import com.gp.grocerypos.model.Permission;
import com.gp.grocerypos.model.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Singleton that holds the currently authenticated user for the lifetime
 * of a login session.
 * <p>
 * After a successful login, {@link AuthService} calls {@link #login} to
 * populate this context. Every service and controller can then call
 * {@link #getCurrentUser()} or {@link #hasPermission(String)} without
 * needing to pass a user object through every call chain.
 * <p>
 * On logout or application exit, {@link #logout()} clears all state.
 * <p>
 * This class is thread-safe for reads; writes only happen on the JavaFX
 * Application Thread during login/logout.
 */
public final class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private volatile User            currentUser;
    private volatile Set<String>     permissions;   // permission name strings
    private volatile LocalDateTime   loginTime;

    private SessionContext() {
        permissions = Collections.emptySet();
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Session lifecycle
    // -------------------------------------------------------------------------

    /**
     * Establishes a session for the given user with the supplied permissions.
     * Called by {@link AuthService} immediately after a successful login.
     *
     * @param user        the authenticated user (must not be null)
     * @param permList    permissions loaded for the user's role
     */
    public void login(User user, List<Permission> permList) {
        if (user == null) throw new IllegalArgumentException("User must not be null");

        Set<String> names = new HashSet<>();
        if (permList != null) {
            for (Permission p : permList) {
                names.add(p.getName());
            }
        }

        this.currentUser = user;
        this.permissions = Collections.unmodifiableSet(names);
        this.loginTime   = LocalDateTime.now();
    }

    /**
     * Clears all session state. Must be called on logout.
     */
    public void logout() {
        this.currentUser = null;
        this.permissions = Collections.emptySet();
        this.loginTime   = null;
    }

    // -------------------------------------------------------------------------
    // Session queries
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if a user is currently logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Returns the currently logged-in user, or {@code null} if no session.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns the time the current session was established.
     */
    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    // -------------------------------------------------------------------------
    // Permission checks
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the current user holds the named permission.
     * <p>
     * Usage in service/controller:
     * <pre>{@code
     *   if (!SessionContext.getInstance().hasPermission("PRODUCT_EDIT")) {
     *       throw new AuthException("Permission denied", AuthException.Reason.PERMISSION_DENIED);
     *   }
     * }</pre>
     *
     * @param permissionName the permission key (e.g. "SALE_CREATE")
     */
    public boolean hasPermission(String permissionName) {
        return permissions.contains(permissionName);
    }

    /**
     * Throws {@link com.gp.grocerypos.exception.AuthException} if the current
     * user does not hold the given permission. Convenience guard method.
     *
     * @param permissionName the required permission key
     * @throws com.gp.grocerypos.exception.AuthException if permission is absent
     */
    public void requirePermission(String permissionName) {
        if (!hasPermission(permissionName)) {
            throw new com.gp.grocerypos.exception.AuthException(
                "Permission denied: " + permissionName,
                com.gp.grocerypos.exception.AuthException.Reason.PERMISSION_DENIED
            );
        }
    }

    /**
     * Returns the role name of the current user, or empty string if not logged in.
     */
    public String getCurrentRoleName() {
        return currentUser != null ? currentUser.getRoleName() : "";
    }

    /**
     * Returns {@code true} if the current user has the ADMIN role.
     */
    public boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(getCurrentRoleName());
    }

    /**
     * Returns an unmodifiable view of all permission names for the current session.
     */
    public Set<String> getPermissions() {
        return permissions;
    }
}
