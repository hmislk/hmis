package com.divudi.service.opd;

import javax.ejb.ApplicationException;

/**
 * A business error while settling an OPD pre-bill (#26246). Rolls back the
 * transaction, so the bill, its payments and the batch links are never saved in
 * part, and keeps its own message for the user.
 */
@ApplicationException(rollback = true)
public class OpdPreSettleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OpdPreSettleException(String message) {
        super(message);
    }

    public OpdPreSettleException(String message, Throwable cause) {
        super(message, cause);
    }
}
