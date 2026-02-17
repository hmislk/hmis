/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.analytics.AggregatedRecord;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author buddhika
 */
@Stateless
public class AggregatedRecordFacade extends AbstractFacade<AggregatedRecord> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if (em == null) {
        }
        return em;
    }

    public AggregatedRecordFacade() {
        super(AggregatedRecord.class);
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
