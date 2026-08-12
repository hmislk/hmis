/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dto.BillItemData;
import com.divudi.core.data.dto.PrintBillData;
import com.divudi.core.data.dto.StockAggregateResult;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PreBill;
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
    private volatile String tItem = null;
    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Payment> settle(PreBill preBill, BilledBill saleBill, List<BillItemData> items,
                          PaymentMethod paymentMethod, PaymentMethodData paymentMethodData,
                          PaymentScheme paymentScheme) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No items to settle");
        }

        long t0 = System.currentTimeMillis();

        // Step 1a: Persist PreBill first (bill items will reference its ID).
        preBill.setBillItems(null);
        em.persist(preBill);
        em.flush();
        long preBillId = preBill.getId();

        // Step 1b: Persist BilledBill (payment will reference its ID).
        saleBill.setBillItems(null);
        saleBill.setReferenceBill(preBill);
        em.persist(saleBill);
        em.flush();
        long billId = saleBill.getId();

        // Step 1c: Cross-link PreBill → BilledBill via JPA so the L2 cache stays coherent.
        preBill.setReferenceBill(saleBill);
        em.merge(preBill);
        em.flush();

        LOGGER.log(Level.INFO, "[RetailNativeSettle] PreBill id={0} BilledBill id={1} ms={2}",
                new Object[]{preBillId, billId, System.currentTimeMillis() - t0});

        // Step 2a: Native INSERT BillItem + bare PBI on PreBill (rates only, no cost values — matches old flow PreBill pattern)
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

            // PreBill PBI: rates only, cost/retail/purchase values are 0 (matches old flow)
            em.createNativeQuery(
                "INSERT INTO " + pharmBillItemTable()
                + " (billItem_ID, itemBatch_ID, stock_ID, qty, stringValue,"
                + " costRate, purchaseRate, retailRate, wholesaleRate, doe, description,"
                + " costValue, retailValue, purchaseValue)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,0,0,0)")
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

        // Step 2b: Native INSERT BillItem + populated PBI on BilledBill (matches old flow BilledBill pattern)
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
                + " (bill_ID, item_ID, qty, descreption, netValue, grossValue, Rate, netRate,"
                + " discount, discountRate, marginValue,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine')")
                .setParameter(1, billId)
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
            biIds[i] = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();

            // BilledBill PBI: fully populated (matches old flow BilledBill PBI)
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
        cache.evict(BilledBill.class);

        LOGGER.log(Level.INFO, "[RetailNativeSettle] Stock deducted + history inserted ms={0}", System.currentTimeMillis() - t0);

        // Step 4: Finance details on BilledBill (BIFD on BilledBill items, BFD on BilledBill — matches old flow)
        double[] billTotals = insertFinanceDetails(billId, biIds, pbIds, items);

        // Step 5: Update totals on both bills
        em.createNativeQuery(
                "UPDATE " + billTable() + " SET total=?, netTotal=?, DISCOUNT=? WHERE ID=? OR ID=?")
                .setParameter(1, billTotals[0])
                .setParameter(2, billTotals[1])
                .setParameter(3, billTotals[2])
                .setParameter(4, preBillId)
                .setParameter(5, billId)
                .executeUpdate();

        // Step 6: Insert Payment record(s) against BilledBill. A single payment
        // for ordinary methods; one Payment per component for MultiplePaymentMethods.
        List<Payment> payments = insertPayment(saleBill, billId, billTotals[1], paymentMethod, paymentMethodData, paymentScheme);

        // Both bills were persisted with billItems nulled (steps 1a/1b) because the items are
        // inserted natively, so the in-memory entities claim they have none — and
        // Bill.getBillItems() hands out an empty list rather than a lazy one. Anything that
        // later merges these instances writes that empty collection into the shared cache, so
        // every subsequent read of the bill shows a bill with no items (a reprint printing
        // "No of Items: 0" while the total is correct). Issue #22511, same cause as #22506.
        // Refreshing rebuilds the lazy collections from the rows just written, and also picks
        // up the totals updated above.
        em.refresh(preBill);
        em.refresh(saleBill);

        LOGGER.log(Level.INFO, "[RetailNativeSettle] DONE items={0} ms={1}",
                new Object[]{items.size(), System.currentTimeMillis() - t0});

        return payments;
    }

    // -----------------------------------------------------------------------
    // Payment
    // -----------------------------------------------------------------------

    private List<Payment> insertPayment(Bill bill, long billId, double netTotal,
                                   PaymentMethod paymentMethod, PaymentMethodData pmd,
                                   PaymentScheme paymentScheme) {
        // MultiplePaymentMethods: persist one Payment per component (Cash, Card,
        // Cheque, ...), each carrying its own entered value and reference fields.
        // Mirrors PharmacySaleController.createMultiplePayments for parity with
        // the legacy retail sale page.
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            return insertMultiplePayments(bill, billId, pmd);
        }

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
        List<Payment> result = new ArrayList<>();
        result.add(p);
        return result;
    }

    /**
     * Persists one {@link Payment} per selected component of a
     * MultiplePaymentMethods bill. Each component's own entered value and
     * method-specific reference fields are copied onto its Payment, mirroring
     * {@code PharmacySaleController.createMultiplePayments}. Components with no
     * payment method or no data are skipped.
     */
    private List<Payment> insertMultiplePayments(Bill bill, long billId, PaymentMethodData pmd) {
        List<Payment> result = new ArrayList<>();
        if (pmd == null
                || pmd.getPaymentMethodMultiple() == null
                || pmd.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
            return result;
        }
        for (ComponentDetail cd : pmd.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
            if (cd == null || cd.getPaymentMethod() == null || cd.getPaymentMethodData() == null) {
                continue;
            }
            Payment p = new Payment();
            p.setBill(em.getReference(Bill.class, billId));
            p.setInstitution(bill.getInstitution());
            p.setDepartment(bill.getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(bill.getCreater());
            p.setPaymentMethod(cd.getPaymentMethod());

            PaymentMethodData cpmd = cd.getPaymentMethodData();
            switch (cd.getPaymentMethod()) {
                case Card:
                    p.setBank(cpmd.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(cpmd.getCreditCard().getNo());
                    p.setPaidValue(cpmd.getCreditCard().getTotalValue());
                    p.setComments(cpmd.getCreditCard().getComment());
                    break;
                case Cheque:
                    p.setChequeDate(cpmd.getCheque().getDate());
                    p.setChequeRefNo(cpmd.getCheque().getNo());
                    p.setBank(cpmd.getCheque().getInstitution());
                    p.setPaidValue(cpmd.getCheque().getTotalValue());
                    p.setComments(cpmd.getCheque().getComment());
                    break;
                case Cash:
                    p.setPaidValue(cpmd.getCash().getTotalValue());
                    break;
                case ewallet:
                    p.setPaidValue(cpmd.getEwallet().getTotalValue());
                    p.setPolicyNo(cpmd.getEwallet().getReferralNo());
                    p.setComments(cpmd.getEwallet().getComment());
                    p.setReferenceNo(cpmd.getEwallet().getReferenceNo());
                    p.setBank(cpmd.getEwallet().getInstitution());
                    break;
                case Agent:
                case PatientDeposit:
                    p.setPaidValue(cpmd.getPatient_deposit().getTotalValue());
                    break;
                case Credit:
                    p.setPolicyNo(cpmd.getCredit().getReferralNo());
                    p.setReferenceNo(cpmd.getCredit().getReferenceNo());
                    p.setCreditCompany(cpmd.getCredit().getInstitution());
                    p.setPaidValue(cpmd.getCredit().getTotalValue());
                    p.setComments(cpmd.getCredit().getComment());
                    break;
                case Slip:
                    p.setPaidValue(cpmd.getSlip().getTotalValue());
                    p.setBank(cpmd.getSlip().getInstitution());
                    p.setRealizedAt(cpmd.getSlip().getDate());
                    p.setPaymentDate(cpmd.getSlip().getDate());
                    p.setComments(cpmd.getSlip().getComment());
                    p.setReferenceNo(cpmd.getSlip().getReferenceNo());
                    break;
                case OnlineSettlement:
                    p.setPaidValue(cpmd.getOnlineSettlement().getTotalValue());
                    p.setBank(cpmd.getOnlineSettlement().getInstitution());
                    p.setRealizedAt(cpmd.getOnlineSettlement().getDate());
                    p.setPaymentDate(cpmd.getOnlineSettlement().getDate());
                    p.setReferenceNo(cpmd.getOnlineSettlement().getReferenceNo());
                    p.setComments(cpmd.getOnlineSettlement().getComment());
                    break;
                case IOU:
                    p.setPaidValue(cpmd.getIou().getTotalValue());
                    p.setBank(cpmd.getIou().getInstitution());
                    p.setRealizedAt(cpmd.getIou().getDate());
                    p.setPaymentDate(cpmd.getIou().getDate());
                    p.setReferenceNo(cpmd.getIou().getReferenceNo());
                    p.setComments(cpmd.getIou().getComment());
                    break;
                case Staff:
                    p.setPaidValue(cpmd.getStaffCredit().getTotalValue());
                    if (cpmd.getStaffCredit().getToStaff() != null) {
                        p.setToStaff(cpmd.getStaffCredit().getToStaff());
                        if (bill.getToStaff() == null) {
                            bill.setToStaff(cpmd.getStaffCredit().getToStaff());
                        }
                    }
                    break;
                case Staff_Welfare:
                    p.setPaidValue(cpmd.getStaffWelfare().getTotalValue());
                    if (cpmd.getStaffWelfare().getToStaff() != null) {
                        p.setToStaff(cpmd.getStaffWelfare().getToStaff());
                        if (bill.getToStaff() == null) {
                            bill.setToStaff(cpmd.getStaffWelfare().getToStaff());
                        }
                    }
                    break;
                default:
                    break;
            }

            // Skip components with no entered value — the controller has already
            // validated that the selected components cover the net total, so a
            // zero here is an unfilled row that should not create a Payment.
            if (p.getPaidValue() <= 0.0) {
                continue;
            }
            em.persist(p);
            result.add(p);
        }
        em.flush();
        return result;
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
            pbd.setDepartmentAddress(safeStr(dept.getAddress()));
            Institution inst = dept.getInstitution();
            if (inst != null) {
                pbd.setInstitutionName(safeStr(inst.getName()));
                pbd.setInstitutionAddress(safeStr(inst.getAddress()));
                pbd.setInstitutionEmail(safeStr(inst.getEmail()));
                pbd.setInstitutionWeb(safeStr(inst.getWeb()));
            }
        }
        pbd.setBillNo(safeStr(bill.getDeptId()));
        pbd.setCreatedAt(bill.getCreatedAt());
        if (bill.getCreater() != null) {
            pbd.setCreatorName(safeStr(bill.getCreater().getName()));
        }
        if (bill.getPatient() != null && bill.getPatient().getPerson() != null) {
            pbd.setPatientName(safeStr(bill.getPatient().getPerson().getNameWithTitle()));
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

        // Items are on the BilledBill. For legacy plain-Bill records (pre two-bill structure)
        // or PreBill IDs passed in, fall back to referenceBill if the bill has no items.
        long itemsBillId = billId;

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
            itemList.add(bid);
        }

        return new Object[]{pbd, itemList};
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
