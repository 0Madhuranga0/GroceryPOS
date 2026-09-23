package com.gp.grocerypos.service;

import com.gp.grocerypos.model.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Business logic contract for customer management.
 */
public interface CustomerService {

    List<Customer> getAllCustomers();
    List<Customer> getActiveCustomers();
    List<Customer> searchCustomers(String keyword);

    Customer         getCustomerById(int id);
    Optional<Customer> getCustomerByPhone(String phone);

    /**
     * Validates and saves a new customer.
     * @throws com.gp.grocerypos.exception.ValidationException if data invalid
     * @throws com.gp.grocerypos.exception.AuthException       if caller lacks permission
     */
    Customer createCustomer(Customer customer);

    /**
     * Validates and updates an existing customer.
     */
    Customer updateCustomer(Customer customer);

    void deactivateCustomer(int id);
    void activateCustomer(int id);

    /** Adds loyalty points to a customer (called after a sale). */
    void addLoyaltyPoints(int customerId, int points);
}
