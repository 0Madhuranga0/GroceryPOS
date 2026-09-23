package com.gp.grocerypos.dao;

import com.gp.grocerypos.model.Sale;
import com.gp.grocerypos.model.SaleItem;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access contract for {@link Sale} and {@link SaleItem} entities.
 * <p>
 * Insert methods accept a caller-supplied {@link Connection} so they can
 * participate in the SaleService transaction (BEGIN/COMMIT/ROLLBACK lives
 * in the service layer, not here).
 */
public interface SaleDAO {

    Optional<Sale> findById(int id);
    Optional<Sale> findByInvoiceNumber(String invoiceNumber);

    /** All sales ordered by sale_date DESC. */
    List<Sale> findAll();

    /** Sales within a date range, newest first. */
    List<Sale> findByDateRange(LocalDate from, LocalDate to);

    /** Sales by a specific cashier. */
    List<Sale> findByUser(int userId);

    /** Sales linked to a specific customer. */
    List<Sale> findByCustomer(int customerId);

    /** Sales filtered by payment method. */
    List<Sale> findByPaymentMethod(String paymentMethod);

    /**
     * Inserts the sale header using the provided connection.
     * Sets the generated ID on the sale object and returns it.
     */
    int insertHeader(Sale sale, Connection conn);

    /**
     * Inserts all sale items using the provided connection.
     * Uses a single batch for efficiency.
     */
    void insertItems(List<SaleItem> items, int saleId, Connection conn);

    /**
     * Inserts a payment record using the provided connection.
     */
    void insertPayment(int saleId, String method,
                       java.math.BigDecimal amount, Connection conn);

    /** Loads all items for a given sale ID (own connection). */
    List<SaleItem> findItemsBySaleId(int saleId);

    /** Marks a sale as VOIDED. */
    void voidSale(int saleId);

    /** Marks a sale as RETURNED. */
    void markReturned(int saleId);

    /** Count of completed sales for today. */
    int countToday();

    /** Sum of grand_total for completed sales today. */
    java.math.BigDecimal sumTodaySales();
}
