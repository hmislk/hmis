package com.divudi.core.facade;

import com.divudi.core.entity.CreditCompanyClaim;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class CreditCompanyClaimFacade extends AbstractFacade<CreditCompanyClaim> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CreditCompanyClaimFacade() {
        super(CreditCompanyClaim.class);
    }
}
