package com.gp.grocerypos.exception;

import java.math.BigDecimal;

/**
 * Thrown when a stock-related business rule is violated.
 * <p>
 * Examples:
 * <ul>
 *   <li>Attempting to sell more units than are in stock</li>
 *   <li>Attempting to return more units than were sold</li>
 *   <li>A product is inactive and cannot be sold</li>
 * </ul>
 */
public class StockException extends POSException {

    private final int        productId;
    private final String     productName;
    private final BigDecimal availableStock;
    private final BigDecimal requestedQuantity;

    /**
     * General stock exception without quantity detail.
     */
    public StockException(String message, String userMessage) {
        super(message, userMessage);
        this.productId         = 0;
        this.productName       = null;
        this.availableStock    = null;
        this.requestedQuantity = null;
    }

    /**
     * Insufficient stock exception with full quantity context.
     *
     * @param productId         the product that ran out
     * @param productName       the product name for the error message
     * @param availableStock    current stock level
     * @param requestedQuantity quantity that was attempted
     */
    public StockException(int productId, String productName,
                          BigDecimal availableStock, BigDecimal requestedQuantity) {
        super(
            String.format("Insufficient stock for product '%s' (id=%d). " +
                          "Available: %s, Requested: %s",
                          productName, productId, availableStock, requestedQuantity),
            String.format("Insufficient stock for '%s'. Available: %s, Requested: %s.",
                          productName, availableStock.stripTrailingZeros().toPlainString(),
                          requestedQuantity.stripTrailingZeros().toPlainString())
        );
        this.productId         = productId;
        this.productName       = productName;
        this.availableStock    = availableStock;
        this.requestedQuantity = requestedQuantity;
    }

    public int        getProductId()         { return productId; }
    public String     getProductName()       { return productName; }
    public BigDecimal getAvailableStock()    { return availableStock; }
    public BigDecimal getRequestedQuantity() { return requestedQuantity; }
}
