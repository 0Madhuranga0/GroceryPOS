package com.gp.grocerypos.exception;

/**
 * Thrown when a product lookup (by barcode, ID, or name) returns no result.
 * <p>
 * The POS billing module throws this when the barcode scanner submits
 * a code that does not match any active product in the database.
 */
public class ProductNotFoundException extends POSException {

    private final String lookupValue;

    /**
     * @param lookupValue the barcode, ID, or search term that was not found
     */
    public ProductNotFoundException(String lookupValue) {
        super(
            "Product not found for lookup value: " + lookupValue,
            "Product not found: " + lookupValue
        );
        this.lookupValue = lookupValue;
    }

    public ProductNotFoundException(String lookupValue, String userMessage) {
        super("Product not found for lookup value: " + lookupValue, userMessage);
        this.lookupValue = lookupValue;
    }

    public String getLookupValue() {
        return lookupValue;
    }
}
