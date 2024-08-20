/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.facade;

import com.divudi.entity.lab.PatientSample;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author buddhika
 */
@Stateless
public class PatientSampleFacade extends AbstractFacade<PatientSample> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public PatientSampleFacade() {
        super(PatientSample.class);
    }

    public Long bhtBySql(String sql) {
        Query q = getEntityManager().createQuery(sql);
        if (q.getSingleResult() != null) {
            return (Long) q.getSingleResult();
        } else {
            return 0L;
        }
    }

}
