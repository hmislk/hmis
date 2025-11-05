/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.entity.BillExpense;
import com.divudi.core.facade.AbstractFacade;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author pdhs
 */
@Stateless
public class BillExpenseFacade extends AbstractFacade<BillExpense> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        // EntityManager is injected by the container. If this is null, check
        // the persistence context configuration.
        return em;
    }

    public BillExpenseFacade() {
        super(BillExpense.class);
    }

}
