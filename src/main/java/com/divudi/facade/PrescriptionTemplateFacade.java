package com.divudi.facade;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.divudi.entity.clinical.PrescriptionTemplate;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Buddhika
 */
@Stateless
public class PrescriptionTemplateFacade extends AbstractFacade<PrescriptionTemplate> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public PrescriptionTemplateFacade() {
        super(PrescriptionTemplate.class);
    }
    
}
