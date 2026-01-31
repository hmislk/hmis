/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.Item;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Facade for audit database operations including schema management.
 * This facade connects to the audit database for database administration tasks.
 *
 * @author buddhika
 */
@Stateless
public class AuditDatabaseFacade extends AbstractFacade<Item> {

    @PersistenceContext(unitName = "hmisAuditPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}
        return em;
    }

    public AuditDatabaseFacade() {
        super(Item.class);
    }
}