package com.gp.grocerypos.service;

import com.gp.grocerypos.model.Category;

import java.util.List;

/**
 * Business logic contract for category management.
 */
public interface CategoryService {

    List<Category> getAllCategories();
    List<Category> getActiveCategories();

    Category getCategoryById(int id);

    /**
     * Validates and saves a new category.
     * @throws com.gp.grocerypos.exception.ValidationException if data is invalid
     * @throws com.gp.grocerypos.exception.AuthException if caller lacks permission
     */
    Category createCategory(String name, String description);

    /**
     * Validates and updates an existing category.
     */
    Category updateCategory(int id, String name, String description,
                            Category.Status status);

    void deactivateCategory(int id);
    void activateCategory(int id);

    /**
     * Hard-deletes a category only if no products are linked to it.
     * @throws com.gp.grocerypos.exception.ValidationException if products exist
     */
    void deleteCategory(int id);
}
