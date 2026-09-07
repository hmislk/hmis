/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.inward.PatientFormEntry;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Stateless
public class PatientFormEntryFacade extends AbstractFacade<PatientFormEntry> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PatientFormEntryFacade() {
        super(PatientFormEntry.class);
    }
}
