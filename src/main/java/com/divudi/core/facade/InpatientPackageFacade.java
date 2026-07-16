package com.divudi.core.facade;

import com.divudi.core.entity.inward.InpatientPackage;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class InpatientPackageFacade extends AbstractFacade<InpatientPackage> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public InpatientPackageFacade() {
        super(InpatientPackage.class);
    }
}
