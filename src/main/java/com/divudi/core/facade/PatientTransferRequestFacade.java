package com.divudi.core.facade;

import com.divudi.core.entity.inward.PatientTransferRequest;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Stateless
public class PatientTransferRequestFacade extends AbstractFacade<PatientTransferRequest> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PatientTransferRequestFacade() {
        super(PatientTransferRequest.class);
    }
}
