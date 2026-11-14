package com.divudi.service.inward;

import javax.ejb.ApplicationException;

/**
 * A business error while saving an inward payment (#25610). Rolls back the
 * transaction, so the payment bill is never saved without its payment.
 */
@ApplicationException(rollback = true)
public class InwardPaymentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InwardPaymentException(String message) {
        super(message);
    }

    public InwardPaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
