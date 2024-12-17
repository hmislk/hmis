package com.divudi.facade;

import com.divudi.entity.workflow.ProcessDefinition;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 *
 */
@Stateless
public class ProcessDefinitionFacade extends AbstractFacade<ProcessDefinition> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public ProcessDefinitionFacade() {
        super(ProcessDefinition.class);
    }
    
}
