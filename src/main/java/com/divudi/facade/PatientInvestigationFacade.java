/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;

import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.bean.common.util.JsfUtil;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Buddhika
 */
@Stateless
public class PatientInvestigationFacade extends AbstractFacade<PatientInvestigation> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){
            JsfUtil.addErrorMessage("null em");
        }
        if(em == null){}return em;
    }

    public PatientInvestigationFacade() {
        super(PatientInvestigation.class);
    }
    
}
