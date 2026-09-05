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
// Audit writing is best-effort and must NEVER be able to fail a business
// operation. With the default REQUIRED attribute this facade joins the
// caller's JTA transaction, so an audit INSERT that fails (e.g. an
// EclipseLink sequencing edge case on a freshly migrated audit database
// that intermittently emits "Field 'ID' doesn't have a default value")
// calls setRollbackOnly() on the shared transaction and takes the caller's
// work — an OPD bill settle, a discharge, a GRN, ... — down with it, even
// though AuditEventApplicationController.saveAuditEvent() catches the
// exception.
//
// REQUIRES_NEW suspends the caller's transaction and runs each audit
// operation in its own. If it fails, only that transaction rolls back; the
// caller's transaction is untouched and the caught exception is logged and
// ignored as intended. All non-write usage of this facade elsewhere is
// findByJpql (reads), for which running in a fresh short transaction is
// harmless.
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
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

    // create()/edit() are inherited from the generic AbstractFacade<T>. Per the
    // EJB spec a class-level @TransactionAttribute on this subclass applies only
    // to methods *defined here*, not to un-overridden inherited ones, and the
    // container dispatches writes through the synthetic bridge create(Object)/
    // edit(Object). Override both here so the REQUIRES_NEW attribute is
    // unambiguously in effect for the audit write path regardless of container
    // interpretation.

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
