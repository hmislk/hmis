/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.facade;

import com.divudi.core.entity.pharmacy.StockHistory;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TemporalType;

/**
 *
 * @author ruhunu
 */
@Stateless
public class StockHistoryFacade extends AbstractFacade<StockHistory> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public StockHistoryFacade() {
        super(StockHistory.class);
    }

    /**
     * Calculates the stock value at retail rate for a given date and department.
     * This method uses a simplified native SQL query for better performance.
     *
     * Performance improvements:
     * 1. Uses native SQL instead of complex JPQL with nested subqueries
     * 2. Leverages database indexing more efficiently
     * 3. Reduces the number of subquery levels
     *
     * @param date The date for which to calculate stock value
     * @param departmentId The department ID for which to calculate stock value (can be null for all departments)
     * @return The total stock value at retail rate, or 0.0 if calculation fails
     */
    public double calculateStockValueAtRetailRateOptimized(Date date, Long departmentId) {
        try {
            // Use native SQL for better performance
            // This query finds the latest stock history record for each item batch before the given date
            // and calculates the total retail value
            String sql =
                "SELECT COALESCE(SUM(latest_stock.stock_qty * latest_stock.retail_rate), 0.0) AS total_value " +
                "FROM ( " +
                "    SELECT  " +
                "        sh.stock_qty, " +
                "        COALESCE(ib.retailsale_rate, 0.0) AS retail_rate " +
                "    FROM stock_history sh " +
                "    INNER JOIN ( " +
                "        SELECT  " +
                "            department_id, " +
                "            item_batch_id, " +
                "            MAX(id) AS max_id " +
                "        FROM stock_history " +
                "        WHERE retired = 0 " +
                "        AND created_at < ? " +
                (departmentId != null ? "        AND department_id = ? " : "") +
                "        GROUP BY department_id, item_batch_id " +
                "    ) AS latest ON sh.id = latest.max_id " +
                "    INNER JOIN item_batch ib ON sh.item_batch_id = ib.id " +
                "    WHERE sh.retired = 0 " +
                "    AND sh.stock_qty > 0 " +
                ") AS latest_stock";

            // Execute the native query
            javax.persistence.Query query = getEntityManager().createNativeQuery(sql);
            query.setParameter(1, date, TemporalType.TIMESTAMP);
            if (departmentId != null) {
                query.setParameter(2, departmentId);
            }

            Object result = query.getSingleResult();
            if (result != null) {
                if (result instanceof Number) {
                    return ((Number) result).doubleValue();
                }
            }
            return 0.0;

        } catch (Exception e) {
            System.err.println("Error calculating stock value at retail rate for date: " + date + " - " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * Alternative optimized method using a two-step approach.
     * This can be even faster for very large datasets as it breaks the complex query into simpler parts.
     *
     * @param date The date for which to calculate stock value
     * @param departmentId The department ID for which to calculate stock value (can be null for all departments)
     * @return The total stock value at retail rate, or 0.0 if calculation fails
     */
    public double calculateStockValueAtRetailRateTwoStep(Date date, Long departmentId) {
        try {
            // Step 1: Get the latest stock history IDs using a simplified query
            String findLatestIdsSql =
                "SELECT MAX(id) AS max_id " +
                "FROM stock_history " +
                "WHERE retired = 0 " +
                "AND created_at < ? " +
                (departmentId != null ? "AND department_id = ? " : "") +
                "GROUP BY department_id, item_batch_id";

            javax.persistence.Query idsQuery = getEntityManager().createNativeQuery(findLatestIdsSql);
            idsQuery.setParameter(1, date, TemporalType.TIMESTAMP);
            if (departmentId != null) {
                idsQuery.setParameter(2, departmentId);
            }

            List<Object> latestIds = idsQuery.getResultList();

            if (latestIds == null || latestIds.isEmpty()) {
                return 0.0;
            }

            // Step 2: Calculate the sum of stock values for the latest records
            String calculateValueSql =
                "SELECT COALESCE(SUM(sh.stock_qty * COALESCE(ib.retailsale_rate, 0.0)), 0.0) " +
                "FROM stock_history sh " +
                "INNER JOIN item_batch ib ON sh.item_batch_id = ib.id " +
                "WHERE sh.id IN (:ids) " +
                "AND sh.retired = 0 " +
                "AND sh.stock_qty > 0";

            javax.persistence.Query valueQuery = getEntityManager().createNativeQuery(calculateValueSql);
            valueQuery.setParameter("ids", latestIds);

            Object result = valueQuery.getSingleResult();
            if (result != null) {
                if (result instanceof Number) {
                    return ((Number) result).doubleValue();
                }
            }
            return 0.0;

        } catch (Exception e) {
            System.err.println("Error calculating stock value (two-step method) for date: " + date + " - " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

}
