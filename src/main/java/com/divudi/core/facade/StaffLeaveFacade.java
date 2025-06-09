/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.facade;

import com.divudi.core.entity.hr.StaffLeave;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author safrin
 */
@Stateless
public class StaffLeaveFacade extends AbstractFacade<StaffLeave> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em==null){

        }
        if(em == null){}return em;
    }

    public StaffLeaveFacade() {
        super(StaffLeave.class);
    }

}
