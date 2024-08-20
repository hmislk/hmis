/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.facade;

import com.divudi.entity.channel.DoctorInstitution;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Sniper 619
 */
@Stateless
public class DoctorInstitutionFacade extends AbstractFacade<DoctorInstitution> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public DoctorInstitutionFacade() {
        super(DoctorInstitution.class);
    }
    
}
