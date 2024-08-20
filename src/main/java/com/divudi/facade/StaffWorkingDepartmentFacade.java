/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;

import com.divudi.entity.hr.StaffWorkingDepartment;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author safrin
 */
@Stateless
public class StaffWorkingDepartmentFacade extends AbstractFacade<StaffWorkingDepartment> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public StaffWorkingDepartmentFacade() {
        super(StaffWorkingDepartment.class);
    }
    
}
