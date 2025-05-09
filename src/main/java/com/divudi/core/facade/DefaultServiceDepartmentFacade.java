/*
* Dr M H B Ariyaratne, Damith & ChatGPT contribution
 */
package com.divudi.core.facade;

import com.divudi.core.entity.DefaultServiceDepartment;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Buddhika, Damith & ChatGPT
 */
@Stateless
public class DefaultServiceDepartmentFacade extends AbstractFacade<DefaultServiceDepartment> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if (em == null) {
        }
        return em;
    }

    public DefaultServiceDepartmentFacade() {
        super(DefaultServiceDepartment.class);
    }

}
