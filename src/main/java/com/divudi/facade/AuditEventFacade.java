/*
 * author Senula Nanayakkara
 */

package com.divudi.facade;

import com.divudi.entity.AuditEvent;
import javax.ejb.Stateless;
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
    
}
