package com.gp.grocerypos.dao;

import com.gp.grocerypos.model.Permission;
import com.gp.grocerypos.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for {@link User} entities.
 * All methods throw {@link com.gp.grocerypos.exception.DatabaseException}
 * (unchecked) on SQL failure — callers do not need to handle SQLException.
 */
public interface UserDAO {

    /**
     * Finds a user by username. Returns an empty Optional if not found.
     * The returned User includes the associated Role object.
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by primary key. Returns an empty Optional if not found.
     */
    Optional<User> findById(int id);

    /**
     * Returns all users ordered by full_name, with their Role populated.
     */
    List<User> findAll();

    /**
     * Returns all active users ordered by full_name.
     */
    List<User> findAllActive();

    /**
     * Inserts a new user and returns the generated ID.
     * The passwordHash must already be a BCrypt hash — never plain text.
     */
    int insert(User user);

    /**
     * Updates an existing user's profile fields (not password).
     */
    void update(User user);

    /**
     * Updates only the password hash for the given user ID.
     */
    void updatePassword(int userId, String newPasswordHash);

    /**
     * Records the last login timestamp for the given user ID.
     */
    void updateLastLogin(int userId);

    /**
     * Soft-deletes a user by setting status to INACTIVE.
     */
    void deactivate(int userId);

    /**
     * Re-activates a previously deactivated user.
     */
    void activate(int userId);

    /**
     * Loads all permissions for a given role ID.
     * Used by AuthService to populate the session after login.
     */
    List<Permission> findPermissionsByRoleId(int roleId);

    /**
     * Returns true if a username is already taken (case-insensitive).
     */
    boolean usernameExists(String username);
}
