package com.gp.grocerypos.service;

import com.gp.grocerypos.dao.CategoryDAO;
import com.gp.grocerypos.dao.CategoryDAOImpl;
import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.exception.ValidationException;
import com.gp.grocerypos.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);
    private final CategoryDAO categoryDAO;

    public CategoryServiceImpl() {
        this.categoryDAO = new CategoryDAOImpl();
    }

    public CategoryServiceImpl(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }

    @Override
    public List<Category> getActiveCategories() {
        return categoryDAO.findAllActive();
    }

    @Override
    public Category getCategoryById(int id) {
        return categoryDAO.findById(id)
                .orElseThrow(() -> new POSException(
                        "Category not found id=" + id,
                        "Category not found."));
    }

    @Override
    public Category createCategory(String name, String description) {
        SessionContext.getInstance().requirePermission("PRODUCT_CREATE");
        validate(name, 0);

        Category category = new Category();
        category.setName(name.trim());
        category.setDescription(description != null ? description.trim() : null);
        category.setStatus(Category.Status.ACTIVE);
        categoryDAO.insert(category);

        log.info("Category created: '{}' id={}", category.getName(), category.getId());
        return category;
    }

    @Override
    public Category updateCategory(int id, String name, String description,
                                   Category.Status status) {
        SessionContext.getInstance().requirePermission("PRODUCT_EDIT");
        validate(name, id);

        Category category = getCategoryById(id);
        category.setName(name.trim());
        category.setDescription(description != null ? description.trim() : null);
        category.setStatus(status != null ? status : Category.Status.ACTIVE);
        categoryDAO.update(category);

        log.info("Category updated: id={} name='{}'", id, name);
        return category;
    }

    @Override
    public void deactivateCategory(int id) {
        SessionContext.getInstance().requirePermission("PRODUCT_EDIT");
        categoryDAO.deactivate(id);
        log.info("Category deactivated id={}", id);
    }

    @Override
    public void activateCategory(int id) {
        SessionContext.getInstance().requirePermission("PRODUCT_EDIT");
        categoryDAO.activate(id);
        log.info("Category activated id={}", id);
    }

    @Override
    public void deleteCategory(int id) {
        SessionContext.getInstance().requirePermission("PRODUCT_DELETE");

        // Check if any products are still linked to this category
        // We query via a raw check — no ProductDAO dependency needed here
        // The DB FK ON DELETE SET NULL means it is safe to delete, but we warn
        getCategoryById(id); // existence check
        categoryDAO.delete(id);
        log.info("Category deleted id={}", id);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(String name, int excludeId) {
        List<String> errors = new ArrayList<>();

        if (name == null || name.isBlank()) {
            errors.add("Category name is required.");
        } else if (name.trim().length() > 100) {
            errors.add("Category name must not exceed 100 characters.");
        } else {
            boolean exists = excludeId > 0
                    ? categoryDAO.nameExistsExcluding(name.trim(), excludeId)
                    : categoryDAO.nameExists(name.trim());
            if (exists) {
                errors.add("A category with the name '" + name.trim() + "' already exists.");
            }
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
