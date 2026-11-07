package com.divudi.core.facade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

/**
 * MySQL reports a drawer row lock that was not granted as error 1205 wrapped in a
 * generic PersistenceException; it must still be recognised (issue #25123).
 */
class DrawerFacadeLockWaitTest {

    @Test
    void lockWaitTimeoutInCauseChainIsRecognised() {
        SQLException sql = new SQLException("Lock wait timeout exceeded; try restarting transaction", "HY000", 1205);
        assertTrue(DrawerFacade.isLockWaitFailure(new PersistenceException(new RuntimeException(sql))));
    }

    @Test
    void deadlockIsRecognised() {
        SQLException sql = new SQLException("Deadlock found when trying to get lock", "40001", 1213);
        assertTrue(DrawerFacade.isLockWaitFailure(new PersistenceException(sql)));
    }

    @Test
    void otherErrorsAreNot() {
        SQLException sql = new SQLException("Unknown column 'x'", "42S22", 1054);
        assertFalse(DrawerFacade.isLockWaitFailure(new PersistenceException(sql)));
        assertFalse(DrawerFacade.isLockWaitFailure(new PersistenceException("connection refused")));
    }
}
