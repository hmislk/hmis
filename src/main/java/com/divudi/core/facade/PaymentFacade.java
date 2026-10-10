/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.facade;

import com.divudi.core.entity.Payment;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;

/**
 *
 * @author Sniper 619
 */
@Stateless
public class PaymentFacade extends AbstractFacade<Payment> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PaymentFacade() {
        super(Payment.class);
    }

    /**
     * Runs a JPQL constructor-DTO query and lets any query failure propagate,
     * unlike findLightsByJpql, which logs it and returns an empty list. For
     * reports that must show an error instead of a silently empty result.
     */
    public List<?> findDtosByJpqlOrThrow(String jpql, Map<String, Object> parameters, TemporalType tt) {
        Query qry = em.createQuery(jpql);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Date) {
                qry.setParameter(entry.getKey(), (Date) value, tt);
            } else {
                qry.setParameter(entry.getKey(), value);
            }
        }
        return qry.getResultList();
    }

}
