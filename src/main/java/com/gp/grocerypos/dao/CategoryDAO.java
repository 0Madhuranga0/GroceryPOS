package com.gp.grocerypos.dao;

import com.gp.grocerypos.model.Category;

import java.util.List;
import java.util.Optional;

/**
 * Data access contract for {@link Category} entities.
 */
public interface CategoryDAO {

    Optional<Category> findById(int id);
    Optional<Category> findByName(String name);
    List<Category>     findAll();
    List<Category>     findAllActive();

    /** Inserts a new category and returns the generated ID. */
    int  insert(Category category);
    void update(Category category);
    void activate(int id);
    void deactivate(int id);
    void delete(int id);

    boolean nameExists(String name);
    boolean nameExistsExcluding(String name, int excludeId);
}
