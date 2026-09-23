package com.gp.grocerypos.dao;

import com.gp.grocerypos.model.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for {@link Customer} entities.
 */
public interface CustomerDAO {

    Optional<Customer> findById(int id);
    Optional<Customer> findByPhone(String phone);
    List<Customer>     findAll();
    List<Customer>     findAllActive();

    /** Search by name or phone number (case-insensitive LIKE). */
    List<Customer>     search(String keyword);

    /** Inserts a new customer and returns the generated ID. */
    int  insert(Customer customer);
    void update(Customer customer);
    void activate(int id);
    void deactivate(int id);

    /** Add or subtract loyalty points (pass negative value to subtract). */
    void addLoyaltyPoints(int customerId, int points);

    boolean phoneExists(String phone);
    boolean phoneExistsExcluding(String phone, int excludeId);
}
