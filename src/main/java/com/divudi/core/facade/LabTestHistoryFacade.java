/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade.lab;

import com.divudi.core.entity.lab.LabTestHistory;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.divudi.core.facade.AbstractFacade;

/**
 * ChatGPT contributed
 */
@Stateless
public class LabTestHistoryFacade extends AbstractFacade<LabTestHistory> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public LabTestHistoryFacade() {
        super(LabTestHistory.class);
    }
}
