package com.divudi.service.inward;

import javax.ejb.ApplicationException;

/**
 * Business-rule validation failure during item request operations (#24552).
 * Annotated with @ApplicationException(rollback = true) so that EJB container rolls back
 * and delivers the exception unwrapped to callers (rather than wrapping in EJBException),
 * allowing JSF controllers to display the validation message.
 */
@ApplicationException(rollback = true)
public class ItemRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ItemRequestException(String message) {
        super(message);
    }
}

