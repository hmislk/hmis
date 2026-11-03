package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * An agent hold can no longer be completed because, once the session row lock was taken, it was
 * found already paid, cancelled or retired (#25050). Annotated so the container rolls back and
 * delivers it to the caller unwrapped (not as EJBException).
 */
@ApplicationException(rollback = true)
public class ChannelBookingNotAvailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChannelBookingNotAvailableException(String message) {
        super(message);
    }
}
