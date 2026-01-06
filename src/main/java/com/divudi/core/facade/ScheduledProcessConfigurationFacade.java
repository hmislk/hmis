package com.divudi.core.facade;

import com.divudi.core.entity.ScheduledProcessConfiguration;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class ScheduledProcessConfigurationFacade extends AbstractFacade<ScheduledProcessConfiguration> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}
        return em;
    }

    public ScheduledProcessConfigurationFacade() {
        super(ScheduledProcessConfiguration.class);
    }
}
