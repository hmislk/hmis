/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.facade;

import com.divudi.core.entity.ConfigOption;
import java.util.Map;
import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Sniper 619
 */
@Stateless
public class ConfigOptionFacade extends AbstractFacade<ConfigOption> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public ConfigOptionFacade() {
        super(ConfigOption.class);
    }

    @PermitAll
    public ConfigOption findFirstByJpqlWithLock(String jpql, Map<String, Object> params) {
        javax.persistence.TypedQuery<ConfigOption> qry = getEntityManager().createQuery(jpql, ConfigOption.class);
        for (Map.Entry<String, Object> e : params.entrySet()) {
            qry.setParameter(e.getKey(), e.getValue());
        }
        qry.setMaxResults(1);
        qry.setLockMode(javax.persistence.LockModeType.PESSIMISTIC_WRITE);
        try {
            return qry.getSingleResult();
        } catch (javax.persistence.NoResultException ex) {
            return null;
        }
    }

}
