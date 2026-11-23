package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * A patient complaint action was refused (wrong status, missing response,
 * not the patient's own complaint). Rolls back, so a refused action writes
 * nothing (#25886).
 */
@ApplicationException(rollback = true)
public class PatientComplaintException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PatientComplaintException(String message) {
        super(message);
    }
}
