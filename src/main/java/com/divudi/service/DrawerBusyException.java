package com.divudi.service;

import javax.ejb.ApplicationException;

/**
 * Another request held the row lock on this cash drawer for too long (issue
 * #25123). Rolls back the transaction the drawer update runs in, so neither the
 * drawer history entry nor the balance change is saved without the other.
 */
@ApplicationException(rollback = true)
public class DrawerBusyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DrawerBusyException(String message) {
        super(message);
    }
}
