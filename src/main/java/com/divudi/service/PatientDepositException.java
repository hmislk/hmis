package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * Business error from the patient deposit service. Rolls back the
 * transaction and keeps its own message for the cashier.
 */
@ApplicationException(rollback = true)
public class PatientDepositException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PatientDepositException(String message) {
        super(message);
    }
}
