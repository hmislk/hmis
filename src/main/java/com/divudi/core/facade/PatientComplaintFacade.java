package com.divudi.core.facade;

import com.divudi.core.entity.PatientComplaint;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class PatientComplaintFacade extends AbstractFacade<PatientComplaint> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PatientComplaintFacade() {
        super(PatientComplaint.class);
    }
}
