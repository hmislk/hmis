package com.divudi.service.patient;

import javax.ejb.ApplicationException;

/**
 * Business-rule failure during patient merge or unmerge operations (#24552).
 * Annotated with @ApplicationException(rollback = true) so the EJB container rolls back
 * and delivers the exception unwrapped to callers (rather than wrapping it in EJBException),
 * allowing JSF controllers to display the intended validation message.
 */
@ApplicationException(rollback = true)
public class PatientMergeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PatientMergeException(String message) {
        super(message);
    }
}
