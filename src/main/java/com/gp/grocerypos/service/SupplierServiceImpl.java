package com.gp.grocerypos.service;

import com.gp.grocerypos.dao.SupplierDAO;
import com.gp.grocerypos.dao.SupplierDAOImpl;
import com.gp.grocerypos.exception.POSException;
import com.gp.grocerypos.exception.ValidationException;
import com.gp.grocerypos.model.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SupplierServiceImpl implements SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierServiceImpl.class);
    private final SupplierDAO supplierDAO;

    public SupplierServiceImpl() {
        this.supplierDAO = new SupplierDAOImpl();
    }

    public SupplierServiceImpl(SupplierDAO supplierDAO) {
        this.supplierDAO = supplierDAO;
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        return supplierDAO.findAll();
    }

    @Override
    public List<Supplier> getActiveSuppliers() {
        return supplierDAO.findAllActive();
    }

    @Override
    public List<Supplier> searchSuppliers(String keyword) {
        if (keyword == null || keyword.isBlank()) return supplierDAO.findAll();
        return supplierDAO.search(keyword.trim());
    }

    @Override
    public Supplier getSupplierById(int id) {
        return supplierDAO.findById(id)
                .orElseThrow(() -> new POSException(
                        "Supplier not found id=" + id, "Supplier not found."));
    }

    @Override
    public Supplier createSupplier(Supplier supplier) {
        SessionContext.getInstance().requirePermission("SUPPLIER_MANAGE");
        validate(supplier, 0);
        supplier.setStatus(Supplier.Status.ACTIVE);
        supplierDAO.insert(supplier);
        log.info("Supplier created: '{}' id={}", supplier.getName(), supplier.getId());
        return supplier;
    }

    @Override
    public Supplier updateSupplier(Supplier supplier) {
        SessionContext.getInstance().requirePermission("SUPPLIER_MANAGE");
        validate(supplier, supplier.getId());
        supplierDAO.update(supplier);
        log.info("Supplier updated: id={} name='{}'", supplier.getId(), supplier.getName());
        return supplier;
    }

    @Override
    public void deactivateSupplier(int id) {
        SessionContext.getInstance().requirePermission("SUPPLIER_MANAGE");
        supplierDAO.deactivate(id);
        log.info("Supplier deactivated id={}", id);
    }

    @Override
    public void activateSupplier(int id) {
        SessionContext.getInstance().requirePermission("SUPPLIER_MANAGE");
        supplierDAO.activate(id);
        log.info("Supplier activated id={}", id);
    }

    @Override
    public void deleteSupplier(int id) {
        SessionContext.getInstance().requirePermission("SUPPLIER_MANAGE");
        getSupplierById(id); // existence check
        supplierDAO.delete(id);
        log.info("Supplier deleted id={}", id);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate(Supplier s, int excludeId) {
        List<String> errors = new ArrayList<>();

        if (s.getName() == null || s.getName().isBlank()) {
            errors.add("Supplier name is required.");
        } else if (s.getName().trim().length() > 100) {
            errors.add("Supplier name must not exceed 100 characters.");
        } else {
            boolean exists = excludeId > 0
                    ? supplierDAO.nameExistsExcluding(s.getName().trim(), excludeId)
                    : supplierDAO.nameExists(s.getName().trim());
            if (exists) {
                errors.add("A supplier named '" + s.getName().trim() + "' already exists.");
            }
        }

        if (s.getPhone() != null && s.getPhone().length() > 20) {
            errors.add("Phone number must not exceed 20 characters.");
        }

        if (s.getEmail() != null && !s.getEmail().isBlank()) {
            if (!s.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                errors.add("Email address is not valid.");
            }
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);
    }
}
