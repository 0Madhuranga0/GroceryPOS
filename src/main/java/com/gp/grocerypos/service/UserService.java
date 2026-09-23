package com.gp.grocerypos.service;

import com.gp.grocerypos.model.Role;
import com.gp.grocerypos.model.User;

import java.util.List;

/**
 * Business logic contract for user account management (admin operations).
 * All mutations require USER_MANAGE permission.
 */
public interface UserService {

    List<User> getAllUsers();
    List<User> getActiveUsers();

    User getUserById(int id);

    /**
     * Creates a new user account.
     * The plainPassword is hashed internally — never stored as plain text.
     *
     * @throws com.gp.grocerypos.exception.ValidationException on invalid data
     * @throws com.gp.grocerypos.exception.AuthException       on permission failure
     */
    User createUser(User user, String plainPassword);

    /**
     * Updates user profile (not password — use AuthService.resetPassword for that).
     */
    User updateUser(User user);

    void activateUser(int id);
    void deactivateUser(int id);

    /** Returns all available roles for the role dropdown. */
    List<Role> getAllRoles();
}
