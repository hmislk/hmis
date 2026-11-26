package com.divudi.service.inward;

import javax.ejb.ApplicationException;

/**
 * An admission type change was refused (same type, discharged, final bill
 * settled, no reason). Rolls the transaction back and reaches the caller
 * unwrapped so the page can show the message (#25814).
 */
@ApplicationException(rollback = true)
public class AdmissionTypeChangeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AdmissionTypeChangeException(String message) {
        super(message);
    }
}
