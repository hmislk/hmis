/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;

import com.divudi.entity.channel.PatientAppointment;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author buddhika
 */
@Stateless
public class PatientAppointmentFacade extends AbstractFacade<PatientAppointment> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public PatientAppointmentFacade() {
        super(PatientAppointment.class);
    }

}
