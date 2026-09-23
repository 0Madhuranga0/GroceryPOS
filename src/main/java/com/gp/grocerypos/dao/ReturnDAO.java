package com.gp.grocerypos.dao;

import com.gp.grocerypos.model.ReturnItem;
import com.gp.grocerypos.model.SaleReturn;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access contract for {@link SaleReturn} and {@link ReturnItem} entities.
 * Insert methods accept a caller-supplied {@link Connection} to participate
 * in the ReturnService transaction.
 */
public interface ReturnDAO {

    Optional<SaleReturn> findById(int id);
    Optional<SaleReturn> findByReturnNumber(String returnNumber);

    /** All returns for a given original sale ID. */
    List<SaleReturn> findBySaleId(int saleId);

    /** All returns within a date range, newest first. */
    List<SaleReturn> findByDateRange(LocalDate from, LocalDate to);

    /** All returns ordered by return_date DESC. */
    List<SaleReturn> findAll();

    /**
     * Inserts the return header using the provided connection.
     * Sets the generated ID on the SaleReturn object and returns it.
     */
    int insertHeader(SaleReturn saleReturn, Connection conn);

    /**
     * Inserts all return items using the provided connection.
     */
    void insertItems(List<ReturnItem> items, int returnId, Connection conn);

    /** Loads all items for a given return ID (own connection). */
    List<ReturnItem> findItemsByReturnId(int returnId);
}
