/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.pharmacy.Stock;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.divudi.core.data.dto.BeforeStockTakingDTO;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 *
 * @author safrin
 */
@Stateless
public class StockFacade extends AbstractFacade<Stock> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public StockFacade() {
        super(Stock.class);
    }

    /**
     * Finds a Stock by id with its itemBatch association force-loaded via
     * JOIN FETCH so the returned entity carries a non-null itemBatch even
     * after the persistence context closes (EclipseLink weaving issue).
     */
    public Stock findWithItemBatch(Long id) {
        if (id == null) {
            return null;
        }
        try {
            return em.createQuery(
                    "SELECT s FROM Stock s LEFT JOIN FETCH s.itemBatch WHERE s.id = :id",
                    Stock.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<BeforeStockTakingDTO> findBeforeStockTakingReport(String jpql, Map<String, Object> parameters) {
        Query query = em.createQuery(jpql);

        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }
        }

        return query.getResultList();
    }

}
