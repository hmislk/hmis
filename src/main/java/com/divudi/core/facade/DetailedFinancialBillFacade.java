/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.cashTransaction.DetailedFinancialBill;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Facade for managing DetailedFinancialBill entities.
 *
 * @author Buddhika
 */
@Stateless
public class DetailedFinancialBillFacade extends AbstractFacade<DetailedFinancialBill> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public DetailedFinancialBillFacade() {
        super(DetailedFinancialBill.class);
    }
}
