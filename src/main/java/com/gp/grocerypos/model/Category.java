package com.gp.grocerypos.model;

import java.time.LocalDateTime;

/**
 * Represents a product category (e.g. Dairy & Eggs, Beverages).
 */
public class Category {

    public enum Status { ACTIVE, INACTIVE }

    private int           id;
    private String        name;
    private String        description;
    private Status        status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Category() {}

    public Category(int id, String name) {
        this.id   = id;
        this.name = name;
        this.status = Status.ACTIVE;
    }

    public int           getId()                { return id; }
    public void          setId(int id)          { this.id = id; }

    public String        getName()                  { return name; }
    public void          setName(String name)       { this.name = name; }

    public String        getDescription()                    { return description; }
    public void          setDescription(String description)  { this.description = description; }

    public Status        getStatus()                  { return status; }
    public void          setStatus(Status status)     { this.status = status; }

    public LocalDateTime getCreatedAt()                          { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                          { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime updatedAt)   { this.updatedAt = updatedAt; }

    public boolean isActive() { return Status.ACTIVE.equals(status); }

    @Override
    public String toString() { return name; }
}
