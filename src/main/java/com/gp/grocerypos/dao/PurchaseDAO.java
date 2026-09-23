package com.gp.grocerypos.dao;

import com.gp.grocerypos.model.Purchase;
import com.gp.grocerypos.model.PurchaseItem;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access contract for {@link Purchase} and {@link PurchaseItem} entities.
 * <p>
 * Insert methods accept an external {@link Connection} so they can
 * participate in a caller-managed transaction (BEGIN / COMMIT / ROLLBACK
 * lives in PurchaseService, not here).
 */
public interface PurchaseDAO {

    Optional<Purchase> findById(int id);
    Optional<Purchase> findByPurchaseNumber(String purchaseNumber);

    /** All purchases ordered by purchase_date DESC. */
    List<Purchase> findAll();

    /** Purchases filtered by supplier. */
    List<Purchase> findBySupplier(int supplierId);

    /** Purchases within a date range (inclusive), ordered newest first. */
    List<Purchase> findByDateRange(LocalDate from, LocalDate to);

    /**
     * Inserts a new purchase header using the provided connection.
     * The connection must already have autoCommit disabled.
     * Returns the generated ID and sets it on the purchase object.
     */
    int insertHeader(Purchase purchase, Connection conn);

    /**
     * Inserts all purchase items for a purchase using the provided connection.
     */
    void insertItems(List<PurchaseItem> items, int purchaseId, Connection conn);

    /** Loads all items for a given purchase ID. */
    List<PurchaseItem> findItemsByPurchaseId(int purchaseId);

    /** Cancels a purchase (sets status = CANCELLED). */
    void cancel(int purchaseId);

    /** Count of all purchases. */
    int count();
}
