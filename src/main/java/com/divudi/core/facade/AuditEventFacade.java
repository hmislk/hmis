/*
 * author Senula Nanayakkara
 */

package com.divudi.core.facade;

import com.divudi.core.entity.AuditEvent;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Sniper 619
 */
@Stateless
public class AuditEventFacade extends AbstractFacade<AuditEvent> {
    @PersistenceContext(unitName = "hmisAuditPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public AuditEventFacade() {
        super(AuditEvent.class);
    }

    /**
     * Persist an audit event in its OWN transaction.
     *
     * Audit writing is best-effort and must never be able to fail a business
     * operation. With the default REQUIRED attribute this facade joins the
     * caller's JTA transaction, so an audit INSERT that fails (e.g. an
     * EclipseLink sequencing edge case on a freshly migrated audit database
     * that intermittently emits "Field 'ID' doesn't have a default value")
     * calls setRollbackOnly() on the shared transaction and takes the caller's
     * work — an OPD bill settle, a discharge, etc. — down with it, even though
     * {@code AuditEventApplicationController.saveAuditEvent} catches the
     * exception.
     *
     * REQUIRES_NEW suspends the caller's transaction and runs the audit INSERT
     * in a separate one. If the audit write fails, only this new transaction
     * rolls back; the caller's transaction is untouched and the caught
     * exception is logged and ignored by the caller as intended.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void create(AuditEvent entity) {
        super.create(entity);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void edit(AuditEvent entity) {
        super.edit(entity);
    }

}
