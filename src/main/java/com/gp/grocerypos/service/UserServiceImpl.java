package com.gp.grocerypos.service;

import com.gp.grocerypos.dao.UserDAO;
import com.gp.grocerypos.dao.UserDAOImpl;
import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.exception.ValidationException;
import com.gp.grocerypos.model.Role;
import com.gp.grocerypos.model.User;
import com.gp.grocerypos.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserDAO userDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDAOImpl();
    }

    @Override
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    @Override
    public List<User> getActiveUsers() {
        return userDAO.findAllActive();
    }

    @Override
    public User getUserById(int id) {
        return userDAO.findById(id)
            .orElseThrow(() -> new POSException("User not found id=" + id, "User not found."));
    }

    @Override
    public User createUser(User user, String plainPassword) {
        SessionContext.getInstance().requirePermission("USER_MANAGE");
        validateUser(user, 0);
        validatePassword(plainPassword);

        user.setPasswordHash(HashUtil.hash(plainPassword));
        if (user.getStatus() == null) user.setStatus(User.Status.ACTIVE);
        userDAO.insert(user);
        log.info("User created: '{}' id={}", user.getUsername(), user.getId());
        return user;
    }

    @Override
    public User updateUser(User user) {
        SessionContext.getInstance().requirePermission("USER_MANAGE");
        validateUser(user, user.getId());
        userDAO.update(user);
        log.info("User updated: id={} username='{}'", user.getId(), user.getUsername());
        return user;
    }

    @Override
    public void activateUser(int id) {
        SessionContext.getInstance().requirePermission("USER_MANAGE");
        userDAO.activate(id);
        log.info("User activated id={}", id);
    }

    @Override
    public void deactivateUser(int id) {
        SessionContext.getInstance().requirePermission("USER_MANAGE");
        // Prevent deactivating yourself
        int currentUserId = SessionContext.getInstance().getCurrentUser().getId();
        if (id == currentUserId) {
            throw new ValidationException("You cannot deactivate your own account.");
        }
        userDAO.deactivate(id);
        log.info("User deactivated id={}", id);
    }

    @Override
    public List<Role> getAllRoles() {
        // Load roles directly with a simple query via UserDAO connection
        List<Role> roles = new ArrayList<>();
        com.gp.grocerypos.database.DatabaseConnection dbConn =
            com.gp.grocerypos.database.DatabaseConnection.getInstance();
        Connection conn = null;
        try {
            conn = dbConn.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name, description FROM roles ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roles.add(new Role(rs.getInt("id"),
                        rs.getString("name"), rs.getString("description")));
                }
            }
        } catch (Exception e) {
            log.error("Failed to load roles: {}", e.getMessage(), e);
        } finally {
            dbConn.releaseConnection(conn);
        }
        return roles;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateUser(User u, int excludeId) {
        List<String> errors = new ArrayList<>();

        if (u.getUsername() == null || u.getUsername().isBlank()) {
            errors.add("Username is required.");
        } else if (u.getUsername().trim().length() < 3) {
            errors.add("Username must be at least 3 characters.");
        } else {
            boolean exists = excludeId > 0
                ? !userDAO.findByUsername(u.getUsername().trim())
                          .filter(found -> found.getId() != excludeId).isEmpty()
                : userDAO.usernameExists(u.getUsername().trim());
            if (exists) {
                errors.add("Username '" + u.getUsername().trim() + "' is already taken.");
            }
        }

        if (u.getFullName() == null || u.getFullName().isBlank()) {
            errors.add("Full name is required.");
        }

        if (u.getRole() == null || u.getRole().getId() == 0) {
            errors.add("A role must be assigned to the user.");
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }
    }
}
