package com.divudi.core.facade;

import com.divudi.core.entity.AppointmentScheduleTemplate;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AppointmentScheduleTemplateFacade extends AbstractFacade<AppointmentScheduleTemplate> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public AppointmentScheduleTemplateFacade() {
        super(AppointmentScheduleTemplate.class);
    }
}
