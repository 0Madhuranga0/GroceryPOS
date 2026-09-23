package com.gp.grocerypos.service;

import com.gp.grocerypos.dao.CustomerDAO;
import com.gp.grocerypos.dao.CustomerDAOImpl;
import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.exception.ValidationException;
import com.gp.grocerypos.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);
    private final CustomerDAO customerDAO;

    public CustomerServiceImpl() {
        this.customerDAO = new CustomerDAOImpl();
    }

    public CustomerServiceImpl(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    @Override
    public List<Customer> getActiveCustomers() {
        return customerDAO.findAllActive();
    }

    @Override
    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) return customerDAO.findAll();
        return customerDAO.search(keyword.trim());
    }

    @Override
    public Customer getCustomerById(int id) {
        return customerDAO.findById(id)
                .orElseThrow(() -> new POSException(
                        "Customer not found id=" + id, "Customer not found."));
    }

    @Override
    public Optional<Customer> getCustomerByPhone(String phone) {
        if (phone == null || phone.isBlank()) return Optional.empty();
        return customerDAO.findByPhone(phone.trim());
    }

    @Override
    public Customer createCustomer(Customer customer) {
        // Customers can be created by cashiers (CUSTOMER_VIEW) and admins (CUSTOMER_MANAGE)
        // We allow any logged-in user to create customers for the POS workflow
        validate(customer, 0);
        customer.setStatus(Customer.Status.ACTIVE);
        if (customer.getLoyaltyPoints() < 0) customer.setLoyaltyPoints(0);
        customerDAO.insert(customer);
        log.info("Customer created: '{}' id={}", customer.getName(), customer.getId());
        return customer;
    }

    @Override
    public Customer updateCustomer(Customer customer) {
        SessionContext.getInstance().requirePermission("CUSTOMER_MANAGE");
        validate(customer, customer.getId());
        customerDAO.update(customer);
        log.info("Customer updated: id={} name='{}'", customer.getId(), customer.getName());
        return customer;
    }

    @Override
    public void deactivateCustomer(int id) {
        SessionContext.getInstance().requirePermission("CUSTOMER_MANAGE");
        customerDAO.deactivate(id);
        log.info("Customer deactivated id={}", id);
    }

    @Override
    public void activateCustomer(int id) {
        SessionContext.getInstance().requirePermission("CUSTOMER_MANAGE");
        customerDAO.activate(id);
        log.info("Customer activated id={}", id);
    }

    @Override
    public void addLoyaltyPoints(int customerId, int points) {
        if (points == 0) return;
        customerDAO.addLoyaltyPoints(customerId, points);
        log.debug("Added {} loyalty points to customer id={}", points, customerId);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(Customer c, int excludeId) {
        List<String> errors = new ArrayList<>();

        if (c.getName() == null || c.getName().isBlank()) {
            errors.add("Customer name is required.");
        } else if (c.getName().trim().length() > 100) {
            errors.add("Customer name must not exceed 100 characters.");
        }

        if (c.getPhone() != null && !c.getPhone().isBlank()) {
            if (c.getPhone().length() > 20) {
                errors.add("Phone number must not exceed 20 characters.");
            } else {
                boolean exists = excludeId > 0
                        ? customerDAO.phoneExistsExcluding(c.getPhone().trim(), excludeId)
                        : customerDAO.phoneExists(c.getPhone().trim());
                if (exists) {
                    errors.add("A customer with phone '" + c.getPhone().trim()
                            + "' already exists.");
                }
            }
        }

        if (c.getEmail() != null && !c.getEmail().isBlank()) {
            if (!c.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                errors.add("Email address is not valid.");
            }
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
