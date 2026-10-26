package com.divudi.service.inward;

import javax.ejb.ApplicationException;

/**
 * Signals a request-validation failure (mapped to HTTP 400 by the REST layer).
 *
 * <p>{@code @ApplicationException(rollback = true)} is required here: without it, the EJB
 * container treats this checked exception as a normal application exception that does NOT
 * automatically roll back the surrounding container-managed transaction. Since header fields
 * are persisted (e.g. {@code packageFacade.create(pkg)}) before item-level validation runs,
 * an item validation failure thrown without this annotation would leave a partially-created,
 * broken entity committed to the database even though the API response reports failure and
 * nothing was created.</p>
 */
@ApplicationException(rollback = true)
public class InpatientPackageValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    public InpatientPackageValidationException(String message) {
        super(message);
    }
}
