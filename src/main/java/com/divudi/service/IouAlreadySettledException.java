package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * An IOU chosen for settlement is no longer open. Rolls back the claim, so a
 * refused settlement writes nothing.
 */
@ApplicationException(rollback = true)
public class IouAlreadySettledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IouAlreadySettledException(String message) {
        super(message);
    }
}
