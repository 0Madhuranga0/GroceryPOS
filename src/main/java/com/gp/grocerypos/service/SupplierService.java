package com.gp.grocerypos.service;

import com.gp.grocerypos.model.Supplier;

import java.util.List;

/**
 * Business logic contract for supplier management.
 */
public interface SupplierService {

    List<Supplier> getAllSuppliers();
    List<Supplier> getActiveSuppliers();
    List<Supplier> searchSuppliers(String keyword);

    Supplier getSupplierById(int id);

    /**
     * Validates and creates a new supplier.
     * @throws com.gp.grocerypos.exception.ValidationException if data is invalid
     * @throws com.gp.grocerypos.exception.AuthException if caller lacks permission
     */
    Supplier createSupplier(Supplier supplier);

    /**
     * Validates and updates an existing supplier.
     */
    Supplier updateSupplier(Supplier supplier);

    void deactivateSupplier(int id);
    void activateSupplier(int id);
    void deleteSupplier(int id);
}
