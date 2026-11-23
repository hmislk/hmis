package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * An OPD quotation could not be saved (missing input). Rolls back, so a
 * refused save writes nothing (#25885).
 */
@ApplicationException(rollback = true)
public class OpdQuotationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OpdQuotationException(String message) {
        super(message);
    }
}
