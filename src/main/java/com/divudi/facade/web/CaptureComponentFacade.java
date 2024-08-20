/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade.web;


import com.divudi.entity.web.CaptureComponent;
import com.divudi.facade.AbstractFacade;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Senula Nanayakkara
 */
@Stateless
public class CaptureComponentFacade extends AbstractFacade<CaptureComponent> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public CaptureComponentFacade() {
        super(CaptureComponent.class);
    }
    
}
