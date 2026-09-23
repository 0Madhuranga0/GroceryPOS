package com.gp.grocerypos.dao;

import com.gp.grocerypos.model.InventoryMovement;

import java.time.LocalDate;
import java.util.List;

/**
 * Data access contract for {@link InventoryMovement} records.
 * Movements are append-only — never update or delete existing records.
 */
public interface InventoryMovementDAO {

    /**
     * Records a new inventory movement and returns the generated ID.
     * The product's current_stock is NOT changed here — that is the
     * responsibility of the caller (ProductDAO.updateStock).
     */
    int insert(InventoryMovement movement);

    /** All movements for a specific product, newest first. */
    List<InventoryMovement> findByProduct(int productId);

    /** All movements for a specific product within a date range. */
    List<InventoryMovement> findByProductAndDateRange(
            int productId, LocalDate from, LocalDate to);

    /** All movements of a specific type (e.g. all SALEs), newest first. */
    List<InventoryMovement> findByType(InventoryMovement.MovementType type);

    /** All movements linked to a specific sale or purchase reference. */
    List<InventoryMovement> findByReference(int referenceId, String referenceType);

    /** All movements performed by a specific user. */
    List<InventoryMovement> findByUser(int userId);

    /** All movements within a date range, newest first. Useful for reports. */
    List<InventoryMovement> findByDateRange(LocalDate from, LocalDate to);

    /** Most recent N movements across all products. Used by dashboard. */
    List<InventoryMovement> findRecent(int limit);
}
