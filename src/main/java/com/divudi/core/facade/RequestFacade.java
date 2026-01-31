package com.divudi.core.facade;

import com.divudi.core.entity.Request;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

@Stateless
public class RequestFacade extends AbstractFacade<Request> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public RequestFacade() {
        super(Request.class);
    }

}
