package com.divudi.core.facade;

import com.divudi.core.entity.process.ProcessStepInstance;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 *
 */
@Stateless
public class ProcessStepInstanceFacade extends AbstractFacade<ProcessStepInstance> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em; // Fixed the method to return 'em' directly without the empty if condition.
    }

    public ProcessStepInstanceFacade() {
        super(ProcessStepInstance.class);
    }

}
