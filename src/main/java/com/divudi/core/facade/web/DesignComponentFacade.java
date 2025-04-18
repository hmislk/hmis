/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade.web;


import com.divudi.core.entity.web.DesignComponent;
import com.divudi.core.facade.AbstractFacade;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Senula Nanayakkara
 */
@Stateless
public class DesignComponentFacade extends AbstractFacade<DesignComponent> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public DesignComponentFacade() {
        super(DesignComponent.class);
    }

}
