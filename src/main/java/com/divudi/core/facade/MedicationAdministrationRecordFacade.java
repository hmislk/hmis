package com.divudi.core.facade;

import com.divudi.core.entity.clinical.MedicationAdministrationRecord;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Stateless
public class MedicationAdministrationRecordFacade extends AbstractFacade<MedicationAdministrationRecord> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public MedicationAdministrationRecordFacade() {
        super(MedicationAdministrationRecord.class);
    }

}
