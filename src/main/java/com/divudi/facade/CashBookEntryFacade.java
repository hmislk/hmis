/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;


import com.divudi.entity.cashTransaction.CashBookEntry;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Buddhika
 */
@Stateless
public class CashBookEntryFacade extends AbstractFacade<CashBookEntry> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public CashBookEntryFacade() {
        super(CashBookEntry.class);
    }
    
}
