package com.gp.grocerypos.service;

import com.gp.grocerypos.model.Purchase;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business logic contract for purchase management.
 * <p>
 * The {@link #savePurchase(Purchase)} method is the core operation — it
 * runs the full purchase transaction atomically:
 * <ol>
 *   <li>Validate the purchase</li>
 *   <li>Generate a unique purchase number</li>
 *   <li>INSERT into purchases (header)</li>
 *   <li>INSERT into purchase_items (all lines)</li>
 *   <li>UPDATE products.current_stock for each item</li>
 *   <li>INSERT into inventory_movements for each item</li>
 *   <li>COMMIT — or ROLLBACK on any failure</li>
 * </ol>
 */
public interface PurchaseService {

    List<Purchase> getAllPurchases();
    List<Purchase> getPurchasesBySupplier(int supplierId);
    List<Purchase> getPurchasesByDateRange(LocalDate from, LocalDate to);

    Optional<Purchase> getPurchaseById(int id);

    /**
     * Executes the complete purchase transaction.
     *
     * @param purchase a fully populated Purchase with at least one item
     * @return the saved Purchase with generated ID and purchase number
     * @throws com.gp.grocerypos.exception.ValidationException if cart is empty or data invalid
     * @throws com.gp.grocerypos.exception.AuthException       if caller lacks PURCHASE_CREATE
     * @throws com.gp.grocerypos.exception.DatabaseException   if the transaction fails
     */
    Purchase savePurchase(Purchase purchase);

    /** Cancels a purchase (does NOT reverse stock). */
    void cancelPurchase(int purchaseId);
}
