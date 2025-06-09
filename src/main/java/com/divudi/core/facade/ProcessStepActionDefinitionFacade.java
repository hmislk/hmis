/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.process.ProcessStepActionDefinition;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Facade class for ProcessStepActionDefinition entity operations.
 */
@Stateless
public class ProcessStepActionDefinitionFacade extends AbstractFacade<ProcessStepActionDefinition> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em; // Return the EntityManager
    }

    public ProcessStepActionDefinitionFacade() {
        super(ProcessStepActionDefinition.class);
    }
}
