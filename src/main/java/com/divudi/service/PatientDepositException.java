package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * A business error while a bill takes money from a patient's deposit record.
 * Rolls back the transaction and keeps its own message for the cashier.
 */
@ApplicationException(rollback = true)
public class PatientDepositException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PatientDepositException(String message) {
        super(message);
    }
}
