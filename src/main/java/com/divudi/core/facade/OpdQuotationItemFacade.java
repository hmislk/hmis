package com.divudi.core.facade;

import com.divudi.core.entity.OpdQuotationItem;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class OpdQuotationItemFacade extends AbstractFacade<OpdQuotationItem> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public OpdQuotationItemFacade() {
        super(OpdQuotationItem.class);
    }
}
