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
        if (em == null) {
        }
        return em;
    }

    public StockFacade() {
        super(Stock.class);
    }

    public List<BeforeStockTakingDTO> findBeforeStockTakingReport(String sql, Map<String, Object> parameters) {
        Query query = em.createQuery(sql);

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        return query.getResultList();
    }

}
