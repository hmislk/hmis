/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.facade;

import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.service.DrawerBusyException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;

/**
 *
 * @author safrin
 */
@Stateless
public class DrawerFacade extends AbstractFacade<Drawer> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    /**
     * Lock-timeout hint, in milliseconds, for databases whose EclipseLink platform
     * supports FOR UPDATE WAIT n. EclipseLink's MySQLPlatform emits a plain FOR UPDATE
     * and ignores it, so on MySQL a blocked update waits innodb_lock_wait_timeout
     * (50 s by default) and then fails with error 1205, mapped below.
     */
    private static final int LOCK_TIMEOUT_HINT_MS = 10000;

    /** MySQL "Lock wait timeout exceeded" and "Deadlock found". */
    private static final int MYSQL_LOCK_WAIT_TIMEOUT = 1205;
    private static final int MYSQL_DEADLOCK = 1213;

    /**
     * The drawer re-read from the database with a row lock (SELECT ... FOR UPDATE),
     * held until the caller's transaction ends. Must run inside the caller's
     * transaction (e.g. from DrawerService); called with no transaction, the lock
     * is released as soon as this method returns.
     *
     * @throws DrawerBusyException when the database gives up waiting for the lock
     * (on MySQL after innodb_lock_wait_timeout) or picks this request as a deadlock
     * victim, so the cashier gets a clear message instead of a generic error
     */
    public Drawer findForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        Map<String, Object> hints = new HashMap<>();
        hints.put("javax.persistence.lock.timeout", LOCK_TIMEOUT_HINT_MS);
        try {
            return em.find(Drawer.class, id, LockModeType.PESSIMISTIC_WRITE, hints);
        } catch (PessimisticLockException | LockTimeoutException e) {
            throw drawerBusy();
        } catch (PersistenceException e) {
            if (isLockWaitFailure(e)) {
                throw drawerBusy();
            }
            throw e;
        }
    }

    private static DrawerBusyException drawerBusy() {
        return new DrawerBusyException("The cash drawer is being updated by another transaction. Please try again.");
    }

    /** True when the cause chain holds MySQL error 1205 or 1213. */
    static boolean isLockWaitFailure(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof SQLException) {
                int code = ((SQLException) c).getErrorCode();
                if (code == MYSQL_LOCK_WAIT_TIMEOUT || code == MYSQL_DEADLOCK) {
                    return true;
                }
            }
            String msg = c.getMessage();
            if (msg != null && (msg.contains("Lock wait timeout exceeded") || msg.contains("Deadlock found"))) {
                return true;
            }
        }
        return false;
    }

    public DrawerFacade() {
        super(Drawer.class);
    }

}
