package com.divudi.service.forms;

import javax.ejb.ApplicationException;

/**
 * Business-rule or validation failure during form API operations (#24552).
 * Annotated with @ApplicationException(rollback = true) so the EJB container rolls back
 * and delivers the exception unwrapped to callers (rather than wrapping it in EJBException),
 * allowing REST API endpoints to return intended HTTP status codes (400 or 404).
 */
@ApplicationException(rollback = true)
public class FormApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final int status;

    public FormApiException(String message) {
        this(message, message != null && message.toLowerCase().contains("not found") ? 404 : 400);
    }

    public FormApiException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
