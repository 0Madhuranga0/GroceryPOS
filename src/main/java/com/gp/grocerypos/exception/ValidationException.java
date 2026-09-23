package com.gp.grocerypos.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when user input or business-rule validation fails.
 * <p>
 * Supports both a single error message and a list of field-level
 * validation errors, so the UI can display all issues at once rather
 * than one at a time.
 * <p>
 * Example — single error:
 * <pre>{@code
 *   throw new ValidationException("Selling price must be greater than zero.");
 * }</pre>
 * Example — multiple field errors:
 * <pre>{@code
 *   List<String> errors = new ArrayList<>();
 *   if (name.isBlank())       errors.add("Product name is required.");
 *   if (price.signum() <= 0)  errors.add("Selling price must be greater than zero.");
 *   if (!errors.isEmpty()) throw new ValidationException(errors);
 * }</pre>
 */
public class ValidationException extends POSException {

    private final List<String> errors;

    /**
     * Single validation error.
     *
     * @param message the validation error message (also shown to the user)
     */
    public ValidationException(String message) {
        super(message, message);
        this.errors = List.of(message);
    }

    /**
     * Multiple validation errors collected at once.
     *
     * @param errors list of user-friendly validation error messages
     */
    public ValidationException(List<String> errors) {
        super(
            "Validation failed: " + String.join("; ", errors),
            errors.isEmpty() ? "Validation failed." : errors.get(0)
        );
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /**
     * Returns all validation error messages.
     * Always contains at least one entry.
     *
     * @return unmodifiable list of error messages
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Returns {@code true} if more than one validation error was collected.
     */
    public boolean hasMultipleErrors() {
        return errors.size() > 1;
    }
}
