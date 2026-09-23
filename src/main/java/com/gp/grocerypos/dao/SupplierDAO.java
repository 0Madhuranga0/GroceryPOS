package com.gp.grocerypos.dao;

import com.gp.grocerypos.model.Supplier;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for {@link Supplier} entities.
 */
public interface SupplierDAO {

    Optional<Supplier> findById(int id);
    List<Supplier>     findAll();
    List<Supplier>     findAllActive();

    /** Search by name or contact person (case-insensitive LIKE). */
    List<Supplier>     search(String keyword);

    /** Inserts a new supplier and returns the generated ID. */
    int  insert(Supplier supplier);
    void update(Supplier supplier);
    void activate(int id);
    void deactivate(int id);
    void delete(int id);

    boolean nameExists(String name);
    boolean nameExistsExcluding(String name, int excludeId);
}
