package com.divudi.core.facade;

import com.divudi.core.entity.AppointmentScheduleDateOverride;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AppointmentScheduleDateOverrideFacade extends AbstractFacade<AppointmentScheduleDateOverride> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if (em == null) {
        }
        return em;
    }

    public AppointmentScheduleDateOverrideFacade() {
        super(AppointmentScheduleDateOverride.class);
    }
}
