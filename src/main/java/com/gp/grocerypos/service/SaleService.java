package com.gp.grocerypos.service;

import com.gp.grocerypos.model.Sale;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business logic contract for sale transactions.
 * <p>
 * {@link #completeSale(Sale)} is the core atomic operation:
 * <pre>
 * BEGIN
 *   1. Validate cart (non-empty, all products active, sufficient stock)
 *   2. Generate invoice number
 *   3. INSERT into sales
 *   4. INSERT into sale_items
 *   5. INSERT into payments
 *   6. Reduce product stock for each item
 *   7. INSERT into inventory_movements for each item
 * COMMIT  (ROLLBACK on any failure)
 * </pre>
 */
public interface SaleService {

    List<Sale> getAllSales();
    List<Sale> getSalesByDateRange(LocalDate from, LocalDate to);
    List<Sale> getSalesByUser(int userId);
    List<Sale> getSalesByCustomer(int customerId);

    Optional<Sale> getSaleById(int id);
    Optional<Sale> getSaleByInvoiceNumber(String invoiceNumber);

    /**
     * Executes the complete sale transaction.
     *
     * @param sale fully populated Sale with items, payment method, cash received
     * @return the saved Sale with generated ID and invoice number
     * @throws com.gp.grocerypos.exception.ValidationException if cart is empty or invalid
     * @throws com.gp.grocerypos.exception.StockException      if any item has insufficient stock
     * @throws com.gp.grocerypos.exception.SaleException       if the transaction fails
     * @throws com.gp.grocerypos.exception.AuthException       if caller lacks SALE_CREATE
     */
    Sale completeSale(Sale sale);

    /**
     * Voids a completed sale. Does NOT automatically reverse stock.
     * Stock reversal should be handled via a return/refund process.
     */
    void voidSale(int saleId);

    /** Today's completed sale count. */
    int  countTodaySales();

    /** Today's total sales revenue (sum of grand_total for COMPLETED sales). */
    java.math.BigDecimal sumTodaySales();
}
