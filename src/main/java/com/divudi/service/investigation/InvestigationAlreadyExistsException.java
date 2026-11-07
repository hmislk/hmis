package com.divudi.service.investigation;

import javax.ejb.ApplicationException;

/**
 * An active investigation with the requested name already exists. Delivered
 * unwrapped to the REST layer, which answers 409 with the existing id.
 */
@ApplicationException(rollback = true)
public class InvestigationAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Long existingId;

    public InvestigationAlreadyExistsException(Long existingId) {
        super("Investigation already exists with ID: " + existingId);
        this.existingId = existingId;
    }

    public Long getExistingId() {
        return existingId;
    }
}
