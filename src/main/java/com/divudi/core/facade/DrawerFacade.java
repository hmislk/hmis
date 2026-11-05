/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.facade;

import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.service.DrawerBusyException;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.PersistenceContext;
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

    /** How long a drawer update waits for another request's lock on the same drawer. */
    private static final int LOCK_TIMEOUT_MS = 10000;

    /**
     * The drawer re-read from the database with a row lock (SELECT ... FOR UPDATE),
     * held until the caller's transaction ends. Must run inside the caller's
     * transaction (e.g. from DrawerService); called with no transaction, the lock
     * is released as soon as this method returns.
     *
     * @throws DrawerBusyException when the lock is not granted in time, so a stuck
     * transaction fails the request with a message instead of hanging the cashier
     */
    public Drawer findForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        Map<String, Object> hints = new HashMap<>();
        hints.put("javax.persistence.lock.timeout", LOCK_TIMEOUT_MS);
        try {
            return em.find(Drawer.class, id, LockModeType.PESSIMISTIC_WRITE, hints);
        } catch (PessimisticLockException | LockTimeoutException e) {
            throw new DrawerBusyException("The cash drawer is being updated by another transaction. Please try again.");
        }
    }

    public DrawerFacade() {
        super(Drawer.class);
    }

}
