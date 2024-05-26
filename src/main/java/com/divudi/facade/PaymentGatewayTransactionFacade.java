/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;


import com.divudi.entity.PaymentGatewayTransaction;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author safrin
 */
@Stateless
public class PaymentGatewayTransactionFacade extends AbstractFacade<PaymentGatewayTransaction> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public PaymentGatewayTransactionFacade() {
        super(PaymentGatewayTransaction.class);
    }
    
}
