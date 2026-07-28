/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.TokenType;
import com.divudi.core.data.dto.BillItemData;
import com.divudi.core.data.dto.PrintBillData;
import com.divudi.core.data.dto.StockAggregateResult;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.Token;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.StockHistory;
import java.util.ArrayList;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Native-SQL settle path for the pharmacy "Sale for Cashier" page.
 *
 * Unlike RetailSaleNativeSqlService this writes a SINGLE bill — a PreBill of
 * BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER. There is no BilledBill
 * and there are no Payment rows: that atomic type is NO_PAYMENT / NO_FINANCE_TRANSACTIONS,
 * and the cashier settles payment later via PharmacyPreSettleController.
 *
 * Stock is still deducted here, at pharmacy time, exactly as the legacy
 * PharmacySaleForCashierController did.
 *
 * Deviation from legacy: BillFinanceDetails / BillItemFinanceDetails ARE written at settle.
 * Legacy wrote none, which is why DataAdministrationController
 * .backfillBfdForPreToSettleAtCashierBills() exists — without BFD the F15 report reads
 * totalRetailSaleValue as 0. Bills created here never need that backfill.
 *
 * Issue: #20261
 */
@Stateless
public class RetailSaleForCashierNativeSqlService {

    private static final Logger LOGGER = Logger.getLogger(RetailSaleForCashierNativeSqlService.class.getName());

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
    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void settle(PreBill preBill, List<BillItemData> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No items to settle");
        }

        // No payment parameters: PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER is a
        // NO_PAYMENT bill type, so no Payment entity is created here. The controller
        // stamps payment details onto the bill's own columns before calling settle().

        long t0 = System.currentTimeMillis();

        // Step 1a: Persist PreBill first (bill items will reference its ID).
        preBill.setBillItems(null);
        em.persist(preBill);
        em.flush();
        long preBillId = preBill.getId();

        LOGGER.log(Level.INFO, "[CashierNativeSettle] PreBill id={0} ms={1}",
                new Object[]{preBillId, System.currentTimeMillis() - t0});

        // Step 2: Native INSERT BillItem + fully-populated PBI on the single PreBill
        long[] biPreIds = new long[items.size()];
        long[] pbPreIds = new long[items.size()];

        for (int i = 0; i < items.size(); i++) {
            BillItemData d = items.get(i);
            Date createdAt = d.getCreatedAt() != null ? d.getCreatedAt() : new Date();

            double absQty       = Math.abs(d.getQty());
            double absNetValue  = Math.abs(d.getNetValue());
            double absGrossValue = Math.abs(d.getGrossValue());
            double netRate      = absQty > 0 ? absNetValue / absQty : 0.0;

            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (bill_ID, item_ID, qty, descreption, netValue, grossValue, Rate, netRate,"
                + " discount, discountRate, marginValue,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine')")
                .setParameter(1, preBillId)
                .setParameter(2, d.getItemId())
                .setParameter(3, absQty)
                .setParameter(4, d.getDescription())
                .setParameter(5, absNetValue)
                .setParameter(6, absGrossValue)
                .setParameter(7, d.getRate())
                .setParameter(8, netRate)
                .setParameter(9, d.getDiscountValue())
                .setParameter(10, absQty > 0 ? (d.getDiscountValue() / absQty) : 0.0)
                .setParameter(11, d.getMarginValue())
                .setParameter(12, new Timestamp(createdAt.getTime()))
                .setParameter(13, d.getCreaterId())
                .executeUpdate();
            biPreIds[i] = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();

            // Single-bill flow: this PBI is the ONLY one, so it carries the real
            // cost/retail/purchase values. (The two-bill retail flow zeroes them here
            // because its BilledBill PBI carries them instead.)
            em.createNativeQuery(
                "INSERT INTO " + pharmBillItemTable()
                + " (billItem_ID, itemBatch_ID, stock_ID, qty, stringValue,"
                + " costRate, purchaseRate, retailRate, wholesaleRate, doe, description)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?)")
                .setParameter(1, biPreIds[i])
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
            pbPreIds[i] = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        }

        LOGGER.log(Level.INFO, "[RetailNativeSettle] BillItem+PBI inserted ms={0}", System.currentTimeMillis() - t0);

        // Step 3: Per-item: atomic stock deduction + aggregates + StockHistory (linked to PreBill PBI — matches old flow)
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

            insertStockHistory(pbPreIds[i], item, agg, ampItemId, itemBatchId, departmentId, institutionId);
        }

        // Evict natively-written entity classes from EclipseLink L2 cache
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(StockHistory.class);
        cache.evict(Stock.class);
        cache.evict(BillItem.class);
        cache.evict(Bill.class);
        cache.evict(PreBill.class);

        LOGGER.log(Level.INFO, "[RetailNativeSettle] Stock deducted + history inserted ms={0}", System.currentTimeMillis() - t0);

        // Deviation from legacy (which wrote no finance details for cashier bills):
        // BFD/BIFD are written against the single PreBill so F15's totalRetailSaleValue
        // is correct without backfillBfdForPreToSettleAtCashierBills. Issue #20261.
        double[] billTotals = insertFinanceDetails(preBillId, biPreIds, pbPreIds, items);

        // Step 5: Update totals on the single bill
        em.createNativeQuery(
                "UPDATE " + billTable() + " SET total=?, netTotal=?, DISCOUNT=? WHERE ID=?")
                .setParameter(1, billTotals[0])
                .setParameter(2, billTotals[1])
                .setParameter(3, billTotals[2])
                .setParameter(4, preBillId)
                .executeUpdate();

        LOGGER.log(Level.INFO, "[CashierNativeSettle] DONE items={0} ms={1}",
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
    // Aggregate computation
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

    private void insertStockHistory(long pbId, BillItemData item, StockAggregateResult agg,
                                    long ampItemId, long itemBatchId, long departmentId, long institutionId) {
        Date now = new Date();
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
    }

    // -----------------------------------------------------------------------
    // Finance details (JPA IDENTITY)
    // -----------------------------------------------------------------------

    private double[] insertFinanceDetails(long billId, long[] biIds, long[] pbIds,
                                          List<BillItemData> items) {
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        BigDecimal totalRetailSaleValue = BigDecimal.ZERO;
        BigDecimal totalWholesaleValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalFreeQuantity = BigDecimal.ZERO;
        BigDecimal billNetTotal = BigDecimal.ZERO;
        BigDecimal billGrossTotal = BigDecimal.ZERO;
        BigDecimal billDiscount = BigDecimal.ZERO;

        for (int i = 0; i < items.size(); i++) {
            BillItemData item = items.get(i);

            BigDecimal qty = BigDecimal.valueOf(Math.abs(item.getQty()));
            BigDecimal freeQty = BigDecimal.valueOf(Math.abs(item.getFreeQty()));
            BigDecimal totalQty = qty.add(freeQty);

            BigDecimal retailRate    = BigDecimal.valueOf(item.getRetailRate());
            BigDecimal purchaseRate  = BigDecimal.valueOf(item.getPurchaseRate());
            BigDecimal wholesaleRate = BigDecimal.valueOf(item.getWholesaleRate());
            BigDecimal batchRetail   = BigDecimal.valueOf(item.getBatchRetailRate());
            BigDecimal batchPurchase = BigDecimal.valueOf(item.getBatchPurchaseRate());
            BigDecimal batchWholesale = BigDecimal.valueOf(item.getBatchWholesaleRate());
            BigDecimal costRate = item.getBatchCostRate() != null && item.getBatchCostRate() > 0
                    ? BigDecimal.valueOf(item.getBatchCostRate())
                    : batchPurchase;

            BigDecimal itemCostValue      = costRate.multiply(qty);
            BigDecimal itemRetailValue    = batchRetail.multiply(totalQty);
            BigDecimal itemPurchaseValue  = batchPurchase.multiply(totalQty);
            BigDecimal itemWholesaleValue = batchWholesale.multiply(totalQty);

            BigDecimal netValue   = BigDecimal.valueOf(Math.abs(item.getNetValue()));
            BigDecimal grossValue = BigDecimal.valueOf(Math.abs(item.getGrossValue()));
            BigDecimal lineNetRate = qty.compareTo(BigDecimal.ZERO) > 0
                    ? netValue.divide(qty, 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BillItemFinanceDetails bifd = new BillItemFinanceDetails();
            bifd.setBillItem(em.getReference(BillItem.class, biIds[i]));
            bifd.setCreatedAt(new Date());

            bifd.setLineNetRate(lineNetRate);
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
            em.flush();

            em.createNativeQuery("UPDATE " + billItemTable()
                    + " SET BILLITEMFINANCEDETAILS_ID=? WHERE ID=?")
                    .setParameter(1, bifd.getId())
                    .setParameter(2, biIds[i])
                    .executeUpdate();

            em.createNativeQuery("UPDATE " + pharmBillItemTable()
                    + " SET costRate=?, costValue=?, retailValue=?, purchaseValue=? WHERE ID=?")
                    .setParameter(1, costRate.doubleValue())
                    .setParameter(2, itemCostValue.doubleValue())
                    .setParameter(3, itemRetailValue.doubleValue())
                    .setParameter(4, itemPurchaseValue.doubleValue())
                    .setParameter(5, pbIds[i])
                    .executeUpdate();

            totalCostValue        = totalCostValue.add(itemCostValue);
            totalPurchaseValue    = totalPurchaseValue.add(itemPurchaseValue);
            totalRetailSaleValue  = totalRetailSaleValue.add(itemRetailValue);
            totalWholesaleValue   = totalWholesaleValue.add(itemWholesaleValue);
            totalQuantity         = totalQuantity.add(qty);
            totalFreeQuantity     = totalFreeQuantity.add(freeQty);
            billNetTotal          = billNetTotal.add(netValue);
            billGrossTotal        = billGrossTotal.add(grossValue);
            billDiscount          = billDiscount.add(grossValue.subtract(netValue));
        }

        Bill billRef = em.getReference(Bill.class, billId);
        BillFinanceDetails bfd = new BillFinanceDetails();
        bfd.setBill(billRef);
        bfd.setCreatedAt(new Date());
        bfd.setNetTotal(billNetTotal);
        bfd.setGrossTotal(billGrossTotal);
        bfd.setTotalCostValue(totalCostValue.negate());
        bfd.setTotalPurchaseValue(totalPurchaseValue.negate());
        bfd.setTotalRetailSaleValue(totalRetailSaleValue.negate());
        bfd.setTotalWholesaleValue(totalWholesaleValue.negate());
        bfd.setTotalQuantity(totalQuantity.negate());
        bfd.setTotalFreeQuantity(totalFreeQuantity.negate());
        em.persist(bfd);
        em.flush();

        em.createNativeQuery("UPDATE " + billTable() + " SET BILLFINANCEDETAILS_ID=? WHERE ID=?")
                .setParameter(1, bfd.getId())
                .setParameter(2, billId)
                .executeUpdate();

        return new double[]{billGrossTotal.doubleValue(), billNetTotal.doubleValue(), billDiscount.doubleValue()};
    }

    // -----------------------------------------------------------------------
    // Table name resolution
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

    private static String safeStr(String s) {
        return s != null ? s : "";
    }

    /**
     * Loads view data for an existing settled retail sale bill.
     * Returns Object[]{PrintBillData, List&lt;BillItemData&gt;}.
     * Used by RetailSaleNativeSqlController.viewByBillId to display
     * the print preview without triggering a full entity-graph load.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Object[] loadViewDataByBillId(long billId) {
        Bill bill = em.find(Bill.class, billId);
        if (bill == null) {
            return null;
        }

        PrintBillData pbd = new PrintBillData();
        Department dept = bill.getDepartment();
        if (dept != null) {
            pbd.setDepartmentName(safeStr(dept.getName()));
            pbd.setDepartmentPrintingName(dept.getPrintingName() != null
                    ? dept.getPrintingName() : safeStr(dept.getName()));
            pbd.setDepartmentTelephone1(safeStr(dept.getTelephone1()));
            pbd.setDepartmentTelephone2(safeStr(dept.getTelephone2()));
            pbd.setDepartmentFax(safeStr(dept.getFax()));
            pbd.setDepartmentAddress(safeStr(dept.getAddress()));
            if (dept.getSite() != null) {
                pbd.setDepartmentSiteName(safeStr(dept.getSite().getName()));
            }
            Institution inst = dept.getInstitution();
            if (inst != null) {
                pbd.setInstitutionName(safeStr(inst.getName()));
                pbd.setInstitutionAddress(safeStr(inst.getAddress()));
                pbd.setInstitutionEmail(safeStr(inst.getEmail()));
                pbd.setInstitutionWeb(safeStr(inst.getWeb()));
            }
        }
        pbd.setBillNo(safeStr(bill.getDeptId()));
        pbd.setBillIdStr(safeStr(bill.getIdStr()));
        pbd.setCancelled(bill.isCancelled());
        pbd.setInvoiceNumber(safeStr(bill.getInvoiceNumber()));
        pbd.setCreatedAt(bill.getCreatedAt());
        if (bill.getCreater() != null) {
            pbd.setCreatorName(safeStr(bill.getCreater().getName()));
            pbd.setCreatorCode(safeStr(bill.getCreater().getCode()));
        }
        pbd.setTokenNumber(findTokenNumber(bill));
        if (bill.getPatient() != null && bill.getPatient().getPerson() != null) {
            pbd.setPatientName(safeStr(bill.getPatient().getPerson().getNameWithTitle()));
            // Same rule as the fresh-print path: never emit a bare " / " when neither
            // age nor sex is known, so reprints match freshly printed bills.
            String age = safeStr(bill.getPatient().getPerson().getAgeAsShortString()).trim();
            String sex = bill.getPatient().getPerson().getSex() != null
                    ? safeStr(bill.getPatient().getPerson().getSex().getLabel()).trim() : "";
            if (!age.isEmpty() && !sex.isEmpty()) {
                pbd.setPatientAgeSex(age + " / " + sex);
            } else if (!age.isEmpty()) {
                pbd.setPatientAgeSex(age);
            } else if (!sex.isEmpty()) {
                pbd.setPatientAgeSex(sex);
            }
            pbd.setPatientPhone(safeStr(bill.getPatient().getPerson().getPhone()));
            pbd.setPatientPhn(safeStr(bill.getPatient().getPhn()));
        }
        if (bill.getPaymentMethod() != null) {
            pbd.setPaymentMethodLabel(bill.getPaymentMethod().getLabel());
        }
        if (bill.getPaymentScheme() != null) {
            pbd.setPaymentSchemePrintingName(bill.getPaymentScheme().getPrintingName() != null
                    ? bill.getPaymentScheme().getPrintingName() : safeStr(bill.getPaymentScheme().getName()));
        }
        pbd.setComment(safeStr(bill.getComments()));
        double net = Math.abs(bill.getNetTotal());
        double gross = bill.getTotal() > 0 ? bill.getTotal() : net;
        double disc = Math.max(0, gross - net);
        pbd.setNetTotal(net);
        pbd.setTotal(gross);
        pbd.setDiscount(disc);
        pbd.setDiscountPercentPharmacy(gross > 0 ? (disc / gross) * 100.0 : 0.0);

        // For Multiple Payment Methods, bill.cashPaid stays 0 and the methods are
        // recorded as individual Payment rows. Rebuild the itemised payment lines
        // (and derive the amount paid from their sum) so reprinted/reopened bills
        // match what the settle-time printout shows.
        double cashPaid = bill.getCashPaid();
        double balance = cashPaid - net;
        if (bill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            List<PrintBillData.PaymentLine> lines = buildPrintPaymentLinesFromPersisted(bill);
            pbd.setPayments(lines);
            cashPaid = 0.0;
            for (PrintBillData.PaymentLine line : lines) {
                cashPaid += line.getValue();
            }
            // The split is accepted at settle when within 1.0 of the net total;
            // clamp the reprinted balance to zero within the same tolerance so a
            // settled bill never shows a residual balance.
            balance = cashPaid - net;
            if (Math.abs(balance) <= 1.0) {
                balance = 0.0;
            }
        }
        pbd.setCashPaid(cashPaid);
        pbd.setBalance(balance);

        // Single-bill flow: the items hang off this bill directly. There is no BilledBill
        // and no referenceBill to fall back to (contrast the two-bill retail sale path).
        long itemsBillId = billId;

        String sql = "SELECT i.name, ABS(bi.qty), COALESCE(bi.rate, bi.netRate, 0),"
                + " ABS(bi.netRate), ABS(bi.netValue), ABS(bi.grossValue), ib.dateOfExpire,"
                + " i.code"
                + " FROM " + billItemTable() + " bi"
                + " JOIN " + pharmBillItemTable() + " pbi ON pbi.billItem_ID = bi.ID"
                + " JOIN " + itemBatchTable() + " ib ON ib.ID = pbi.itemBatch_ID"
                + " JOIN " + itemTable() + " i ON i.ID = bi.item_ID"
                + " WHERE bi.bill_ID = ? AND (bi.retired IS NULL OR bi.retired = 0)"
                + " ORDER BY bi.ID";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter(1, itemsBillId)
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
            bid.setItemCode(row[7] != null ? row[7].toString() : "");
            itemList.add(bid);
        }

        return new Object[]{pbd, itemList};
    }

    /**
     * Token number of the pharmacy token issued for this bill, or null when the token
     * system was off for the sale (no token exists). Same lookup as
     * PharmacyPreSettleController.findTokenFromBill(Bill) - bill + SALE_FOR_CASHIER token
     * type - so a reprinted token slip shows the same number the settle-time printout did.
     */
    private String findTokenNumber(Bill bill) {
        List<Token> tokens = em.createQuery(
                "select t from Token t"
                + " where t.bill = :bill"
                + " and t.tokenType = :ty", Token.class)
                .setParameter("bill", bill)
                .setParameter("ty", TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER)
                .setMaxResults(1)
                .getResultList();
        if (tokens.isEmpty()) {
            return null;
        }
        return tokens.get(0).getTokenNumber();
    }

    /**
     * Rebuilds the itemised payment lines for a reprinted/reopened Multiple
     * Payment Methods bill from the persisted {@link Payment} rows. Each line
     * carries the method label, paid value and a method-specific reference
     * (card/cheque/slip/ewallet/online/credit reference) when available.
     */
    private List<PrintBillData.PaymentLine> buildPrintPaymentLinesFromPersisted(Bill bill) {
        List<PrintBillData.PaymentLine> lines = new ArrayList<>();
        List<Payment> payments = em.createQuery(
                "select p from Payment p where p.bill = :bill"
                + " and p.retired = false"
                + " order by p.id", Payment.class)
                .setParameter("bill", bill)
                .getResultList();
        for (Payment p : payments) {
            if (p.getPaymentMethod() == null || p.getPaidValue() <= 0.0) {
                continue;
            }
            String reference = "";
            switch (p.getPaymentMethod()) {
                case Card:
                    reference = safeStr(p.getCreditCardRefNo());
                    break;
                case Cheque:
                    reference = safeStr(p.getChequeRefNo());
                    break;
                case ewallet:
                case Slip:
                case OnlineSettlement:
                case IOU:
                case Credit:
                    reference = safeStr(p.getReferenceNo());
                    break;
                default:
                    break;
            }
            lines.add(new PrintBillData.PaymentLine(
                    p.getPaymentMethod().getLabel(), p.getPaidValue(), reference));
        }
        return lines;
    }
}
