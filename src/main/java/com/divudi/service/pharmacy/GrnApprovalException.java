package com.divudi.service.pharmacy;

import javax.ejb.ApplicationException;

/**
 * Business-rule or concurrent-approval failure during GRN approval (#24552).
 * Annotated with @ApplicationException(rollback = true) so that EJB container rolls back
 * and delivers the exception unwrapped to callers (rather than wrapping in EJBException),
 * allowing JSF controllers to catch and display the race message to the user.
 */
@ApplicationException(rollback = true)
public class GrnApprovalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GrnApprovalException(String message) {
        super(message);
    }
}
