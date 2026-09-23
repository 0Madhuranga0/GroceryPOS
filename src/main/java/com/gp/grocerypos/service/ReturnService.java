package com.gp.grocerypos.service;

import com.gp.grocerypos.model.SaleReturn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business logic contract for sales return / refund transactions.
 * <p>
 * {@link #processReturn(SaleReturn)} is the core atomic operation:
 * <pre>
 * BEGIN
 *   1. Validate the return (sale exists, COMPLETED, quantities ≤ sold)
 *   2. Generate return number
 *   3. INSERT into returns (header)
 *   4. INSERT into return_items
 *   5. For each item where restock=true:
 *        UPDATE products.current_stock (+quantity)
 *        INSERT into inventory_movements (RETURN_IN)
 *   6. UPDATE sales.status = 'RETURNED'
 * COMMIT  (ROLLBACK on any failure)
 * </pre>
 */
public interface ReturnService {

    List<SaleReturn> getAllReturns();
    List<SaleReturn> getReturnsByDateRange(LocalDate from, LocalDate to);
    List<SaleReturn> getReturnsBySaleId(int saleId);

    Optional<SaleReturn> getReturnById(int id);

    /**
     * Executes the complete return transaction.
     *
     * @param saleReturn a populated SaleReturn with at least one ReturnItem
     * @return the saved SaleReturn with generated ID and return number
     * @throws com.gp.grocerypos.exception.ValidationException if items are invalid
     * @throws com.gp.grocerypos.exception.AuthException       if caller lacks RETURN_PROCESS
     * @throws com.gp.grocerypos.exception.DatabaseException   if the transaction fails
     */
    SaleReturn processReturn(SaleReturn saleReturn);
}
