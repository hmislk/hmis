package com.divudi.core.facade;

import com.divudi.core.entity.inward.AdmissionCancellationReason;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AdmissionCancellationReasonFacade extends AbstractFacade<AdmissionCancellationReason> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public AdmissionCancellationReasonFacade() {
        super(AdmissionCancellationReason.class);
    }
}
