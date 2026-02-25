/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.facade;

import com.divudi.core.data.HistoryType;
import com.divudi.core.data.dto.StockHistoryDTO;
import com.divudi.core.entity.pharmacy.StockHistory;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TemporalType;
import java.util.Calendar;

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
        System.out.println("=== calculateStockValueAtRetailRateOptimized START ===");
        System.out.println("Input Date: " + date);

        try {
            // Use native SQL for better performance
            // This query finds the latest stock history record for each item batch before the given date
            // and calculates the total retail value
            String sql =
                "SELECT COALESCE(SUM(latest_stock.STOCKQTY * latest_stock.retail_rate), 0.0) AS total_value " +
                "FROM ( " +
                "    SELECT  " +
                "        sh.STOCKQTY, " +
                "        COALESCE(ib.RETAILSALERATE, 0.0) AS retail_rate " +
                "    FROM STOCKHISTORY sh " +
                "    INNER JOIN ( " +
                "        SELECT  " +
                "            DEPARTMENT_ID, " +
                "            ITEMBATCH_ID, " +
                "            MAX(ID) AS max_id " +
                "        FROM STOCKHISTORY " +
                "        WHERE RETIRED = 0 " +
                "        AND CREATEDAT < ? " +
                (departmentId != null ? "        AND DEPARTMENT_ID = ? " : "") +
                "        GROUP BY DEPARTMENT_ID, ITEMBATCH_ID " +
                "    ) AS latest ON sh.ID = latest.max_id " +
                "    INNER JOIN ITEMBATCH ib ON sh.ITEMBATCH_ID = ib.ID " +
                "    WHERE sh.RETIRED = 0 " +
                "    AND sh.STOCKQTY > 0 " +
                ") AS latest_stock";

            System.out.println("SQL Query: " + sql);

            // Execute the native query
            javax.persistence.Query query = getEntityManager().createNativeQuery(sql);
            query.setParameter(1, date, TemporalType.TIMESTAMP);
            System.out.println("Parameter 1 (date): " + date);

            if (departmentId != null) {
                query.setParameter(2, departmentId);
            } else {
            }

            System.out.println("Executing query...");
            Object result = query.getSingleResult();
            System.out.println("Raw result: " + result);
            System.out.println("Result class: " + (result != null ? result.getClass().getName() : "null"));

            if (result != null) {
                if (result instanceof Number) {
                    double value = ((Number) result).doubleValue();
                    System.out.println("Returning value: " + value);
                    return value;
                }
            }
            System.out.println("Result is null or not a Number, returning 0.0");
            return 0.0;

        } catch (Exception e) {
            System.err.println("=== EXCEPTION in calculateStockValueAtRetailRateOptimized ===");
            System.err.println("Error calculating stock value at retail rate for date: " + date + " - " + e.getMessage());
            System.err.println("Exception class: " + e.getClass().getName());
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
                "SELECT MAX(ID) AS max_id " +
                "FROM STOCKHISTORY " +
                "WHERE RETIRED = 0 " +
                "AND CREATEDAT < ? " +
                (departmentId != null ? "AND DEPARTMENT_ID = ? " : "") +
                "GROUP BY DEPARTMENT_ID, ITEMBATCH_ID";

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
                "SELECT COALESCE(SUM(sh.STOCKQTY * COALESCE(ib.RETAILSALERATE, 0.0)), 0.0) " +
                "FROM STOCKHISTORY sh " +
                "INNER JOIN ITEMBATCH ib ON sh.ITEMBATCH_ID = ib.ID " +
                "WHERE sh.ID IN (:ids) " +
                "AND sh.RETIRED = 0 " +
                "AND sh.STOCKQTY > 0";

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
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * Calculates the stock value at cost rate for a given date and department.
     * This method uses a simplified native SQL query for better performance.
     *
     * Cost rate calculation logic mirrors ItemBatch.getCostRate():
     * - Use costRate if it's not null and not 0
     * - Otherwise fallback to purcahseRate if it's not null and not 0
     * - Otherwise use 0.0
     *
     * Performance improvements:
     * 1. Uses native SQL instead of complex JPQL with nested subqueries
     * 2. Leverages database indexing more efficiently
     * 3. Reduces the number of subquery levels
     *
     * @param date The date for which to calculate stock value
     * @param departmentId The department ID for which to calculate stock value (can be null for all departments)
     * @return The total stock value at cost rate, or 0.0 if calculation fails
     */
    public double calculateStockValueAtCostRateOptimized(Date date, Long departmentId) {
        System.out.println("=== calculateStockValueAtCostRateOptimized START ===");
        System.out.println("Input Date: " + date);

        try {
            // Use native SQL for better performance
            // This query finds the latest stock history record for each item batch before the given date
            // and calculates the total cost value using the cost rate fallback logic
            String sql =
                "SELECT COALESCE(SUM(latest_stock.STOCKQTY * latest_stock.cost_rate), 0.0) AS total_value " +
                "FROM ( " +
                "    SELECT  " +
                "        sh.STOCKQTY, " +
                "        CASE " +
                "            WHEN ib.COSTRATE IS NOT NULL AND ib.COSTRATE != 0 THEN ib.COSTRATE " +
                "            WHEN ib.PURCAHSERATE IS NOT NULL AND ib.PURCAHSERATE != 0 THEN ib.PURCAHSERATE " +
                "            ELSE 0.0 " +
                "        END AS cost_rate " +
                "    FROM STOCKHISTORY sh " +
                "    INNER JOIN ( " +
                "        SELECT  " +
                "            DEPARTMENT_ID, " +
                "            ITEMBATCH_ID, " +
                "            MAX(ID) AS max_id " +
                "        FROM STOCKHISTORY " +
                "        WHERE RETIRED = 0 " +
                "        AND CREATEDAT < ? " +
                (departmentId != null ? "        AND DEPARTMENT_ID = ? " : "") +
                "        GROUP BY DEPARTMENT_ID, ITEMBATCH_ID " +
                "    ) AS latest ON sh.ID = latest.max_id " +
                "    INNER JOIN ITEMBATCH ib ON sh.ITEMBATCH_ID = ib.ID " +
                "    WHERE sh.RETIRED = 0 " +
                "    AND sh.STOCKQTY > 0 " +
                ") AS latest_stock";

            System.out.println("SQL Query: " + sql);

            // Execute the native query
            javax.persistence.Query query = getEntityManager().createNativeQuery(sql);
            query.setParameter(1, date, TemporalType.TIMESTAMP);
            System.out.println("Parameter 1 (date): " + date);

            if (departmentId != null) {
                query.setParameter(2, departmentId);
            } else {
            }

            System.out.println("Executing query...");
            Object result = query.getSingleResult();
            System.out.println("Raw result: " + result);
            System.out.println("Result class: " + (result != null ? result.getClass().getName() : "null"));

            if (result != null) {
                if (result instanceof Number) {
                    double value = ((Number) result).doubleValue();
                    System.out.println("Returning value: " + value);
                    return value;
                }
            }
            System.out.println("Result is null or not a Number, returning 0.0");
            return 0.0;

        } catch (Exception e) {
            System.err.println("=== EXCEPTION in calculateStockValueAtCostRateOptimized ===");
            System.err.println("Error calculating stock value at cost rate for date: " + date + " - " + e.getMessage());
            System.err.println("Exception class: " + e.getClass().getName());
            e.printStackTrace();
            return 0.0;
        }
    }


    public List<StockHistoryDTO> findStockHistoryDtos(Long itemId, Long departmentId, Long billId, Date fromDate, Date toDate,
            HistoryType historyType, int maxResults) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dto.StockHistoryDTO(")
                .append("sh.id, sh.createdAt, sh.stockAt, sh.historyType, ")
                .append("item.id, item.name, ib.batchNo, ib.dateOfExpire, ")
                .append("dept.id, dept.name, ")
                .append("bill.id, bill.deptId, bill.billTypeAtomic, pb.qty, ")
                .append("sh.stockQty, sh.instituionBatchQty, sh.totalBatchQty, ")
                .append("sh.itemStock, sh.institutionItemStock, sh.totalItemStock, ")
                .append("sh.retailRate, sh.purchaseRate, sh.costRate, ")
                .append("sh.stockSaleValue, sh.stockPurchaseValue, sh.stockCostValue, ")
                .append("sh.itemStockValueAtSaleRate, sh.itemStockValueAtPurchaseRate, sh.itemStockValueAtCostRate, ")
                .append("sh.institutionBatchStockValueAtSaleRate, sh.institutionBatchStockValueAtPurchaseRate, sh.institutionBatchStockValueAtCostRate, ")
                .append("sh.totalBatchStockValueAtSaleRate, sh.totalBatchStockValueAtPurchaseRate, sh.totalBatchStockValueAtCostRate")
                .append(") ")
                .append("FROM StockHistory sh ")
                .append("LEFT JOIN sh.item item ")
                .append("LEFT JOIN sh.itemBatch ib ")
                .append("LEFT JOIN sh.department dept ")
                .append("LEFT JOIN sh.pbItem pb ")
                .append("LEFT JOIN pb.billItem bi ")
                .append("LEFT JOIN bi.bill bill ")
                .append("WHERE sh.retired = false ");

        if (itemId != null) {
            jpql.append("AND item.id = :itemId ");
        }
        if (departmentId != null) {
            jpql.append("AND dept.id = :departmentId ");
        }
        if (billId != null) {
            jpql.append("AND bill.id = :billId ");
        }
        if (fromDate != null) {
            jpql.append("AND sh.createdAt >= :fromDate ");
        }
        if (toDate != null) {
            jpql.append("AND sh.createdAt < :toDate ");
        }
        if (historyType != null) {
            jpql.append("AND sh.historyType = :historyType ");
        }

        jpql.append("ORDER BY sh.id DESC");

        javax.persistence.TypedQuery<StockHistoryDTO> query = getEntityManager().createQuery(jpql.toString(), StockHistoryDTO.class);

        if (itemId != null) {
            query.setParameter("itemId", itemId);
        }
        if (departmentId != null) {
            query.setParameter("departmentId", departmentId);
        }
        if (billId != null) {
            query.setParameter("billId", billId);
        }
        if (fromDate != null) {
            query.setParameter("fromDate", fromDate, TemporalType.TIMESTAMP);
        }
        if (toDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(toDate);
            cal.add(Calendar.SECOND, 1);
            query.setParameter("toDate", cal.getTime(), TemporalType.TIMESTAMP);
        }
        if (historyType != null) {
            query.setParameter("historyType", historyType);
        }

        query.setMaxResults(maxResults);
        return query.getResultList();
    }

}
