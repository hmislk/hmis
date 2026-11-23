package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * A credit company claim action was refused (bad bill, wrong status, missing
 * input). Rolls back, so a refused action writes nothing (#25847).
 */
@ApplicationException(rollback = true)
public class CreditCompanyClaimException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CreditCompanyClaimException(String message) {
        super(message);
    }
}
