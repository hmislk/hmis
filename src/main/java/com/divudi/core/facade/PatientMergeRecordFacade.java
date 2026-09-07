package com.divudi.core.facade;

import com.divudi.core.entity.PatientMergeRecord;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class PatientMergeRecordFacade extends AbstractFacade<PatientMergeRecord> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if (em == null) {
        }
        return em;
    }

    public PatientMergeRecordFacade() {
        super(PatientMergeRecord.class);
    }
}
