/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.ejb.PharmacyBean;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
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
 * <p>Alternative AMP resolution uses
 * {@link PharmacyBean#resolveSubstituteAmps(Item)} (exact AMP &rarr;
 * same-VMP sibling brands; VMP/VMPP/VTM/ATM &rarr; every AMP under the node).
 * This is intentionally distinct from {@code PharmacyBean.resolveAmps}, which
 * stays brand-exact for prescription / dispensing flows. The
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

    @EJB
    private ItemBatchFacade itemBatchFacade;

    private String tStock;
    private String tItemBatch;
    private String tItem;

    /**
     * Resolve in-stock, non-expired substitute stocks for the given item in the
     * given department, FEFO-ordered (earliest expiry first).
     *
     * <p>Performance: the lookup is a <b>single</b> native-SQL round-trip that
     * selects every display + swap field (item name/code, rates, batch, expiry,
     * stock) directly into {@link StockDTO}. This avoids the previous N+1 of one
     * {@code findWithItemBatch} per result row, keeping the on-demand click fast
     * even when an AMP has many sibling brands across many batches.
     *
     * @param item        the bill item's current item to find alternatives for
     * @param department  the current department whose stock should be considered
     * @param requiredQty the bill-item quantity that must be covered; batches
     *                    with less stock than this are excluded so the cashier
     *                    can never pick an under-stocked substitute. Values
     *                    {@code <= 0} fall back to requiring any positive stock.
     * @return list of {@link StockDTO}, FEFO-ordered, never null
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<StockDTO> findSubstituteStocks(Item item, Department department, double requiredQty) {
        List<StockDTO> result = new ArrayList<>();
        if (item == null || department == null || department.getId() == null) {
            return result;
        }

        List<Amp> amps = pharmacyBean.resolveSubstituteAmps(item);
        if (amps == null || amps.isEmpty()) {
            return result;
        }

        // Never offer a batch that cannot cover the existing line quantity. The
        // normal add/edit paths already guard qty > stock; mirror that here so a
        // selected substitute is always valid for the current line. Default to
        // any positive stock when the caller has no meaningful quantity.
        double minStock = requiredQty > 0 ? requiredQty : 0d;

        List<Long> ampIds = new ArrayList<>();
        for (Amp amp : amps) {
            if (amp != null && amp.getId() != null) {
                ampIds.add(amp.getId());
            }
        }
        if (ampIds.isEmpty()) {
            return result;
        }

        // Native SQL: stock covering the line qty, non-expired (expiry strictly
        // in the future), current department, item in resolved AMPs, and not
        // retired (Stock, ItemBatch or Item) — retired rows are admin
        // soft-deletes and must never be offered as substitutes. FEFO ordered.
        // All display + swap fields are returned in one round-trip.
        // Params: ?1 = department, ?2 = min stock, ?3.. = AMP ids.
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT s.ID, s.itemBatch_ID, ib.item_ID, i.name, i.code, ")
                .append("ib.retailsaleRate, s.stock, ib.dateOfExpire, ib.batchNo, ")
                .append("ib.purcahseRate, ib.costRate, ib.wholesaleRate ")
                .append("FROM ").append(stockTable()).append(" s ")
                .append("JOIN ").append(itemBatchTable()).append(" ib ON s.itemBatch_ID = ib.ID ")
                .append("JOIN ").append(itemTable()).append(" i ON ib.item_ID = i.ID ")
                .append("WHERE s.department_ID = ?1 ")
                .append("AND s.stock >= ?2 AND s.stock > 0 ")
                .append("AND ib.dateOfExpire > NOW() ")
                .append("AND (s.retired = 0 OR s.retired IS NULL) ")
                .append("AND (ib.retired = 0 OR ib.retired IS NULL) ")
                .append("AND (i.retired = 0 OR i.retired IS NULL) ")
                .append("AND ib.item_ID IN (");
        for (int i = 0; i < ampIds.size(); i++) {
            sql.append("?").append(i + 3);
            if (i < ampIds.size() - 1) {
                sql.append(",");
            }
        }
        sql.append(") ORDER BY ib.dateOfExpire ASC");

        javax.persistence.Query q = em.createNativeQuery(sql.toString());
        q.setParameter(1, department.getId());
        q.setParameter(2, minStock);
        for (int i = 0; i < ampIds.size(); i++) {
            q.setParameter(i + 3, ampIds.get(i));
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        for (Object[] row : rows) {
            if (row == null) {
                continue;
            }
            StockDTO dto = new StockDTO();
            dto.setId(toLong(row[0]));
            dto.setStockId(toLong(row[0]));
            dto.setItemBatchId(toLong(row[1]));
            dto.setItemId(toLong(row[2]));
            dto.setItemName(row[3] == null ? "" : row[3].toString());
            dto.setCode(row[4] == null ? "" : row[4].toString());
            dto.setRetailRate(toDouble(row[5]));
            dto.setStockQty(toDouble(row[6]));
            dto.setDateOfExpire(row[7] instanceof Date ? (Date) row[7] : null);
            dto.setBatchNo(row[8] == null ? "" : row[8].toString());
            dto.setPurchaseRate(toDouble(row[9]));
            dto.setCostRate(toDouble(row[10]));
            dto.setWholesaleRate(toDouble(row[11]));
            result.add(dto);
        }
        return result;
    }

    /**
     * Swap the selected substitute stock (chosen from the FEFO alternatives
     * list) into the bill item: updates item, batch, stock and all rate +
     * finance-detail fields. Bill totals must be recalculated by the caller
     * using that controller's own rate-recalc method (each sale controller
     * derives line values from {@code pharmaceuticalBillItem.stock.itemBatch}).
     *
     * <p>One entity load happens here (the chosen {@link Stock} with its batch)
     * — acceptable, as this is a single deliberate user action, not the hot
     * lookup path.
     *
     * @param billItem the bill item to update
     * @param selected the chosen substitute stock DTO
     * @return true if the swap succeeded
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean swapStockIntoBillItem(BillItem billItem, StockDTO selected) {
        if (billItem == null || selected == null || selected.getStockId() == null) {
            return false;
        }
        Stock substituteStock = stockFacade.findWithItemBatch(selected.getStockId());
        if (substituteStock == null || substituteStock.getItemBatch() == null) {
            return false;
        }
        ItemBatch itemBatch = substituteStock.getItemBatch();

        // Use the raw cost rate carried by the DTO from the native lookup
        // (ib.costRate read directly, already coalesced to 0.0 when null).
        // Do NOT call itemBatch.getCostRate() here: that getter derives and
        // ASSIGNS costRate = purcahseRate when costRate is null, which dirties
        // the managed ItemBatch (writes back to the entity / triggers an UPDATE)
        // and returns purchaseRate rather than 0.0. Reading the DTO keeps this
        // swap non-mutating on the batch row.
        Double rawCost = selected.getCostRate();
        double costRate = rawCost == null ? 0d : rawCost;

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
        phItem.setCostRate(costRate);
        phItem.setCostRatePack(costRate);

        BillItemFinanceDetails financeDetails = billItem.getBillItemFinanceDetails();
        if (financeDetails != null) {
            // Retail sale line value is at the retail (sale) rate.
            BigDecimal saleRate = BigDecimal.valueOf(itemBatch.getRetailsaleRate());
            financeDetails.setLineGrossRate(saleRate);
            financeDetails.setLineNetRate(saleRate);
            financeDetails.setLineCostRate(BigDecimal.valueOf(costRate));
            financeDetails.setRetailSaleRate(saleRate);

            // getBillItemFinanceDetails() lazily creates a fresh details object
            // with a null quantity, so fall back to the bill-item quantity (not
            // 1) to value multi-quantity substitute lines correctly.
            BigDecimal qty = financeDetails.getQuantity();
            if (qty == null) {
                double billQty = billItem.getQty() == null ? 0d : Math.abs(billItem.getQty());
                qty = BigDecimal.valueOf(billQty);
                financeDetails.setQuantity(qty);
                financeDetails.setTotalQuantity(qty);
            }
            financeDetails.setValueAtCostRate(BigDecimal.valueOf(costRate).multiply(qty));
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

    private String itemTable() {
        if (tItem == null) {
            tItem = resolveTable("ITEM");
        }
        return tItem;
    }

    private static Long toLong(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private static Double toDouble(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }
}
