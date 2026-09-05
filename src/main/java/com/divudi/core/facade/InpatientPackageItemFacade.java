package com.divudi.core.facade;

import com.divudi.core.entity.inward.InpatientPackageItem;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class InpatientPackageItemFacade extends AbstractFacade<InpatientPackageItem> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public InpatientPackageItemFacade() {
        super(InpatientPackageItem.class);
    }
}
