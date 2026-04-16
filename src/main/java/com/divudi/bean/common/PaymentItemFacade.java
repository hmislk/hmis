/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.entity.PaymentItem;
import com.divudi.core.facade.AbstractFacade;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author buddhika
 */
@Stateless
public class PaymentItemFacade extends AbstractFacade<PaymentItem> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        // EntityManager is injected by the container. If this is null, check
        // the persistence context configuration.
        return em;
    }

    public PaymentItemFacade() {
        super(PaymentItem.class);
    }

}
