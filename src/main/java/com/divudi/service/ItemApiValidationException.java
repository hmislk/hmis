package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * An item API request was refused (missing or invalid input, or a referenced
 * record was not found). Rolls back, so a refused create or update writes
 * nothing, even when some fields were already set (#25992).
 *
 * Checked, so callers that declare and catch {@code Exception} keep working.
 */
@ApplicationException(rollback = true)
public class ItemApiValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    public ItemApiValidationException(String message) {
        super(message);
    }
}
