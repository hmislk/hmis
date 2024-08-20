/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;

import com.divudi.entity.ServiceSessionInstance;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Stateless
public class ServiceSessionInstanceFacade extends AbstractFacade<ServiceSessionInstance> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if (em == null) {
        }
        return em;
    }

    public ServiceSessionInstanceFacade() {
        super(ServiceSessionInstance.class);
    }

}
