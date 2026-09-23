package com.gp.grocerypos.model;

import java.time.LocalDateTime;

/**
 * Represents a customer. Customers are optional on a normal cash sale
 * but can be linked for loyalty points tracking.
 */
public class Customer {

    public enum Status { ACTIVE, INACTIVE }

    private int           id;
    private String        name;
    private String        phone;
    private String        email;
    private String        address;
    private int           loyaltyPoints;
    private Status        status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Customer() {}

    public int           getId()                { return id; }
    public void          setId(int id)          { this.id = id; }

    public String        getName()                  { return name; }
    public void          setName(String name)       { this.name = name; }

    public String        getPhone()                 { return phone; }
    public void          setPhone(String phone)     { this.phone = phone; }

    public String        getEmail()                 { return email; }
    public void          setEmail(String email)     { this.email = email; }

    public String        getAddress()               { return address; }
    public void          setAddress(String address) { this.address = address; }

    public int           getLoyaltyPoints()                      { return loyaltyPoints; }
    public void          setLoyaltyPoints(int loyaltyPoints)     { this.loyaltyPoints = loyaltyPoints; }

    public Status        getStatus()                { return status; }
    public void          setStatus(Status status)   { this.status = status; }

    public LocalDateTime getCreatedAt()                          { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                          { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime updatedAt)   { this.updatedAt = updatedAt; }

    public boolean isActive() { return Status.ACTIVE.equals(status); }

    @Override
    public String toString() { return name + (phone != null ? " (" + phone + ")" : ""); }
}
