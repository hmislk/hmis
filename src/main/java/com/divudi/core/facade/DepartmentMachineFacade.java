/*
* Dr M H B Ariyaratne
* buddhika.ari@gmail.com
*/
package com.divudi.core.facade;


import com.divudi.core.entity.lab.DepartmentMachine;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Buddhika
 */
@Stateless
public class DepartmentMachineFacade extends AbstractFacade<DepartmentMachine> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public DepartmentMachineFacade() {
        super(DepartmentMachine.class);
    }

}
