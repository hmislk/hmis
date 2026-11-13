package com.divudi.service.clinical;

import javax.ejb.ApplicationException;

/**
 * Invalid input to the Favourite Medicine API service (#24552). Annotated so the
 * container rolls back and delivers it to the REST class unwrapped (not as
 * EJBException), so the API answers 400 with the message instead of 500.
 * Extends IllegalArgumentException so existing callers that catch that type keep working.
 */
@ApplicationException(rollback = true)
public class FavouriteMedicineValidationException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public FavouriteMedicineValidationException(String message) {
        super(message);
    }
}
