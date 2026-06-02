package com.divudi.core.facade;

import com.divudi.core.entity.PatientInsurance;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Stateless
public class PatientInsuranceFacade extends AbstractFacade<PatientInsurance> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PatientInsuranceFacade() {
        super(PatientInsurance.class);
    }
}
