package com.divudi.core.facade;

import com.divudi.core.entity.CreditCompanyClaimSettlement;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class CreditCompanyClaimSettlementFacade extends AbstractFacade<CreditCompanyClaimSettlement> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CreditCompanyClaimSettlementFacade() {
        super(CreditCompanyClaimSettlement.class);
    }
}
