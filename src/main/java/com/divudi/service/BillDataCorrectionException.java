package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * Business-rule or validation failure during bill data correction operations (#24552).
 * Annotated with @ApplicationException(rollback = true) so the EJB container rolls back
 * and delivers the exception unwrapped to callers (rather than wrapping it in EJBException),
 * allowing REST API endpoints to return intended HTTP status codes (400 or 409).
 */
@ApplicationException(rollback = true)
public class BillDataCorrectionException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final int status;

    public BillDataCorrectionException(String message) {
        this(message, 400);
    }

    public BillDataCorrectionException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
