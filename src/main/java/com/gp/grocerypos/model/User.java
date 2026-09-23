package com.gp.grocerypos.model;

import java.time.LocalDateTime;

/**
 * Represents a system user (admin or cashier).
 * <p>
 * The {@code passwordHash} field holds the BCrypt hash — the plain-text
 * password is never stored in this object after authentication.
 */
public class User {

    public enum Status { ACTIVE, INACTIVE }

    private int           id;
    private String        username;
    private String        passwordHash;
    private String        fullName;
    private String        email;
    private String        phone;
    private Role          role;
    private Status        status;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public int           getId()                { return id; }
    public void          setId(int id)          { this.id = id; }

    public String        getUsername()                  { return username; }
    public void          setUsername(String username)   { this.username = username; }

    public String        getPasswordHash()                     { return passwordHash; }
    public void          setPasswordHash(String passwordHash)  { this.passwordHash = passwordHash; }

    public String        getFullName()                 { return fullName; }
    public void          setFullName(String fullName)  { this.fullName = fullName; }

    public String        getEmail()               { return email; }
    public void          setEmail(String email)   { this.email = email; }

    public String        getPhone()               { return phone; }
    public void          setPhone(String phone)   { this.phone = phone; }

    public Role          getRole()              { return role; }
    public void          setRole(Role role)     { this.role = role; }

    public Status        getStatus()                { return status; }
    public void          setStatus(Status status)   { this.status = status; }

    public LocalDateTime getLastLogin()                      { return lastLogin; }
    public void          setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public LocalDateTime getCreatedAt()                        { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                        { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // -------------------------------------------------------------------------
    // Convenience helpers
    // -------------------------------------------------------------------------

    public boolean isActive() {
        return Status.ACTIVE.equals(status);
    }

    public String getRoleName() {
        return role != null ? role.getName() : "";
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + getRoleName() + "}";
    }
}
