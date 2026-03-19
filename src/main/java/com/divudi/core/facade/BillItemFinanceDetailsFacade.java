/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.BillItemFinanceDetails;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class BillItemFinanceDetailsFacade extends AbstractFacade<BillItemFinanceDetails> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if (em == null) {
        }
        return em;
    }

    public BillItemFinanceDetailsFacade() {
        super(BillItemFinanceDetails.class);
    }
}
