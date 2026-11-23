package com.divudi.core.facade;

import com.divudi.core.entity.OpdQuotation;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class OpdQuotationFacade extends AbstractFacade<OpdQuotation> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public OpdQuotationFacade() {
        super(OpdQuotation.class);
    }
}
