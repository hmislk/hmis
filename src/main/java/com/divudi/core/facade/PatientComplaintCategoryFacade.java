package com.divudi.core.facade;

import com.divudi.core.entity.PatientComplaintCategory;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class PatientComplaintCategoryFacade extends AbstractFacade<PatientComplaintCategory> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PatientComplaintCategoryFacade() {
        super(PatientComplaintCategory.class);
    }
}
