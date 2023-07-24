/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade.web;


import com.divudi.entity.web.WebTemplate;
import com.divudi.facade.AbstractFacade;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Stateless
public class WebTemplateFacade extends AbstractFacade<WebTemplate> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public WebTemplateFacade() {
        super(WebTemplate.class);
    }
    
}
