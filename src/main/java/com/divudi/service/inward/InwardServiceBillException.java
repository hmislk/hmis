package com.divudi.service.inward;

import javax.ejb.ApplicationException;

/**
 * A business error while settling inward service bills (#17562). Rolls back the
 * transaction, so the bills are never saved in part, and keeps its own message
 * for the user.
 */
@ApplicationException(rollback = true)
public class InwardServiceBillException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InwardServiceBillException(String message) {
        super(message);
    }

    public InwardServiceBillException(String message, Throwable cause) {
        super(message, cause);
    }
}
