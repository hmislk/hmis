/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.BillItemData;
import com.divudi.core.data.dto.StockAggregateResult;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists inpatient direct issue bills using native SQL for all hot-path
 * operations.
 *
 * Avoids the dominant cold-start cost in the original workflow:
 *  - findWithoutCache() in PharmacyBean.addToStockHistory() loads the full
 *    Stock → ItemBatch → Item EAGER cascade (~700 ms per item on first call)
 *  - 9 separate JPQL aggregate queries per item for StockHistory values
 *
 * Strategy:
 *  1. Persist Bill header via JPA (gets correct DTYPE + bill ID), then detach.
 *  2. Allocate BillItem / PharmaceuticalBillItem IDs via SequenceAllocatorService.
 *  3. Bulk-INSERT BillItems and PharmaceuticalBillItems with native SQL.
 *  4. Per item: atomic stock deduction + 2-query aggregate + native StockHistory INSERT.
 *  5. Finance details (BillItemFinanceDetails, BillFinanceDetails) via JPA
 *     (IDENTITY strategy, one-time per item/bill, not on the hot path).
 *  6. Update Bill totals via native SQL UPDATE.
 */
@Stateless
public class InpatientDirectIssueNativeSqlService {

    private static final Logger LOGGER = Logger.getLogger(InpatientDirectIssueNativeSqlService.class.getName());
    private static final int BATCH_SIZE = 100;

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @EJB
    private SequenceAllocatorService sequenceAllocator;

    // Cached resolved table names (cross-deployment case safety via INFORMATION_SCHEMA)
    private volatile String tBillItem = null;
    private volatile String tPharmBillItem = null;
    private volatile String tStockHistory = null;
    private volatile String tStock = null;
    private volatile String tItemBatch = null;
    private volatile String tDepartment = null;
    private volatile String tBill = null;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Settles an inpatient direct issue bill using native SQL for all DB operations.
     *
     * @param bill        The bill header (already set up in memory, no ID yet)
     * @param items       The in-memory bill items (stock/batch rates already resolved)
     * @throws RuntimeException if stock is insufficient or any DB operation fails
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void settle(Bill bill, List<BillItemData> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No items to settle");
        }

        long t0 = System.currentTimeMillis();

        // Step 1: Persist bill header via JPA to get a proper ID + DTYPE.
        //         Clear billItems first so JPA does NOT cascade into the list.
        bill.setBillItems(null);
        em.persist(bill);
        em.flush();
        long billId = bill.getId();
        em.clear(); // detach; native SQL handles all child rows

        LOGGER.log(Level.INFO, "[NativeSettle] Bill header persisted id={0} ms={1}",
                new Object[]{billId, System.currentTimeMillis() - t0});

        // Step 2: Allocate IDs for BillItems
        long[] biIds = sequenceAllocator.allocate(items.size(), BillItem.class);

        // Step 3: Allocate IDs for PharmaceuticalBillItems
        long[] pbIds = sequenceAllocator.allocate(items.size(), PharmaceuticalBillItem.class);

        // Step 4: Bulk native INSERT BillItems
        insertBillItemsBulk(billId, items, biIds);

        // Step 5: Bulk native INSERT PharmaceuticalBillItems
        insertPharmBillItemsBulk(items, biIds, pbIds);

        LOGGER.log(Level.INFO, "[NativeSettle] Rows inserted ms={0}", System.currentTimeMillis() - t0);

        // Step 6: Per-item stock deduction + aggregate + StockHistory
        long[] shIds = new long[items.size()];
        for (int i = 0; i < items.size(); i++) {
            BillItemData item = items.get(i);
            double qty = Math.abs(item.getQty());

            // Atomic deduction — throws on insufficient stock
            deductStock(item.getStockId(), qty);

            // Fetch post-deduction stock qty (needed for StockHistory.stockQty)
            double postDeductQty = fetchStockQty(item.getStockId());

            // Compute all 15 aggregate values in 2 native queries
            StockAggregateResult agg = computeAggregates(
                    item.getAmpItemId(), item.getItemBatchId(),
                    item.getDepartmentId(), item.getInstitutionId(),
                    postDeductQty,
                    item.getBatchRetailRate(), item.getBatchPurchaseRate(),
                    item.getBatchCostRate() != null ? item.getBatchCostRate() : item.getBatchPurchaseRate());

            // Insert StockHistory row
            shIds[i] = insertStockHistory(pbIds[i], item, agg);
        }

        LOGGER.log(Level.INFO, "[NativeSettle] Stock deducted + history inserted ms={0}", System.currentTimeMillis() - t0);

        // Step 7: Link StockHistory ID back into PharmaceuticalBillItem
        for (int i = 0; i < items.size(); i++) {
            em.createNativeQuery(
                    "UPDATE " + pharmBillItemTable() + " SET stockHistory_ID=? WHERE ID=?")
                    .setParameter(1, shIds[i])
                    .setParameter(2, pbIds[i])
                    .executeUpdate();
        }

        // Step 8: Finance details via JPA (IDENTITY PK — no sequence allocation needed)
        double[] billTotals = insertFinanceDetails(billId, biIds, pbIds, items);

        // Step 9: Update bill-level totals in the bill row
        em.createNativeQuery(
                "UPDATE " + billTable() + " SET total=?, netTotal=? WHERE ID=?")
                .setParameter(1, billTotals[0])
                .setParameter(2, billTotals[1])
                .setParameter(3, billId)
                .executeUpdate();

        LOGGER.log(Level.INFO, "[NativeSettle] DONE items={0} ms={1}",
                new Object[]{items.size(), System.currentTimeMillis() - t0});
    }

    // -----------------------------------------------------------------------
    // Stock deduction
    // -----------------------------------------------------------------------

    /**
     * Atomically deducts qty from stock.
     * The WHERE stock >= qty clause prevents overselling — 0 rows = insufficient stock.
     */
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

    /**
     * Computes all StockHistory aggregate values for a single item using two
     * native SQL queries:
     *
     * Query 1 — batch-level: institution and total batch qty + value aggregates
     * Query 2 — item-level:  dept / institution / total item qty + value aggregates
     */
    private StockAggregateResult computeAggregates(
            long ampItemId, long itemBatchId,
            long departmentId, long institutionId,
            double postDeductStockQty,
            double retailRate, double purchaseRate, double costRate) {

        StockAggregateResult r = new StockAggregateResult();
        r.setStockQty(postDeductStockQty);

        // Query 1: batch-level aggregates
        // Sums stock and weighted values by institution / total scope
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

        double instBatchQty   = batchRow[0] == null ? 0.0 : ((Number) batchRow[0]).doubleValue();
        double totalBatchQty  = batchRow[1] == null ? 0.0 : ((Number) batchRow[1]).doubleValue();

        r.setInstitutionBatchQty(instBatchQty);
        r.setTotalBatchQty(totalBatchQty);
        r.setInstitutionBatchStockValueAtPurchaseRate(instBatchQty * purchaseRate);
        r.setTotalBatchStockValueAtPurchaseRate(totalBatchQty * purchaseRate);
        r.setInstitutionBatchStockValueAtSaleRate(instBatchQty * retailRate);
        r.setTotalBatchStockValueAtSaleRate(totalBatchQty * retailRate);
        r.setInstitutionBatchStockValueAtCostRate(instBatchQty * costRate);
        r.setTotalBatchStockValueAtCostRate(totalBatchQty * costRate);

        // Query 2: item-level aggregates
        // Weighted values use per-batch rates stored in itembatch table
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
    // Native INSERT helpers
    // -----------------------------------------------------------------------

    private void insertBillItemsBulk(long billId, List<BillItemData> items, long[] ids) {
        int total = items.size();
        for (int start = 0; start < total; start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, total);
            List<BillItemData> batch = items.subList(start, end);

            StringBuilder sql = new StringBuilder(
                "INSERT INTO " + billItemTable()
                + " (ID, bill_ID, item_ID, qty, descreption, netValue, grossValue,"
                + " catId, createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType)"
                + " VALUES ");
            for (int i = 0; i < batch.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("(?,?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine')");
            }

            Query q = em.createNativeQuery(sql.toString());
            int p = 1;
            for (int i = 0; i < batch.size(); i++) {
                BillItemData bi = batch.get(i);
                q.setParameter(p++, ids[start + i]);
                q.setParameter(p++, billId);
                q.setParameter(p++, bi.getItemId());
                q.setParameter(p++, bi.getQty());
                q.setParameter(p++, bi.getDescription());
                q.setParameter(p++, bi.getNetValue());
                q.setParameter(p++, bi.getGrossValue());
                q.setParameter(p++, bi.getCatId());
                q.setParameter(p++, bi.getCreatedAt() != null
                        ? new Timestamp(bi.getCreatedAt().getTime()) : null);
                q.setParameter(p++, bi.getCreaterId());
            }
            q.executeUpdate();
        }
    }

    private void insertPharmBillItemsBulk(List<BillItemData> items, long[] biIds, long[] pbIds) {
        int total = items.size();
        for (int start = 0; start < total; start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, total);
            List<BillItemData> batch = items.subList(start, end);

            StringBuilder sql = new StringBuilder(
                "INSERT INTO " + pharmBillItemTable()
                + " (ID, billItem_ID, itemBatch_ID, stock_ID, qty, stringValue,"
                + " costRate, purchaseRate, retailRate, doe, description)"
                + " VALUES ");
            for (int i = 0; i < batch.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("(?,?,?,?,?,?,?,?,?,?,?)");
            }

            Query q = em.createNativeQuery(sql.toString());
            int p = 1;
            for (int i = 0; i < batch.size(); i++) {
                BillItemData bi = batch.get(i);
                q.setParameter(p++, pbIds[start + i]);
                q.setParameter(p++, biIds[start + i]);
                q.setParameter(p++, bi.getItemBatchId());
                q.setParameter(p++, bi.getStockId());
                q.setParameter(p++, bi.getPbiQty());
                q.setParameter(p++, bi.getStringValue());
                q.setParameter(p++, bi.getCostRate());
                q.setParameter(p++, bi.getPurchaseRate());
                q.setParameter(p++, bi.getRetailRate());
                q.setParameter(p++, bi.getDoe() != null
                        ? new Timestamp(bi.getDoe().getTime()) : null);
                q.setParameter(p++, bi.getDescription());
            }
            q.executeUpdate();
        }
    }

    private long insertStockHistory(long pbId, BillItemData item, StockAggregateResult agg) {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();

        // IDENTITY strategy — let DB generate the ID
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
            .setParameter(2, item.getItemBatchId())
            .setParameter(3, item.getInstitutionId())
            .setParameter(4, item.getDepartmentId())
            .setParameter(5, item.getAmpItemId())
            .setParameter(6, item.getBatchRetailRate())
            .setParameter(7, item.getBatchWholesaleRate())
            .setParameter(8, item.getBatchPurchaseRate())
            .setParameter(9, item.getBatchCostRate() != null ? item.getBatchCostRate() : item.getBatchPurchaseRate())
            .setParameter(10, new java.sql.Date(now.getTime()))
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

        // Fetch the generated ID
        Object idResult = em.createNativeQuery(
                "SELECT ID FROM " + stockHistoryTable() + " WHERE pbItem_ID=?")
                .setParameter(1, pbId)
                .getSingleResult();
        return ((Number) idResult).longValue();
    }

    // -----------------------------------------------------------------------
    // Finance details (JPA IDENTITY — simpler than native, not on hot path)
    // -----------------------------------------------------------------------

    /**
     * Creates BillItemFinanceDetails for each item and BillFinanceDetails for
     * the bill using the in-memory data from BillItemData (no JPA entity load).
     *
     * @return double[2] = { totalNetValue, totalNetValue } (gross, net)
     */
    private double[] insertFinanceDetails(long billId, long[] biIds, long[] pbIds,
                                          List<BillItemData> items) {
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        BigDecimal totalRetailSaleValue = BigDecimal.ZERO;
        BigDecimal totalWholesaleValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalFreeQuantity = BigDecimal.ZERO;
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

            BigDecimal netValue = BigDecimal.valueOf(item.getNetValue());
            BigDecimal grossValue = BigDecimal.valueOf(item.getGrossValue());

            // Insert BillItemFinanceDetails via JPA (IDENTITY PK)
            // We need the BillItem entity reference — load only by ID (no cascade)
            BillItem biRef = em.getReference(BillItem.class, biIds[i]);

            BillItemFinanceDetails bifd = new BillItemFinanceDetails();
            bifd.setBillItem(biRef);
            bifd.setCreatedAt(new Date());

            bifd.setLineNetRate(BigDecimal.valueOf(item.getNetRate()));
            bifd.setGrossRate(BigDecimal.valueOf(item.getRate()));
            bifd.setLineGrossRate(BigDecimal.valueOf(item.getRate()));
            bifd.setBillCostRate(BigDecimal.ZERO);
            bifd.setTotalCostRate(costRate);
            bifd.setLineCostRate(costRate);
            bifd.setCostRate(costRate);
            bifd.setPurchaseRate(purchaseRate);
            bifd.setRetailSaleRate(retailRate);
            bifd.setWholesaleRate(wholesaleRate);

            bifd.setLineGrossTotal(grossValue);
            bifd.setGrossTotal(grossValue);
            bifd.setLineNetTotal(netValue);
            bifd.setNetTotal(netValue);

            bifd.setLineCost(itemCostValue);
            bifd.setBillCost(BigDecimal.ZERO);
            bifd.setTotalCost(itemCostValue);

            bifd.setValueAtCostRate(costRate.multiply(totalQty).negate());
            bifd.setValueAtPurchaseRate(batchPurchase.multiply(totalQty).negate());
            bifd.setValueAtRetailRate(batchRetail.multiply(totalQty).negate());
            bifd.setValueAtWholesaleRate(batchWholesale.multiply(totalQty).negate());

            bifd.setQuantity(qty.negate());
            bifd.setQuantityByUnits(qty.negate());
            bifd.setTotalQuantity(totalQty.negate());
            bifd.setFreeQuantity(freeQty.negate());

            em.persist(bifd);

            // Link back into BillItem row
            em.createNativeQuery("UPDATE " + billItemTable()
                    + " SET BILLITEMFINANCEDETAILS_ID=? WHERE ID=?")
                    .setParameter(1, bifd.getId())
                    .setParameter(2, biIds[i])
                    .executeUpdate();

            // Update PharmaceuticalBillItem with computed cost values
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
            billNetTotal = billNetTotal.add(netValue);
        }

        // Insert BillFinanceDetails via JPA (IDENTITY PK, one row)
        Bill billRef = em.getReference(Bill.class, billId);
        BillFinanceDetails bfd = new BillFinanceDetails();
        bfd.setBill(billRef);
        bfd.setCreatedAt(new Date());
        bfd.setNetTotal(billNetTotal);
        bfd.setGrossTotal(billNetTotal); // no mark-up for inpatient direct issue
        bfd.setTotalCostValue(totalCostValue.negate());
        bfd.setTotalPurchaseValue(totalPurchaseValue.negate());
        bfd.setTotalRetailSaleValue(totalRetailSaleValue.negate());
        bfd.setTotalWholesaleValue(totalWholesaleValue.negate());
        bfd.setTotalQuantity(totalQuantity.negate());
        bfd.setTotalFreeQuantity(totalFreeQuantity.negate());
        em.persist(bfd);
        em.flush();

        // Link BillFinanceDetails ID into bill row
        em.createNativeQuery("UPDATE " + billTable() + " SET BILLFINANCEDETAILS_ID=? WHERE ID=?")
                .setParameter(1, bfd.getId())
                .setParameter(2, billId)
                .executeUpdate();

        return new double[]{billNetTotal.doubleValue(), billNetTotal.doubleValue()};
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

    private String billItemTable() {
        if (tBillItem == null) tBillItem = resolveTable("BILLITEM");
        return tBillItem;
    }

    private String pharmBillItemTable() {
        if (tPharmBillItem == null) tPharmBillItem = resolveTable("PHARMACEUTICALBILLITEM");
        return tPharmBillItem;
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
}
