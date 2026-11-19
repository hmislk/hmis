package com.divudi.service.pharmacy;

import javax.ejb.ApplicationException;

/**
 * Business-rule failure while moving a department's stock quantity from one
 * batch to another (#17799). Annotated so the container rolls the transaction
 * back and hands the exception to the JSF controller unwrapped (not as an
 * EJBException), letting the controller show the message.
 */
@ApplicationException(rollback = true)
public class PharmacyBatchChangeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PharmacyBatchChangeException(String message) {
        super(message);
    }
}
