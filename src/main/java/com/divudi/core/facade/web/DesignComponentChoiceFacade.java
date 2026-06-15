package com.divudi.core.facade.web;

import com.divudi.core.entity.web.DesignComponentChoice;
import com.divudi.core.facade.AbstractFacade;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class DesignComponentChoiceFacade extends AbstractFacade<DesignComponentChoice> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public DesignComponentChoiceFacade() {
        super(DesignComponentChoice.class);
    }
}
