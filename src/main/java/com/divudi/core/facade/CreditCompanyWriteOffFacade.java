package com.divudi.core.facade;

import com.divudi.core.entity.CreditCompanyWriteOff;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class CreditCompanyWriteOffFacade extends AbstractFacade<CreditCompanyWriteOff> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CreditCompanyWriteOffFacade() {
        super(CreditCompanyWriteOff.class);
    }
}
