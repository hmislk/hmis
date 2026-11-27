package com.divudi.core.facade;

import com.divudi.core.entity.inward.AdmissionTypeHistory;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AdmissionTypeHistoryFacade extends AbstractFacade<AdmissionTypeHistory> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public AdmissionTypeHistoryFacade() {
        super(AdmissionTypeHistory.class);
    }
}
