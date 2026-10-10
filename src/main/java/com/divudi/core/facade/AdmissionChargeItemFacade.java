package com.divudi.core.facade;

import com.divudi.core.entity.inward.AdmissionChargeItem;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AdmissionChargeItemFacade extends AbstractFacade<AdmissionChargeItem> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public AdmissionChargeItemFacade() {
        super(AdmissionChargeItem.class);
    }
}
