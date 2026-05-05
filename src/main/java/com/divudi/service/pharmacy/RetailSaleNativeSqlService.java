/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dto.BillItemData;
import com.divudi.core.data.dto.StockAggregateResult;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.StockHistory;
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
 * Persists pharmacy retail sale bills using native SQL for the hot-path
 * operations (stock deduction, aggregates, StockHistory INSERT, Payment INSERT).
 *
 * BillItem and PharmaceuticalBillItem are persisted via native SQL;
 * BillItemFinanceDetails and BillFinanceDetails via JPA (IDENTITY PKs).
 *
 * Avoids the dominant cold-start cost:
 *  - EAGER cascade Stock → ItemBatch → Item on PharmacySaleController settle
 *  - 9 separate JPQL aggregate queries per item for StockHistory values
 *
 * Patterned on InpatientDirectIssueNativeSqlService (issue #20214).
 * Issue: #20260
 */
@Stateless
public class RetailSaleNativeSqlService {

    private static final Logger LOGGER = Logger.getLogger(RetailSaleNativeSqlService.class.getName());

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
    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void settle(Bill bill, List<BillItemData> items,
                       PaymentMethod paymentMethod, PaymentMethodData paymentMethodData,
                       PaymentScheme paymentScheme) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No items to settle");
        }

        long t0 = System.currentTimeMillis();

        // Step 1: Persist bill header via JPA (gets DTYPE + IDENTITY ID).
        bill.setBillItems(null);
        em.persist(bill);
        em.flush();
        long billId = bill.getId();

        LOGGER.log(Level.INFO, "[RetailNativeSettle] Bill header persisted id={0} ms={1}",
                new Object[]{billId, System.currentTimeMillis() - t0});

        // Step 2: Native INSERT BillItem + PharmaceuticalBillItem per line item
        long[] biIds = new long[items.size()];
        long[] pbIds = new long[items.size()];

        for (int i = 0; i < items.size(); i++) {
            BillItemData d = items.get(i);
            Date createdAt = d.getCreatedAt() != null ? d.getCreatedAt() : new Date();

            double absQty       = Math.abs(d.getQty());
            double absNetValue  = Math.abs(d.getNetValue());
            double absGrossValue = Math.abs(d.getGrossValue());
            double netRate      = absQty > 0 ? absNetValue / absQty : 0.0;

            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (bill_ID, item_ID, qty, descreption, netValue, grossValue, netRate,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType)"
                + " VALUES (?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine')")
                .setParameter(1, billId)
                .setParameter(2, d.getItemId())
                .setParameter(3, absQty)
                .setParameter(4, d.getDescription())
                .setParameter(5, absNetValue)
                .setParameter(6, absGrossValue)
                .setParameter(7, netRate)
                .setParameter(8, new Timestamp(createdAt.getTime()))
                .setParameter(9, d.getCreaterId())
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

        LOGGER.log(Level.INFO, "[RetailNativeSettle] BillItem+PBI inserted ms={0}", System.currentTimeMillis() - t0);

        // Step 3: Per-item: atomic stock deduction + aggregates + StockHistory
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

        // Evict natively-written entity classes from EclipseLink L2 cache
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(StockHistory.class);
        cache.evict(Stock.class);
        cache.evict(BillItem.class);
        cache.evict(Bill.class);

        LOGGER.log(Level.INFO, "[RetailNativeSettle] Stock deducted + history inserted ms={0}", System.currentTimeMillis() - t0);

        // Step 4: Finance details (JPA IDENTITY PKs — one BillItemFinanceDetails per line)
        double[] billTotals = insertFinanceDetails(billId, biIds, pbIds, items);

        // Step 5: Update bill-level totals
        em.createNativeQuery(
                "UPDATE " + billTable() + " SET total=?, netTotal=? WHERE ID=?")
                .setParameter(1, billTotals[0])
                .setParameter(2, billTotals[1])
                .setParameter(3, billId)
                .executeUpdate();

        // Step 6: Insert Payment record
        insertPayment(bill, billId, billTotals[1], paymentMethod, paymentMethodData, paymentScheme);

        LOGGER.log(Level.INFO, "[RetailNativeSettle] DONE items={0} ms={1}",
                new Object[]{items.size(), System.currentTimeMillis() - t0});
    }

    // -----------------------------------------------------------------------
    // Payment
    // -----------------------------------------------------------------------

    private void insertPayment(Bill bill, long billId, double netTotal,
                                PaymentMethod paymentMethod, PaymentMethodData pmd,
                                PaymentScheme paymentScheme) {
        Payment p = new Payment();
        p.setBill(em.getReference(Bill.class, billId));
        p.setInstitution(bill.getInstitution());
        p.setDepartment(bill.getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(bill.getCreater());
        p.setPaymentMethod(paymentMethod);
        p.setPaidValue(netTotal);

        if (paymentMethod != null && pmd != null) {
            switch (paymentMethod) {
                case Card:
                    p.setBank(pmd.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(pmd.getCreditCard().getNo());
                    p.setComments(pmd.getCreditCard().getComment());
                    break;
                case Cheque:
                    p.setChequeDate(pmd.getCheque().getDate());
                    p.setChequeRefNo(pmd.getCheque().getNo());
                    p.setBank(pmd.getCheque().getInstitution());
                    p.setComments(pmd.getCheque().getComment());
                    break;
                case ewallet:
                    p.setPolicyNo(pmd.getEwallet().getReferralNo());
                    p.setComments(pmd.getEwallet().getComment());
                    p.setReferenceNo(pmd.getEwallet().getReferenceNo());
                    p.setBank(pmd.getEwallet().getInstitution());
                    break;
                case Credit:
                    p.setPolicyNo(pmd.getCredit().getReferralNo());
                    p.setReferenceNo(pmd.getCredit().getReferenceNo());
                    p.setCreditCompany(pmd.getCredit().getInstitution());
                    p.setComments(pmd.getCredit().getComment());
                    break;
                case PatientDeposit:
                case Agent:
                    break;
                case Slip:
                    p.setBank(pmd.getSlip().getInstitution());
                    p.setRealizedAt(pmd.getSlip().getDate());
                    p.setPaymentDate(pmd.getSlip().getDate());
                    p.setComments(pmd.getSlip().getComment());
                    p.setReferenceNo(pmd.getSlip().getReferenceNo());
                    break;
                case OnlineSettlement:
                    p.setBank(pmd.getOnlineSettlement().getInstitution());
                    p.setRealizedAt(pmd.getOnlineSettlement().getDate());
                    p.setPaymentDate(pmd.getOnlineSettlement().getDate());
                    p.setReferenceNo(pmd.getOnlineSettlement().getReferenceNo());
                    p.setComments(pmd.getOnlineSettlement().getComment());
                    break;
                case IOU:
                    p.setBank(pmd.getIou().getInstitution());
                    p.setRealizedAt(pmd.getIou().getDate());
                    p.setPaymentDate(pmd.getIou().getDate());
                    p.setReferenceNo(pmd.getIou().getReferenceNo());
                    p.setComments(pmd.getIou().getComment());
                    break;
                case Staff:
                    p.setToStaff(pmd.getStaffCredit().getToStaff());
                    break;
                case Staff_Welfare:
                    p.setToStaff(pmd.getStaffWelfare().getToStaff());
                    break;
                default:
                    break;
            }
        }

        em.persist(p);
        em.flush();
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
        }

        Bill billRef = em.getReference(Bill.class, billId);
        BillFinanceDetails bfd = new BillFinanceDetails();
        bfd.setBill(billRef);
        bfd.setCreatedAt(new Date());
        bfd.setNetTotal(billNetTotal);
        bfd.setGrossTotal(billNetTotal);
        bfd.setTotalCostValue(totalCostValue);
        bfd.setTotalPurchaseValue(totalPurchaseValue);
        bfd.setTotalRetailSaleValue(totalRetailSaleValue);
        bfd.setTotalWholesaleValue(totalWholesaleValue);
        bfd.setTotalQuantity(totalQuantity);
        bfd.setTotalFreeQuantity(totalFreeQuantity);
        em.persist(bfd);
        em.flush();

        em.createNativeQuery("UPDATE " + billTable() + " SET BILLFINANCEDETAILS_ID=? WHERE ID=?")
                .setParameter(1, bfd.getId())
                .setParameter(2, billId)
                .executeUpdate();

        return new double[]{billNetTotal.doubleValue(), billNetTotal.doubleValue()};
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
}
