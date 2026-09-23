package com.gp.grocerypos.model;

import java.time.LocalDateTime;

/**
 * Represents a product supplier / vendor.
 */
public class Supplier {

    public enum Status { ACTIVE, INACTIVE }

    private int           id;
    private String        name;
    private String        contactPerson;
    private String        phone;
    private String        email;
    private String        address;
    private String        notes;
    private Status        status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Supplier() {}

    public int           getId()                    { return id; }
    public void          setId(int id)              { this.id = id; }

    public String        getName()                  { return name; }
    public void          setName(String name)       { this.name = name; }

    public String        getContactPerson()                       { return contactPerson; }
    public void          setContactPerson(String contactPerson)   { this.contactPerson = contactPerson; }

    public String        getPhone()                 { return phone; }
    public void          setPhone(String phone)     { this.phone = phone; }

    public String        getEmail()                 { return email; }
    public void          setEmail(String email)     { this.email = email; }

    public String        getAddress()               { return address; }
    public void          setAddress(String address) { this.address = address; }

    public String        getNotes()                 { return notes; }
    public void          setNotes(String notes)     { this.notes = notes; }

    public Status        getStatus()                { return status; }
    public void          setStatus(Status status)   { this.status = status; }

    public LocalDateTime getCreatedAt()                          { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                          { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime updatedAt)   { this.updatedAt = updatedAt; }

    public boolean isActive() { return Status.ACTIVE.equals(status); }

    @Override
    public String toString() { return name; }
}
