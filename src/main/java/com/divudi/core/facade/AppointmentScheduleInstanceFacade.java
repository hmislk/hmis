package com.divudi.core.facade;

import com.divudi.core.entity.AppointmentScheduleInstance;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AppointmentScheduleInstanceFacade extends AbstractFacade<AppointmentScheduleInstance> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public AppointmentScheduleInstanceFacade() {
        super(AppointmentScheduleInstance.class);
    }
}
