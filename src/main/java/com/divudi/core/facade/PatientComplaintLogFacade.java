package com.divudi.core.facade;

import com.divudi.core.entity.PatientComplaintLog;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class PatientComplaintLogFacade extends AbstractFacade<PatientComplaintLog> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PatientComplaintLogFacade() {
        super(PatientComplaintLog.class);
    }
}
