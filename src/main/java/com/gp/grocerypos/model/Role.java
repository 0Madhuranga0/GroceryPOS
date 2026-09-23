package com.gp.grocerypos.model;

import java.time.LocalDateTime;

/**
 * Represents a user role (e.g. ADMIN, CASHIER).
 */
public class Role {

    private int           id;
    private String        name;
    private String        description;
    private LocalDateTime createdAt;

    public Role() {}

    public Role(int id, String name, String description) {
        this.id          = id;
        this.name        = name;
        this.description = description;
    }

    // Getters and setters
    public int           getId()          { return id; }
    public void          setId(int id)    { this.id = id; }

    public String        getName()             { return name; }
    public void          setName(String name)  { this.name = name; }

    public String        getDescription()                   { return description; }
    public void          setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt()                      { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Role{id=" + id + ", name='" + name + "'}";
    }
}
