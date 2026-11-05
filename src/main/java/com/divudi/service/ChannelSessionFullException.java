package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * A channel session has reached its maximum number of bookings (#25050). Annotated so the
 * container rolls back and delivers it to the caller unwrapped (not as EJBException), letting
 * the REST layer answer "Session is Full".
 */
@ApplicationException(rollback = true)
public class ChannelSessionFullException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChannelSessionFullException(String message) {
        super(message);
    }
}
