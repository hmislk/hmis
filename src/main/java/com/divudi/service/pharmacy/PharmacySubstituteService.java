/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.StockFacade;
import com.divudi.ejb.PharmacyBean;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Shared, on-demand substitute-medicine (alternatives) logic for the three
 * pharmacy sale pages: Retail Sale ({@code PharmacySaleController} /
 * {@code RetailSaleNativeSqlController}), Fast Sale
 * ({@code PharmacyFastRetailSaleController}) and Sale for Cashier
 * ({@code PharmacyFastRetailSaleForCashierController}).
 *
 * <p>Alternative AMP resolution reuses {@link PharmacyBean#resolveAmps(Item)}
 * (exact AMP &rarr; same-strength sibling AMPs &rarr; VMP/VTM siblings); the
 * in-stock, non-expired stock lookup is done with <b>native SQL</b> (FEFO
 * ordered) per issue #21697 requirement 2, consistent with the native retail
 * sale performance path ({@code RetailSaleNativeSqlService}). EclipseLink
 * positional parameters ({@code ?1}, {@code ?2}&hellip;) are used and table
 * names are resolved case-insensitively via {@code INFORMATION_SCHEMA} so the
 * query works across customer DBs regardless of table-name case.
 *
 * Issue: #21697
 */
@Stateless
public class PharmacySubstituteService {

    private static final Logger LOGGER = Logger.getLogger(PharmacySubstituteService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @EJB
    private PharmacyBean pharmacyBean;

    @EJB
    private StockFacade stockFacade;

    private String tStock;
    private String tItemBatch;

    /**
     * Resolve in-stock, non-expired substitute stocks for the given item in the
     * given department, FEFO-ordered (earliest expiry first).
     *
     * @param item       the bill item's current item to find alternatives for
     * @param department the current department whose stock should be considered
     * @return list of {@link Stock} (loaded with item batch), never null
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Stock> findSubstituteStocks(Item item, Department department) {
        List<Stock> result = new ArrayList<>();
        if (item == null || department == null || department.getId() == null) {
            return result;
        }

        List<Amp> amps = pharmacyBean.resolveAmps(item);
        if (amps == null || amps.isEmpty()) {
            return result;
        }

        List<Long> ampIds = new ArrayList<>();
        for (Amp amp : amps) {
            if (amp != null && amp.getId() != null) {
                ampIds.add(amp.getId());
            }
        }
        if (ampIds.isEmpty()) {
            return result;
        }

        // Native SQL: in-stock (> 0), non-expired (expiry strictly in the
        // future), current department, item in resolved AMPs; FEFO ordered.
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT s.ID ")
                .append("FROM ").append(stockTable()).append(" s ")
                .append("JOIN ").append(itemBatchTable()).append(" ib ON s.itemBatch_ID = ib.ID ")
                .append("WHERE s.department_ID = ?1 ")
                .append("AND s.stock > 0 ")
                .append("AND ib.dateOfExpire > NOW() ")
                .append("AND ib.item_ID IN (");
        for (int i = 0; i < ampIds.size(); i++) {
            sql.append("?").append(i + 2);
            if (i < ampIds.size() - 1) {
                sql.append(",");
            }
        }
        sql.append(") ORDER BY ib.dateOfExpire ASC");

        javax.persistence.Query q = em.createNativeQuery(sql.toString());
        q.setParameter(1, department.getId());
        for (int i = 0; i < ampIds.size(); i++) {
            q.setParameter(i + 2, ampIds.get(i));
        }

        @SuppressWarnings("unchecked")
        List<Object> stockIdRows = q.getResultList();
        for (Object row : stockIdRows) {
            if (row == null) {
                continue;
            }
            Long stockId = ((Number) row).longValue();
            Stock loaded = stockFacade.findWithItemBatch(stockId);
            if (loaded != null) {
                result.add(loaded);
            }
        }
        return result;
    }

    /**
     * Swap the selected substitute stock into the bill item: updates item,
     * batch, stock and all rate + finance-detail fields. Bill totals must be
     * recalculated by the caller using that controller's own rate-recalc method
     * (each sale controller derives line values from
     * {@code pharmaceuticalBillItem.stock.itemBatch}).
     *
     * @param billItem        the bill item to update
     * @param substituteStock the chosen substitute stock
     * @return true if the swap succeeded
     */
    public boolean swapStockIntoBillItem(BillItem billItem, Stock substituteStock) {
        if (billItem == null || substituteStock == null || substituteStock.getItemBatch() == null) {
            return false;
        }
        ItemBatch itemBatch = substituteStock.getItemBatch();

        billItem.setItem(itemBatch.getItem());

        PharmaceuticalBillItem phItem = billItem.getPharmaceuticalBillItem();
        if (phItem == null) {
            phItem = new PharmaceuticalBillItem();
            phItem.setBillItem(billItem);
            billItem.setPharmaceuticalBillItem(phItem);
        }

        phItem.setStock(substituteStock);
        phItem.setItemBatch(itemBatch);
        phItem.setDoe(itemBatch.getDateOfExpire());
        // Do NOT "fix" the intentional typo purcahseRate (DB compatibility).
        phItem.setPurchaseRate(itemBatch.getPurcahseRate());
        phItem.setRetailRateInUnit(itemBatch.getRetailsaleRate());
        phItem.setPurchaseRatePack(itemBatch.getPurcahseRate());
        phItem.setRetailRatePack(itemBatch.getRetailsaleRate());
        phItem.setCostRate(itemBatch.getCostRate());
        phItem.setCostRatePack(itemBatch.getCostRate());

        BillItemFinanceDetails financeDetails = billItem.getBillItemFinanceDetails();
        if (financeDetails != null) {
            // Retail sale line value is at the retail (sale) rate.
            BigDecimal saleRate = BigDecimal.valueOf(itemBatch.getRetailsaleRate());
            financeDetails.setLineGrossRate(saleRate);
            financeDetails.setLineNetRate(saleRate);
            financeDetails.setLineCostRate(BigDecimal.valueOf(itemBatch.getCostRate()));
            financeDetails.setRetailSaleRate(saleRate);

            BigDecimal qty = financeDetails.getQuantity() != null ? financeDetails.getQuantity() : BigDecimal.ONE;
            financeDetails.setValueAtCostRate(BigDecimal.valueOf(itemBatch.getCostRate()).multiply(qty));
            financeDetails.setValueAtPurchaseRate(BigDecimal.valueOf(itemBatch.getPurcahseRate()).multiply(qty));
            financeDetails.setValueAtRetailRate(saleRate.multiply(qty));
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Case-insensitive table-name resolution (cross-deployment safety)
    // -----------------------------------------------------------------------

    private String resolveTable(String upperName) {
        Object name = em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = ?1 LIMIT 1")
                .setParameter(1, upperName)
                .getSingleResult();
        return name.toString();
    }

    private String stockTable() {
        if (tStock == null) {
            tStock = resolveTable("STOCK");
        }
        return tStock;
    }

    private String itemBatchTable() {
        if (tItemBatch == null) {
            tItemBatch = resolveTable("ITEMBATCH");
        }
        return tItemBatch;
    }
}
