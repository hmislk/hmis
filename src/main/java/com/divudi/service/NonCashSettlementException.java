package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * Business-rule failure in the Non-Cash Settlement workflow (#24541). Annotated so the
 * container rolls back and delivers it to the caller unwrapped (not as EJBException),
 * letting controllers show the message.
 */
@ApplicationException(rollback = true)
public class NonCashSettlementException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NonCashSettlementException(String message) {
        super(message);
    }
}
