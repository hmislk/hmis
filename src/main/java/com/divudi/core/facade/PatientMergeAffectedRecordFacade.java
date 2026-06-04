package com.divudi.core.facade;

import com.divudi.core.entity.PatientMergeAffectedRecord;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class PatientMergeAffectedRecordFacade extends AbstractFacade<PatientMergeAffectedRecord> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if (em == null) {
        }
        return em;
    }

    public PatientMergeAffectedRecordFacade() {
        super(PatientMergeAffectedRecord.class);
    }
}
