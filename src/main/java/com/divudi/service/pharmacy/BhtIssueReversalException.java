package com.divudi.service.pharmacy;

import javax.ejb.ApplicationException;

/**
 * A data-admin reversal of a BHT direct issue could not be done. Rolls back the
 * whole reversal, so no stock change stays (#26109).
 */
@ApplicationException(rollback = true)
public class BhtIssueReversalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BhtIssueReversalException(String message) {
        super(message);
    }

    public BhtIssueReversalException(String message, Throwable cause) {
        super(message, cause);
    }
}
