package com.divudi.core.facade;

import com.divudi.core.entity.membership.RestrictedPaymentMethod;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Stateless
public class RestrictedPaymentMethodFacade extends AbstractFacade<RestrictedPaymentMethod> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public RestrictedPaymentMethodFacade() {
        super(RestrictedPaymentMethod.class);
    }

}
