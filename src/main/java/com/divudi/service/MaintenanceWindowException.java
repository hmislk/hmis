package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * Validation or business-rule failure on a maintenance window (#25838).
 * Rolls back and reaches the caller unwrapped, so the page or REST class can show the message.
 */
@ApplicationException(rollback = true)
public class MaintenanceWindowException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final int status;

    public MaintenanceWindowException(String message) {
        this(message, 400);
    }

    public MaintenanceWindowException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
