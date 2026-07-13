/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.BillItemData;
import com.divudi.core.data.dto.PrintBillData;
import com.divudi.core.data.dto.StockAggregateResult;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.StockHistory;
import java.util.ArrayList;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists inpatient direct issue bills using fully native SQL throughout the
 * settle path — Bill header, BillItemFinanceDetails, BillFinanceDetails, and
 * all existing stock/aggregates INSERTs.
 *
 * Zero JPA entity class descriptors are initialized during settle, eliminating
 * the dominant cold-start warmup cost (Bill EAGER cascade: PharmacyBill,
 * StockBill, BillFinanceDetails, Request — each triggers its own descriptor
 * and all of its EAGER associations on the first em.persist() call).
 */
@Stateless
public class InpatientDirectIssueNativeSqlService {

    private static final Logger LOGGER = Logger.getLogger(InpatientDirectIssueNativeSqlService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    // Cached resolved table names (cross-deployment case safety via INFORMATION_SCHEMA)
    private volatile String tStockHistory = null;
    private volatile String tStock = null;
    private volatile String tItemBatch = null;
    private volatile String tDepartment = null;
    private volatile String tBill = null;
    private volatile String tBillItem = null;
    private volatile String tPharmBillItem = null;
    private volatile String tItem = null;
    private volatile String tBillItemFD = null;
    private volatile String tBillFD = null;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void settle(Bill bill, List<BillItemData> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No items to settle");
        }

        long t0 = System.currentTimeMillis();

        // Step 1: Persist the bill header via JPA so EclipseLink tracks the entity.
        // Native-only inserts bypass the L2 cache, causing JPQL in fetchIssueTable()
        // to silently swallow the load exception and return an empty list (#20435).
        if (bill.getCreatedAt() == null) {
            bill.setCreatedAt(new java.util.Date());
        }
        em.persist(bill);
        em.flush(); // ensures IDENTITY-generated ID is assigned
        long billId = bill.getId();

        LOGGER.log(Level.INFO, "[NativeSettle] Bill header persisted id={0} ms={1}",
                new Object[]{billId, System.currentTimeMillis() - t0});

        // Step 2: Native INSERT BillItem + PharmaceuticalBillItem one-at-a-time.
        long[] biIds = new long[items.size()];
        long[] pbIds = new long[items.size()];

        for (int i = 0; i < items.size(); i++) {
            BillItemData d = items.get(i);
            java.util.Date createdAt = d.getCreatedAt() != null ? d.getCreatedAt() : new java.util.Date();

            double absQty       = Math.abs(d.getQty());
            double absNetValue  = Math.abs(d.getNetValue());
            double absGrossValue = Math.abs(d.getGrossValue());
            double netRate      = absQty > 0 ? absNetValue / absQty : 0.0;
            double rate         = d.getRate();
            double marginValue  = Math.abs(d.getMarginValue());
            double discountValue = Math.abs(d.getDiscountValue());
            // discountRate is a per-unit discount amount throughout this codebase
            // (see PharmacyFastRetailSaleController.calculateBillItemDiscountRate:
            // dr = retailRate * discountPercent / 100), NOT the raw percentage.
            double discountRate = absQty > 0 ? discountValue / absQty : 0.0;

            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (bill_ID, item_ID, qty, descreption, netValue, grossValue, netRate,"
                + " rate, marginValue, discount, discountRate,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType, referanceBillItem_ID)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine',?)")
                .setParameter(1, billId)
                .setParameter(2, d.getItemId())
                .setParameter(3, absQty)
                .setParameter(4, d.getDescription())
                .setParameter(5, absNetValue)
                .setParameter(6, absGrossValue)
                .setParameter(7, netRate)
                .setParameter(8, rate)
                .setParameter(9, marginValue)
                .setParameter(10, discountValue)
                .setParameter(11, discountRate)
                .setParameter(12, new Timestamp(createdAt.getTime()))
                .setParameter(13, d.getCreaterId())
                .setParameter(14, d.getSourceRequestBillItemId())
                .executeUpdate();
            biIds[i] = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();

            em.createNativeQuery(
                "INSERT INTO " + pharmBillItemTable()
                + " (billItem_ID, itemBatch_ID, stock_ID, qty, stringValue,"
                + " costRate, purchaseRate, retailRate, wholesaleRate, doe, description)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?)")
                .setParameter(1, biIds[i])
                .setParameter(2, d.getItemBatchId())
                .setParameter(3, d.getStockId())
                .setParameter(4, d.getPbiQty())
                .setParameter(5, d.getStringValue())
                .setParameter(6, d.getCostRate())
                .setParameter(7, d.getPurchaseRate())
                .setParameter(8, d.getRetailRate())
                .setParameter(9, d.getWholesaleRate())
                .setParameter(10, d.getDoe() != null ? new Timestamp(d.getDoe().getTime()) : null)
                .setParameter(11, d.getDescription())
                .executeUpdate();
            pbIds[i] = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        }

        LOGGER.log(Level.INFO, "[NativeSettle] BillItem+PBI inserted ms={0}", System.currentTimeMillis() - t0);

        // Step 3: Per-item: stock deduction + aggregates + StockHistory
        for (int i = 0; i < items.size(); i++) {
            BillItemData item = items.get(i);
            double qty = Math.abs(item.getQty());

            deductStock(item.getStockId(), qty);

            double postDeductQty = fetchStockQty(item.getStockId());

            long ampItemId     = item.getAmpItemId()     != null ? item.getAmpItemId()     : (item.getItemId() != null ? item.getItemId() : 0L);
            long itemBatchId   = item.getItemBatchId()   != null ? item.getItemBatchId()   : 0L;
            long departmentId  = item.getDepartmentId()  != null ? item.getDepartmentId()  : 0L;
            long institutionId = item.getInstitutionId() != null ? item.getInstitutionId() : 0L;

            StockAggregateResult agg = computeAggregates(
                    ampItemId, itemBatchId, departmentId, institutionId,
                    postDeductQty,
                    item.getBatchRetailRate(), item.getBatchPurchaseRate(),
                    item.getBatchCostRate() != null ? item.getBatchCostRate() : item.getBatchPurchaseRate());

            insertStockHistory(pbIds[i], item, agg, ampItemId, itemBatchId, departmentId, institutionId);
        }

        LOGGER.log(Level.INFO, "[NativeSettle] Stock deducted + history inserted ms={0}", System.currentTimeMillis() - t0);

        // Step 4: Finance details — fully native INSERTs
        LOGGER.log(Level.INFO, "[NativeSettle] Starting finance details ms={0}", System.currentTimeMillis() - t0);
        double[] billTotals = insertFinanceDetails(billId, biIds, pbIds, items);

        // Step 5: Update bill-level totals natively.
        em.createNativeQuery(
                "UPDATE " + billTable() + " SET total=?, netTotal=?, grantTotal=? WHERE ID=?")
                .setParameter(1, billTotals[0])   // grossTotal
                .setParameter(2, billTotals[1])   // netTotal
                .setParameter(3, billTotals[0])   // grantTotal = grossTotal (intentional naming per Bill entity)
                .setParameter(4, billId)
                .executeUpdate();

        // Reconcile the JPA caches with the natively-written state WITHOUT the
        // catastrophic full-graph reload that em.refresh(bill) triggers.
        //
        // Previously this used em.refresh(bill) to pull the natively-written
        // BILLFINANCEDETAILS_ID FK + totals back into L1 so a stale null FK would
        // not be merged into L2 at commit (#20435). But em.refresh reloads the
        // Bill's entire EAGER graph, and Bill.stockBill (EAGER) ↔ StockBill.bill
        // (EAGER) form a circular EAGER reference; every Bill pulled in drags its
        // own EAGER one-to-ones (pharmacyBill/stockBill/billFinanceDetails/
        // currentRequest). For a batch whose related bill-graph is not yet in the
        // L2 cache this fanned out into a ~30s recursive load — the "first issue of
        // a batch is slow, later issues of the same batch are fast" symptom (#21888).
        //
        // Instead: detach the managed Bill so its stale (billFinanceDetails=null)
        // state is NOT merged into L2 at commit, and evict Bill from L2 so the next
        // read reloads the correct FK straight from the database. Same correctness
        // guarantee as the refresh, none of the EAGER-graph cost.
        long tReconcile = System.currentTimeMillis();
        em.detach(bill);
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(Bill.class, billId);
        cache.evict(StockHistory.class);
        cache.evict(Stock.class);
        cache.evict(BillItem.class);
        cache.evict(BillFinanceDetails.class);
        cache.evict(BillItemFinanceDetails.class);
        LOGGER.log(Level.INFO, "[NativeSettle] cache reconcile done ms={0} reconcileMs={1}",
                new Object[]{System.currentTimeMillis() - t0, System.currentTimeMillis() - tReconcile});

        LOGGER.log(Level.INFO, "[NativeSettle] DONE items={0} ms={1}",
                new Object[]{items.size(), System.currentTimeMillis() - t0});
    }

    // -----------------------------------------------------------------------
    // Stock deduction
    // -----------------------------------------------------------------------

    private void deductStock(long stockId, double qty) {
        int updated = em.createNativeQuery(
                "UPDATE " + stockTable() + " SET stock=stock-? WHERE ID=? AND stock>=?")
                .setParameter(1, qty)
                .setParameter(2, stockId)
                .setParameter(3, qty)
                .executeUpdate();
        if (updated == 0) {
            throw new RuntimeException("Insufficient stock for stock ID " + stockId
                    + " (requested qty=" + qty + ")");
        }
    }

    private double fetchStockQty(long stockId) {
        Object result = em.createNativeQuery(
                "SELECT stock FROM " + stockTable() + " WHERE ID=?")
                .setParameter(1, stockId)
                .getSingleResult();
        return result == null ? 0.0 : ((Number) result).doubleValue();
    }

    // -----------------------------------------------------------------------
    // Aggregate computation (2 queries per item, replacing 9 JPQL calls)
    // -----------------------------------------------------------------------

    private StockAggregateResult computeAggregates(
            long ampItemId, long itemBatchId,
            long departmentId, long institutionId,
            double postDeductStockQty,
            double retailRate, double purchaseRate, double costRate) {

        StockAggregateResult r = new StockAggregateResult();
        r.setStockQty(postDeductStockQty);

        String batchSql =
            "SELECT "
            + "  SUM(CASE WHEN d.institution_ID = ? THEN s.stock ELSE 0 END) AS instBatchQty,"
            + "  SUM(s.stock) AS totalBatchQty "
            + "FROM " + stockTable() + " s "
            + "JOIN " + departmentTable() + " d ON s.department_ID = d.ID "
            + "WHERE s.itemBatch_ID = ?";

        Object[] batchRow = (Object[]) em.createNativeQuery(batchSql)
                .setParameter(1, institutionId)
                .setParameter(2, itemBatchId)
                .getSingleResult();

        double instBatchQty  = batchRow[0] == null ? 0.0 : ((Number) batchRow[0]).doubleValue();
        double totalBatchQty = batchRow[1] == null ? 0.0 : ((Number) batchRow[1]).doubleValue();

        r.setInstitutionBatchQty(instBatchQty);
        r.setTotalBatchQty(totalBatchQty);
        r.setInstitutionBatchStockValueAtPurchaseRate(instBatchQty * purchaseRate);
        r.setTotalBatchStockValueAtPurchaseRate(totalBatchQty * purchaseRate);
        r.setInstitutionBatchStockValueAtSaleRate(instBatchQty * retailRate);
        r.setTotalBatchStockValueAtSaleRate(totalBatchQty * retailRate);
        r.setInstitutionBatchStockValueAtCostRate(instBatchQty * costRate);
        r.setTotalBatchStockValueAtCostRate(totalBatchQty * costRate);

        String itemSql =
            "SELECT "
            + "  SUM(CASE WHEN s.department_ID = ? THEN s.stock ELSE 0 END) AS deptItemQty,"
            + "  SUM(CASE WHEN d.institution_ID = ? THEN s.stock ELSE 0 END) AS instItemQty,"
            + "  SUM(s.stock) AS totalItemQty,"
            + "  SUM(CASE WHEN s.department_ID = ? THEN COALESCE(ib.purcahseRate,0)*s.stock ELSE 0 END) AS deptItemPurchVal,"
            + "  SUM(CASE WHEN d.institution_ID = ? THEN COALESCE(ib.purcahseRate,0)*s.stock ELSE 0 END) AS instItemPurchVal,"
            + "  SUM(COALESCE(ib.purcahseRate,0)*s.stock) AS totalItemPurchVal,"
            + "  SUM(CASE WHEN s.department_ID = ? THEN COALESCE(ib.costRate,0)*s.stock ELSE 0 END) AS deptItemCostVal,"
            + "  SUM(CASE WHEN d.institution_ID = ? THEN COALESCE(ib.costRate,0)*s.stock ELSE 0 END) AS instItemCostVal,"
            + "  SUM(COALESCE(ib.costRate,0)*s.stock) AS totalItemCostVal,"
            + "  SUM(CASE WHEN s.department_ID = ? THEN COALESCE(ib.retailsaleRate,0)*s.stock ELSE 0 END) AS deptItemRetailVal,"
            + "  SUM(CASE WHEN d.institution_ID = ? THEN COALESCE(ib.retailsaleRate,0)*s.stock ELSE 0 END) AS instItemRetailVal,"
            + "  SUM(COALESCE(ib.retailsaleRate,0)*s.stock) AS totalItemRetailVal "
            + "FROM " + stockTable() + " s "
            + "JOIN " + itemBatchTable() + " ib ON s.itemBatch_ID = ib.ID "
            + "JOIN " + departmentTable() + " d ON s.department_ID = d.ID "
            + "WHERE ib.item_ID = ?";

        Object[] itemRow = (Object[]) em.createNativeQuery(itemSql)
                .setParameter(1, departmentId)
                .setParameter(2, institutionId)
                .setParameter(3, departmentId)
                .setParameter(4, institutionId)
                .setParameter(5, departmentId)
                .setParameter(6, institutionId)
                .setParameter(7, departmentId)
                .setParameter(8, institutionId)
                .setParameter(9, ampItemId)
                .getSingleResult();

        r.setDepartmentItemStock(toDouble(itemRow[0]));
        r.setInstitutionItemStock(toDouble(itemRow[1]));
        r.setTotalItemStock(toDouble(itemRow[2]));
        r.setItemStockValueAtPurchaseRate(toDouble(itemRow[3]));
        r.setInstitutionItemStockValueAtPurchaseRate(toDouble(itemRow[4]));
        r.setTotalItemStockValueAtPurchaseRate(toDouble(itemRow[5]));
        r.setItemStockValueAtCostRate(toDouble(itemRow[6]));
        r.setInstitutionItemStockValueAtCostRate(toDouble(itemRow[7]));
        r.setTotalItemStockValueAtCostRate(toDouble(itemRow[8]));
        r.setItemStockValueAtSaleRate(toDouble(itemRow[9]));
        r.setInstitutionItemStockValueAtSaleRate(toDouble(itemRow[10]));
        r.setTotalItemStockValueAtSaleRate(toDouble(itemRow[11]));

        return r;
    }

    private static double toDouble(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    // -----------------------------------------------------------------------
    // StockHistory native INSERT
    // -----------------------------------------------------------------------

    private long insertStockHistory(long pbId, BillItemData item, StockAggregateResult agg,
                                        long ampItemId, long itemBatchId, long departmentId, long institutionId) {
        java.util.Date now = new java.util.Date();
        Calendar cal = Calendar.getInstance();

        em.createNativeQuery(
            "INSERT INTO " + stockHistoryTable()
            + " (pbItem_ID, itemBatch_ID, institution_ID, department_ID, item_ID,"
            + " retailRate, wholesaleRate, purchaseRate, costRate,"
            + " stockAt, fromDate, createdAt,"
            + " stockQty, instituionBatchQty, totalBatchQty,"
            + " itemStock, institutionItemStock, totalItemStock,"
            + " stockSaleValue, stockPurchaseValue, stockCostValue,"
            + " institutionBatchStockValueAtSaleRate, totalBatchStockValueAtSaleRate,"
            + " institutionBatchStockValueAtPurchaseRate, totalBatchStockValueAtPurchaseRate,"
            + " institutionBatchStockValueAtCostRate, totalBatchStockValueAtCostRate,"
            + " itemStockValueAtSaleRate, institutionItemStockValueAtSaleRate, totalItemStockValueAtSaleRate,"
            + " itemStockValueAtPurchaseRate, institutionItemStockValueAtPurchaseRate, totalItemStockValueAtPurchaseRate,"
            + " itemStockValueAtCostRate, institutionItemStockValueAtCostRate, totalItemStockValueAtCostRate,"
            + " hxYear, hxMonth, hxDate, hxWeek,"
            + " retired)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)")
            .setParameter(1, pbId)
            .setParameter(2, itemBatchId)
            .setParameter(3, institutionId)
            .setParameter(4, departmentId)
            .setParameter(5, ampItemId)
            .setParameter(6, item.getBatchRetailRate())
            .setParameter(7, item.getBatchWholesaleRate())
            .setParameter(8, item.getBatchPurchaseRate())
            .setParameter(9, item.getBatchCostRate() != null ? item.getBatchCostRate() : item.getBatchPurchaseRate())
            .setParameter(10, new Date(now.getTime()))
            .setParameter(11, new Timestamp(now.getTime()))
            .setParameter(12, new Timestamp(now.getTime()))
            .setParameter(13, agg.getStockQty())
            .setParameter(14, agg.getInstitutionBatchQty())
            .setParameter(15, agg.getTotalBatchQty())
            .setParameter(16, agg.getDepartmentItemStock())
            .setParameter(17, agg.getInstitutionItemStock())
            .setParameter(18, agg.getTotalItemStock())
            .setParameter(19, agg.getStockQty() * item.getBatchRetailRate())
            .setParameter(20, agg.getStockQty() * item.getBatchPurchaseRate())
            .setParameter(21, agg.getStockQty() * (item.getBatchCostRate() != null ? item.getBatchCostRate() : item.getBatchPurchaseRate()))
            .setParameter(22, agg.getInstitutionBatchStockValueAtSaleRate())
            .setParameter(23, agg.getTotalBatchStockValueAtSaleRate())
            .setParameter(24, agg.getInstitutionBatchStockValueAtPurchaseRate())
            .setParameter(25, agg.getTotalBatchStockValueAtPurchaseRate())
            .setParameter(26, agg.getInstitutionBatchStockValueAtCostRate())
            .setParameter(27, agg.getTotalBatchStockValueAtCostRate())
            .setParameter(28, agg.getItemStockValueAtSaleRate())
            .setParameter(29, agg.getInstitutionItemStockValueAtSaleRate())
            .setParameter(30, agg.getTotalItemStockValueAtSaleRate())
            .setParameter(31, agg.getItemStockValueAtPurchaseRate())
            .setParameter(32, agg.getInstitutionItemStockValueAtPurchaseRate())
            .setParameter(33, agg.getTotalItemStockValueAtPurchaseRate())
            .setParameter(34, agg.getItemStockValueAtCostRate())
            .setParameter(35, agg.getInstitutionItemStockValueAtCostRate())
            .setParameter(36, agg.getTotalItemStockValueAtCostRate())
            .setParameter(37, cal.get(Calendar.YEAR))
            .setParameter(38, cal.get(Calendar.MONTH))
            .setParameter(39, cal.get(Calendar.DATE))
            .setParameter(40, cal.get(Calendar.WEEK_OF_YEAR))
            .executeUpdate();

        Object idResult = em.createNativeQuery(
                "SELECT ID FROM " + stockHistoryTable() + " WHERE pbItem_ID=?")
                .setParameter(1, pbId)
                .getSingleResult();
        return ((Number) idResult).longValue();
    }

    // -----------------------------------------------------------------------
    // Finance details — fully native INSERTs
    // -----------------------------------------------------------------------

    /**
     * Inserts one BillItemFinanceDetails row per item and one BillFinanceDetails row for the bill.
     * Returns double[]{billGrossTotal, billNetTotal}.
     *
     * Previously used em.persist() which triggered EclipseLink class descriptor
     * initialisation for BillItemFinanceDetails (4 EAGER associations: PharmaceuticalBillItem,
     * BillItemFinanceDetails backref, billFees EAGER collection, patientInvestigation).
     */
    private double[] insertFinanceDetails(long billId, long[] biIds, long[] pbIds,
                                          List<BillItemData> items) {
        long fdT0 = System.currentTimeMillis();
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        BigDecimal totalRetailSaleValue = BigDecimal.ZERO;
        BigDecimal totalWholesaleValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalFreeQuantity = BigDecimal.ZERO;
        BigDecimal billGrossTotal = BigDecimal.ZERO;
        BigDecimal billNetTotal = BigDecimal.ZERO;

        for (int i = 0; i < items.size(); i++) {
            BillItemData item = items.get(i);

            BigDecimal qty = BigDecimal.valueOf(Math.abs(item.getQty()));
            BigDecimal freeQty = BigDecimal.valueOf(Math.abs(item.getFreeQty()));
            BigDecimal totalQty = qty.add(freeQty);

            BigDecimal retailRate = BigDecimal.valueOf(item.getRetailRate());
            BigDecimal purchaseRate = BigDecimal.valueOf(item.getPurchaseRate());
            BigDecimal wholesaleRate = BigDecimal.valueOf(item.getWholesaleRate());
            BigDecimal batchRetail = BigDecimal.valueOf(item.getBatchRetailRate());
            BigDecimal batchPurchase = BigDecimal.valueOf(item.getBatchPurchaseRate());
            BigDecimal batchWholesale = BigDecimal.valueOf(item.getBatchWholesaleRate());
            BigDecimal costRate = item.getBatchCostRate() != null && item.getBatchCostRate() > 0
                    ? BigDecimal.valueOf(item.getBatchCostRate())
                    : batchPurchase;

            BigDecimal itemCostValue = costRate.multiply(qty);
            BigDecimal itemRetailValue = batchRetail.multiply(totalQty);
            BigDecimal itemPurchaseValue = batchPurchase.multiply(totalQty);
            BigDecimal itemWholesaleValue = batchWholesale.multiply(totalQty);

            BigDecimal netValue   = BigDecimal.valueOf(Math.abs(item.getNetValue()));
            BigDecimal grossValue = BigDecimal.valueOf(Math.abs(item.getGrossValue()));
            BigDecimal lineNetRate = qty.compareTo(BigDecimal.ZERO) > 0
                    ? netValue.divide(qty, 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal lineGrossRate = BigDecimal.valueOf(item.getRate());

            // Native INSERT BillItemFinanceDetails — avoids BillItem EAGER cascade warmup
            em.createNativeQuery(
                "INSERT INTO " + billItemFdTable()
                + " (createdAt, unitsPerPack,"
                + " lineNetRate, lineGrossRate, grossRate,"
                + " billCostRate, lineCostRate, totalCostRate, costRate,"
                + " purchaseRate, retailSaleRate, wholesaleRate,"
                + " lineGrossTotal, grossTotal,"
                + " lineNetTotal, netTotal,"
                + " lineCost, billCost, totalCost,"
                + " valueAtCostRate, valueAtPurchaseRate, valueAtRetailRate, valueAtWholesaleRate,"
                + " quantity, quantityByUnits, totalQuantity, freeQuantity)"
                + " VALUES (NOW(),1.0,?,?,?,0,?,?,?,?,?,?,?,?,?,?,?,0,?,?,?,?,?,?,?,?,?)")
                .setParameter(1, lineNetRate)
                .setParameter(2, lineGrossRate)
                .setParameter(3, lineGrossRate)    // grossRate = lineGrossRate
                .setParameter(4, costRate)         // lineCostRate
                .setParameter(5, costRate)         // totalCostRate
                .setParameter(6, costRate)         // costRate
                .setParameter(7, purchaseRate)
                .setParameter(8, retailRate)
                .setParameter(9, wholesaleRate)
                .setParameter(10, grossValue)      // lineGrossTotal
                .setParameter(11, grossValue)      // grossTotal
                .setParameter(12, netValue)        // lineNetTotal
                .setParameter(13, netValue)        // netTotal
                .setParameter(14, itemCostValue)   // lineCost
                .setParameter(15, itemCostValue)   // totalCost
                .setParameter(16, costRate.multiply(totalQty).negate())   // valueAtCostRate
                .setParameter(17, batchPurchase.multiply(totalQty).negate()) // valueAtPurchaseRate
                .setParameter(18, batchRetail.multiply(totalQty).negate())   // valueAtRetailRate
                .setParameter(19, batchWholesale.multiply(totalQty).negate()) // valueAtWholesaleRate
                .setParameter(20, qty.negate())
                .setParameter(21, qty.negate())    // quantityByUnits
                .setParameter(22, totalQty.negate())
                .setParameter(23, freeQty.negate())
                .executeUpdate();

            long bifdId = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();

            // Link BillItemFinanceDetails into BillItem row
            em.createNativeQuery("UPDATE " + billItemTable()
                    + " SET BILLITEMFINANCEDETAILS_ID=? WHERE ID=?")
                    .setParameter(1, bifdId)
                    .setParameter(2, biIds[i])
                    .executeUpdate();

            // Update PharmaceuticalBillItem with computed cost/retail/purchase values
            em.createNativeQuery("UPDATE " + pharmBillItemTable()
                    + " SET costRate=?, costValue=?, retailValue=?, purchaseValue=? WHERE ID=?")
                    .setParameter(1, costRate.doubleValue())
                    .setParameter(2, itemCostValue.doubleValue())
                    .setParameter(3, itemRetailValue.doubleValue())
                    .setParameter(4, itemPurchaseValue.doubleValue())
                    .setParameter(5, pbIds[i])
                    .executeUpdate();

            totalCostValue = totalCostValue.add(itemCostValue);
            totalPurchaseValue = totalPurchaseValue.add(itemPurchaseValue);
            totalRetailSaleValue = totalRetailSaleValue.add(itemRetailValue);
            totalWholesaleValue = totalWholesaleValue.add(itemWholesaleValue);
            totalQuantity = totalQuantity.add(qty);
            totalFreeQuantity = totalFreeQuantity.add(freeQty);
            billGrossTotal = billGrossTotal.add(grossValue);
            billNetTotal = billNetTotal.add(netValue);

            LOGGER.log(Level.INFO, "[financeDetails] item {0} inserted bifdId={1} ms={2}",
                    new Object[]{i, bifdId, System.currentTimeMillis() - fdT0});
        }

        // Native INSERT BillFinanceDetails — one row per bill.
        // totalCostValue/totalPurchaseValue/totalRetailSaleValue/totalWholesaleValue/totalQuantity/totalFreeQuantity
        // are stored as NEGATIVE values to reflect the outflow sign convention used by the old entity path
        // (BFD aggregate fields are negative for issues; netTotal/grossTotal remain positive).
        em.createNativeQuery(
            "INSERT INTO " + billFdTable()
            + " (createdAt, netTotal, grossTotal,"
            + " totalCostValue, totalPurchaseValue, totalRetailSaleValue, totalWholesaleValue,"
            + " totalQuantity, totalFreeQuantity)"
            + " VALUES (NOW(),?,?,?,?,?,?,?,?)")
            .setParameter(1, billNetTotal)
            .setParameter(2, billGrossTotal)
            .setParameter(3, totalCostValue.negate())
            .setParameter(4, totalPurchaseValue.negate())
            .setParameter(5, totalRetailSaleValue.negate())
            .setParameter(6, totalWholesaleValue.negate())
            .setParameter(7, totalQuantity.negate())
            .setParameter(8, totalFreeQuantity.negate())
            .executeUpdate();

        long bfdId = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();

        // Link BillFinanceDetails into Bill row (Bill owns the FK BILLFINANCEDETAILS_ID)
        em.createNativeQuery("UPDATE " + billTable() + " SET BILLFINANCEDETAILS_ID=? WHERE ID=?")
                .setParameter(1, bfdId)
                .setParameter(2, billId)
                .executeUpdate();

        LOGGER.log(Level.INFO, "[financeDetails] DONE bfdId={0} ms={1}",
                new Object[]{bfdId, System.currentTimeMillis() - fdT0});

        return new double[]{billGrossTotal.doubleValue(), billNetTotal.doubleValue()};
    }

    // -----------------------------------------------------------------------
    // Table name resolution (INFORMATION_SCHEMA, cached after first call)
    // -----------------------------------------------------------------------

    private String resolveTable(String upperName) {
        Object name = em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = ? LIMIT 1")
                .setParameter(1, upperName)
                .getSingleResult();
        return name.toString();
    }

    private String stockHistoryTable() {
        if (tStockHistory == null) tStockHistory = resolveTable("STOCKHISTORY");
        return tStockHistory;
    }

    private String stockTable() {
        if (tStock == null) tStock = resolveTable("STOCK");
        return tStock;
    }

    private String itemBatchTable() {
        if (tItemBatch == null) tItemBatch = resolveTable("ITEMBATCH");
        return tItemBatch;
    }

    private String departmentTable() {
        if (tDepartment == null) tDepartment = resolveTable("DEPARTMENT");
        return tDepartment;
    }

    private String billTable() {
        if (tBill == null) tBill = resolveTable("BILL");
        return tBill;
    }

    private String billItemTable() {
        if (tBillItem == null) tBillItem = resolveTable("BILLITEM");
        return tBillItem;
    }

    private String pharmBillItemTable() {
        if (tPharmBillItem == null) tPharmBillItem = resolveTable("PHARMACEUTICALBILLITEM");
        return tPharmBillItem;
    }

    private String itemTable() {
        if (tItem == null) tItem = resolveTable("ITEM");
        return tItem;
    }

    private String billItemFdTable() {
        if (tBillItemFD == null) tBillItemFD = resolveTable("BILLITEMFINANCEDETAILS");
        return tBillItemFD;
    }

    private String billFdTable() {
        if (tBillFD == null) tBillFD = resolveTable("BILLFINANCEDETAILS");
        return tBillFD;
    }

    private static String safeStr(String s) {
        return s != null ? s : "";
    }

    /**
     * Loads view data for an existing settled inpatient direct issue bill.
     * Returns Object[]{PatientEncounter, PrintBillData, List&lt;BillItemData&gt;}.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Object[] loadViewDataByBillId(long billId) {
        Bill bill = em.find(Bill.class, billId);
        if (bill == null) {
            return null;
        }

        PatientEncounter pe = bill.getPatientEncounter();

        PrintBillData pbd = new PrintBillData();
        Department dept = bill.getDepartment();
        if (dept != null) {
            pbd.setDepartmentName(safeStr(dept.getName()));
            pbd.setDepartmentPrintingName(dept.getPrintingName() != null
                    ? dept.getPrintingName() : safeStr(dept.getName()));
            pbd.setDepartmentTelephone1(safeStr(dept.getTelephone1()));
            Institution inst = dept.getInstitution();
            if (inst != null) {
                pbd.setInstitutionName(safeStr(inst.getName()));
                pbd.setInstitutionAddress(safeStr(inst.getAddress()));
            }
        }
        if (bill.getPatient() != null && bill.getPatient().getPerson() != null) {
            pbd.setPatientName(safeStr(bill.getPatient().getPerson().getNameWithTitle()));
            try {
                pbd.setPatientAgeSex(safeStr(bill.getPatient().getPerson().getAgeAsShortString())
                        + " / " + (bill.getPatient().getPerson().getSex() != null
                                ? bill.getPatient().getPerson().getSex().getLabel() : ""));
            } catch (Exception ignore) {
            }
        }
        if (pe != null) {
            pbd.setBhtNo(safeStr(pe.getBhtNo()));
        }
        pbd.setBillNo(safeStr(bill.getDeptId()));
        pbd.setCreatedAt(bill.getCreatedAt());
        pbd.setNetTotal(Math.abs(bill.getNetTotal()));

        String sql = "SELECT i.name, ABS(bi.qty), COALESCE(bi.rate, bi.netRate, 0),"
                + " ABS(bi.netRate), ABS(bi.netValue), ABS(bi.grossValue), ib.dateOfExpire"
                + " FROM " + billItemTable() + " bi"
                + " JOIN " + pharmBillItemTable() + " pbi ON pbi.billItem_ID = bi.ID"
                + " JOIN " + itemBatchTable() + " ib ON ib.ID = pbi.itemBatch_ID"
                + " JOIN " + itemTable() + " i ON i.ID = bi.item_ID"
                + " WHERE bi.bill_ID = ? AND (bi.retired IS NULL OR bi.retired = 0)"
                + " ORDER BY bi.ID";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, billId)
                .getResultList();

        List<BillItemData> itemList = new ArrayList<>();
        for (Object[] row : rows) {
            BillItemData bid = new BillItemData();
            bid.setItemName(row[0] != null ? row[0].toString() : "");
            bid.setQty(toDouble(row[1]));
            bid.setRate(toDouble(row[2]));
            bid.setNetRate(toDouble(row[3]));
            bid.setNetValue(toDouble(row[4]));
            bid.setGrossValue(toDouble(row[5]));
            bid.setDoe(row[6] instanceof java.sql.Date
                    ? new java.util.Date(((java.sql.Date) row[6]).getTime())
                    : (row[6] instanceof java.util.Date ? (java.util.Date) row[6] : null));
            itemList.add(bid);
        }

        return new Object[]{pe, pbd, itemList};
    }
}
