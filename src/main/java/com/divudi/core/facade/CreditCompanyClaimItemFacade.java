package com.divudi.core.facade;

import com.divudi.core.entity.CreditCompanyClaimItem;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class CreditCompanyClaimItemFacade extends AbstractFacade<CreditCompanyClaimItem> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CreditCompanyClaimItemFacade() {
        super(CreditCompanyClaimItem.class);
    }
}
