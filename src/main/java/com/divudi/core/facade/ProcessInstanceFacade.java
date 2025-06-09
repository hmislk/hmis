package com.divudi.core.facade;

import com.divudi.core.entity.process.ProcessInstance;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 *
 */
@Stateless
public class ProcessInstanceFacade extends AbstractFacade<ProcessInstance> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em; // Fixed the method to return 'em' directly without the empty if condition.
    }

    public ProcessInstanceFacade() {
        super(ProcessInstance.class);
    }

}
