package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.StockHistory;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFinanceDetailsFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * Single implementation for reconstructing missing or zeroed BillFinanceDetails
 * on historical pharmacy adjustment bills, so the F15 Daily Stock Values report
 * (which reads BillFinanceDetails, not bill.netTotal) stops showing Rs. 0.00 for
 * real stock and price movements.
 *
 * <h3>Why the derivation is not a simple formula</h3>
 *
 * Two code paths write adjustment bills, and until issue #23411 they disagreed
 * about what {@code PharmaceuticalBillItem.beforeAdjustmentValue} /
 * {@code afterAdjustmentValue} mean on a <em>rate</em> adjustment:
 *
 * <ul>
 *   <li><b>RATE semantics</b> — the UI page
 *       ({@code PharmacyAdjustmentController.saveRsrAdjustmentBillItems}) stores the
 *       old and new <em>unit rates</em>. The line's value change is
 *       {@code (after - before) * billItem.qty}.</li>
 *   <li><b>VALUE semantics</b> — the adjustment REST API
 *       ({@code PharmacyAdjustmentApiService.createRetailRateAdjustmentBillItem} before
 *       #23411) stored {@code stockQty * rate}, i.e. the extended value on each side.
 *       The line's value change is {@code (after - before)} with no further multiplication.</li>
 * </ul>
 *
 * Applying one convention's formula to the other's data is not a rounding error: on
 * coop production it overstated a single bill by 1085x. Because both conventions
 * coexist in already-persisted data at some sites, this service does not assume
 * either one and does not use a date cutoff. It <em>resolves</em> the convention per
 * line by checking which interpretation reconciles with {@code billItem.netValue} —
 * the signed change value, which both writers persist correctly — and refuses to
 * guess when neither does. See {@link AuditValueSemantics}.
 *
 * The API writer was converged onto RATE semantics as part of #23411, so no new
 * VALUE-semantics rows are created; the resolver remains because the existing rows
 * are permanent.
 *
 * <h3>Consolidation</h3>
 *
 * This service replaces three divergent implementations that each hard-coded one
 * writer's convention:
 * {@code DataAdministrationController.backfillBfdForStockAdjustmentBills()} (button,
 * stock adjustments only), this service's own former RATE-only formula, and
 * {@code PharmacyAdjustmentApiService.backfillFinanceDetails()} (VALUE-only, but the
 * source of the dry-run mode and the StockHistory rate resolution kept here).
 *
 * Issue #23411. Related: #22580, #18774.
 */
@Stateless
public class PharmacyBfdBackfillService {

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillFinanceDetailsFacade billFinanceDetailsFacade;

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    /**
     * Bill types this service can reconstruct. Rate adjustments other than retail and
     * purchase are excluded because no writer populates the audit fields for them.
     */
    private static final List<BillTypeAtomic> SUPPORTED_TYPES = Arrays.asList(
            BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT,
            BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT,
            BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT);

    /** Base note for a computed-but-unsaved result. */
    public static final String DRY_RUN_NOTE = "Dry run: not persisted";

    /**
     * Appended whenever a cost or purchase value had to come from the item batch's CURRENT
     * rate because no point-in-time snapshot exists. The retail value is exact; these are
     * estimates, and saying so is the difference between a figure and a guess.
     */
    public static final String APPROXIMATION_NOTE =
            ". Cost and purchase values approximated using current item batch rates"
            + " (no historical rate snapshot exists for this bill)";

    /**
     * How a rate-adjustment line's before/after audit values are to be read.
     */
    public enum AuditValueSemantics {
        /** before/after hold unit rates; line change = (after - before) * qty. */
        RATE,
        /** before/after hold extended values (qty * rate); line change = (after - before). */
        VALUE,
        /** before/after hold quantities (stock adjustments); line change = qtyDelta * rate. */
        QUANTITY,
        /** Neither interpretation reconciles with billItem.netValue — do not guess. */
        UNRESOLVED
    }

    // -------------------------------------------------------------------------
    // Public entry points
    // -------------------------------------------------------------------------

    /**
     * Preview or apply a backfill over a date range.
     *
     * @param billTypeAtomics types to process; null or empty means all supported types
     * @param departmentId    department filter, null for all departments
     * @param fromDate        start date, widened to 00:00:00.000
     * @param toDate          end date, widened to 23:59:59.999
     * @param apply           false to compute and report without persisting anything
     * @param auditComment    audit trail comment, recorded on each changed bill
     * @param approvedBy      who authorised the correction
     * @param performedBy     the user running it
     */
    public BackfillReport backfillAdjustmentBfds(
            List<String> billTypeAtomics,
            Long departmentId,
            Date fromDate,
            Date toDate,
            boolean apply,
            String auditComment,
            String approvedBy,
            WebUser performedBy) {

        List<BillTypeAtomic> types = resolveTypes(billTypeAtomics);
        BackfillReport report = new BackfillReport();
        report.setDryRun(!apply);

        List<Bill> bills = findCandidateBills(types, departmentId, fromDate, toDate);
        report.setCandidatesFound(bills.size());

        for (Bill bill : bills) {
            report.add(applyToBill(bill, apply, auditComment, approvedBy, performedBy));
        }

        return report;
    }

    /**
     * Compute (and optionally persist) the corrected finance details for one already-loaded
     * bill. Callers that have a Bill in hand — the adjustment API's per-bill endpoint —
     * use this directly rather than going back through a date-range query.
     *
     * <p>Never throws: a failure on one bill is reported as an ERROR row so a run over
     * thousands of bills is not lost to a single bad one.</p>
     */
    public BillBackfillResult applyToBill(Bill bill, boolean apply, String auditComment,
            String approvedBy, WebUser performedBy) {
        try {
            if (!needsCorrection(bill)) {
                BillBackfillResult result = new BillBackfillResult(bill);
                result.setStatus(BackfillStatus.SKIPPED);
                result.setNote("Skipped: BillFinanceDetails already present and populated");
                return result;
            }
            return processBill(bill, apply, auditComment, approvedBy, performedBy);
        } catch (Exception ex) {
            BillBackfillResult result = new BillBackfillResult(bill);
            result.setStatus(BackfillStatus.ERROR);
            result.setNote(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return result;
        }
    }

    /**
     * A bill needs repair when it has no BillFinanceDetails, when its BFD carries no retail
     * value, or when bill.total was never written.
     *
     * <p>The last two cases matter: the admin button used to select on
     * {@code billFinanceDetails IS NULL} alone, which on coop production matched 1 of the 27
     * affected retail-rate bills — the other 26 had a BFD row and a bill.total of 0, so they
     * survived every backfill run and stayed at 0.00 in F15.</p>
     */
    public boolean needsCorrection(Bill bill) {
        if (bill == null) {
            return false;
        }
        // hasBillFinanceDetails(), never getBillFinanceDetails(): the getter auto-creates
        // and attaches a BillFinanceDetails on first call, so merely asking the question
        // through it would mutate the bill — including during a dry run.
        if (!bill.hasBillFinanceDetails()) {
            return true;
        }
        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        // netTotal, not totalRetailSaleValue: every writer puts the bill's primary value
        // change in netTotal, whichever dimension that is. A purchase rate adjustment
        // legitimately carries totalRetailSaleValue = 0 (it moves purchase value only), so
        // testing the retail column would mark every correctly written purchase-rate bill
        // as broken and rewrite it on every run.
        if (bfd.getNetTotal() == null || bfd.getNetTotal().compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }
        return bill.getTotal() == 0;
    }

    /**
     * Backwards-compatible entry point for {@code POST /api/pharmacy/backfill_bfd},
     * which existed before the dry-run mode was added and expects a flat summary map.
     * Always applies. New callers should use the {@link BackfillReport} overload.
     */
    public Map<String, Object> backfillAdjustmentBfds(
            List<String> billTypeAtomics,
            Long departmentId,
            Date fromDate,
            Date toDate,
            String auditComment,
            String approvedBy,
            WebUser apiUser) {

        BackfillReport report = backfillAdjustmentBfds(billTypeAtomics, departmentId,
                fromDate, toDate, true, auditComment, approvedBy, apiUser);

        Map<String, Object> summary = new HashMap<>();
        summary.put("backfilledBills", report.getBackfilled());
        summary.put("skipped", report.getSkipped());
        summary.put("unresolved", report.getUnresolved());
        summary.put("errors", report.getErrorMessages());
        return summary;
    }

    // -------------------------------------------------------------------------
    // Candidate selection
    // -------------------------------------------------------------------------

    /**
     * A bill needs repair when it has no BillFinanceDetails at all, when its BFD carries
     * no net value, or when bill.total was never written. The last two cases were missed
     * by the button's {@code billFinanceDetails IS NULL} filter, which is why bills with a
     * present-but-empty BFD stayed at 0.00 through repeated backfill runs.
     *
     * <p>The test is on {@code bfd.netTotal}, the bill's primary value change, never on
     * {@code totalRetailSaleValue}: a purchase rate adjustment moves purchase value only
     * and so carries a retail value of 0 by design.</p>
     */
    private List<Bill> findCandidateBills(List<BillTypeAtomic> types, Long departmentId,
            Date fromDate, Date toDate) {

        Date from = CommonFunctions.getStartOfDay(fromDate);
        Date to = CommonFunctions.getEndOfDay(toDate);

        String jpql = "select b from Bill b"
                + " left join b.billFinanceDetails bfd"
                + " where b.retired = false"
                + " and b.billTypeAtomic in :types"
                + " and b.createdAt between :from and :to"
                + " and (bfd is null"
                + "      or bfd.netTotal is null"
                + "      or bfd.netTotal = 0"
                + "      or b.total = 0)"
                + (departmentId != null ? " and b.department.id = :deptId" : "")
                + " order by b.createdAt";

        Map<String, Object> params = new HashMap<>();
        params.put("types", types);
        params.put("from", from);
        params.put("to", to);
        if (departmentId != null) {
            params.put("deptId", departmentId);
        }

        List<Bill> bills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return bills == null ? new ArrayList<>() : bills;
    }

    private List<BillTypeAtomic> resolveTypes(List<String> typeNames) {
        if (typeNames == null || typeNames.isEmpty()) {
            return SUPPORTED_TYPES;
        }
        List<BillTypeAtomic> result = new ArrayList<>();
        for (String name : typeNames) {
            BillTypeAtomic bta;
            try {
                bta = BillTypeAtomic.valueOf(name.trim());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unknown billTypeAtomic: " + name);
            }
            if (!SUPPORTED_TYPES.contains(bta)) {
                throw new IllegalArgumentException("Unsupported billTypeAtomic for backfill: "
                        + name + ". Supported types are " + SUPPORTED_TYPES + ".");
            }
            result.add(bta);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Per-bill computation
    // -------------------------------------------------------------------------

    private BillBackfillResult processBill(Bill bill, boolean apply, String auditComment,
            String approvedBy, WebUser performedBy) {

        BillBackfillResult result = new BillBackfillResult(bill);

        List<BillItem> billItems = bill.getBillItems();
        if (billItems == null || billItems.isEmpty()) {
            result.setStatus(BackfillStatus.SKIPPED);
            result.setNote("Bill has no bill items");
            return result;
        }

        BigDecimal retailValue = BigDecimal.ZERO;
        BigDecimal costValue = BigDecimal.ZERO;
        BigDecimal purchaseValue = BigDecimal.ZERO;
        BigDecimal primaryValue = BigDecimal.ZERO;
        BigDecimal grossValue = BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal beforeValue = BigDecimal.ZERO;
        BigDecimal afterValue = BigDecimal.ZERO;

        int lines = 0;
        boolean ratesApproximated = false;

        for (BillItem billItem : billItems) {
            if (billItem == null || billItem.isRetired()) {
                continue;
            }
            PharmaceuticalBillItem pbi = billItem.getPharmaceuticalBillItem();
            if (pbi == null) {
                continue;
            }

            LineComputation line = computeLine(bill.getBillTypeAtomic(), billItem, pbi);

            if (line.getSemantics() == AuditValueSemantics.UNRESOLVED) {
                // One bad line makes the whole bill's total untrustworthy. Report it
                // rather than persisting a partial figure that looks authoritative.
                result.setStatus(BackfillStatus.UNRESOLVED);
                result.setSemantics(AuditValueSemantics.UNRESOLVED);
                result.setNote("Bill item " + billItem.getId() + ": " + line.getNote());
                return result;
            }

            retailValue = retailValue.add(line.getRetailValue());
            costValue = costValue.add(line.getCostValue());
            purchaseValue = purchaseValue.add(line.getPurchaseValue());
            primaryValue = primaryValue.add(line.getPrimaryValue());
            grossValue = grossValue.add(line.getPrimaryValue().abs());
            quantity = quantity.add(line.getQuantity().abs());
            beforeValue = beforeValue.add(line.getBeforeValue());
            afterValue = afterValue.add(line.getAfterValue());
            ratesApproximated |= line.isRatesApproximated();
            result.setSemantics(line.getSemantics());
            lines++;
        }

        if (lines == 0) {
            result.setStatus(BackfillStatus.SKIPPED);
            result.setNote("No usable pharmaceutical bill items on this bill");
            return result;
        }

        if (grossValue.compareTo(BigDecimal.ZERO) == 0) {
            result.setStatus(BackfillStatus.SKIPPED);
            result.setNote("Computed value is zero — nothing to correct");
            return result;
        }

        result.setComputedRetailValue(retailValue);
        result.setComputedCostValue(costValue);
        result.setComputedPurchaseValue(purchaseValue);
        result.setComputedGrossTotal(grossValue);
        result.setComputedNetTotal(primaryValue);
        result.setComputedQuantity(quantity);
        result.setRatesApproximated(ratesApproximated);

        if (!apply) {
            result.setStatus(BackfillStatus.WOULD_UPDATE);
            result.setNote(ratesApproximated
                    ? DRY_RUN_NOTE + APPROXIMATION_NOTE
                    : DRY_RUN_NOTE);
            return result;
        }

        boolean isNew = !bill.hasBillFinanceDetails();
        BillFinanceDetails bfd;
        if (isNew) {
            bfd = new BillFinanceDetails(bill);
            bill.setBillFinanceDetails(bfd);
        } else {
            bfd = bill.getBillFinanceDetails();
        }

        bfd.setGrossTotal(grossValue);
        bfd.setNetTotal(primaryValue);
        bfd.setTotalRetailSaleValue(retailValue);
        bfd.setTotalCostValue(costValue);
        bfd.setTotalPurchaseValue(purchaseValue);
        bfd.setTotalQuantity(quantity);
        bfd.setTotalBeforeAdjustmentValue(beforeValue);
        bfd.setTotalAfterAdjustmentValue(afterValue);
        if (bfd.getTotalWholesaleValue() == null) {
            bfd.setTotalWholesaleValue(BigDecimal.ZERO);
        }

        // bill.total / bill.netTotal are written too. The F15 BillLight query falls back
        // to the BFD when bill.total is 0, but every other report that reads bill.total
        // directly stays at zero unless these are set.
        bill.setTotal(grossValue.doubleValue());
        bill.setNetTotal(primaryValue.doubleValue());

        appendAuditLog(bill, auditComment, approvedBy, performedBy, isNew,
                result.getSemantics(), grossValue, primaryValue);

        if (isNew) {
            billFinanceDetailsFacade.create(bfd);
        } else {
            billFinanceDetailsFacade.edit(bfd);
        }
        billFacade.edit(bill);

        result.setStatus(BackfillStatus.UPDATED);
        result.setNote((isNew ? "Created new BFD" : "Updated existing BFD")
                + (ratesApproximated ? APPROXIMATION_NOTE : ""));
        return result;
    }

    // -------------------------------------------------------------------------
    // Per-line computation and semantics resolution
    // -------------------------------------------------------------------------

    private LineComputation computeLine(BillTypeAtomic bta, BillItem billItem, PharmaceuticalBillItem pbi) {
        if (bta == BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT) {
            return computeStockAdjustmentLine(billItem, pbi);
        }
        return computeRateAdjustmentLine(bta, billItem, pbi);
    }

    /**
     * Stock adjustments move quantity at a fixed rate. Both writers agree here:
     * before/after hold quantities and {@code pbi.qty} holds the signed delta between
     * them, so no semantics resolution is needed — but the two are cross-checked, and a
     * disagreement is reported rather than silently resolved in favour of one.
     */
    private LineComputation computeStockAdjustmentLine(BillItem billItem, PharmaceuticalBillItem pbi) {
        LineComputation line = new LineComputation();

        double beforeQty = pbi.getBeforeAdjustmentValue();
        double afterQty = pbi.getAfterAdjustmentValue();
        double qtyDeltaFromAudit = afterQty - beforeQty;
        double qtyDelta = pbi.getQty();

        if (qtyDelta == 0.0 && qtyDeltaFromAudit != 0.0) {
            qtyDelta = qtyDeltaFromAudit;
        } else if (qtyDelta != 0.0 && qtyDeltaFromAudit != 0.0
                && !reconciles(qtyDelta, qtyDeltaFromAudit)) {
            line.setSemantics(AuditValueSemantics.UNRESOLVED);
            line.setNote("Quantity delta disagrees with the before/after audit values"
                    + " (pbi.qty=" + qtyDelta + ", after-before=" + qtyDeltaFromAudit + ")");
            return line;
        }

        if (qtyDelta == 0.0) {
            line.setSemantics(AuditValueSemantics.QUANTITY);
            return line; // zero-quantity adjustment contributes nothing
        }

        StockHistory snapshot = stockHistoryFacade.findByPharmaceuticalBillItem(pbi);
        ItemBatch itemBatch = pbi.getItemBatch();

        ResolvedRate retail = resolveRetailRate(pbi, snapshot, billItem, itemBatch);
        ResolvedRate cost = resolveCostRate(pbi, snapshot, itemBatch);
        ResolvedRate purchase = resolvePurchaseRate(snapshot, itemBatch);
        double retailRate = retail.rate;
        double costRate = cost.rate;
        double purchaseRate = purchase.rate;

        if (retailRate == 0.0) {
            line.setSemantics(AuditValueSemantics.UNRESOLVED);
            line.setNote("No retail rate available on the bill item, its stock history"
                    + " snapshot or its item batch — cannot value this adjustment");
            return line;
        }

        line.setSemantics(AuditValueSemantics.QUANTITY);
        line.setPrimaryValue(BigDecimal.valueOf(qtyDelta * retailRate));
        line.setRetailValue(BigDecimal.valueOf(qtyDelta * retailRate));
        line.setCostValue(BigDecimal.valueOf(qtyDelta * costRate));
        line.setPurchaseValue(BigDecimal.valueOf(qtyDelta * purchaseRate));
        line.setQuantity(BigDecimal.valueOf(qtyDelta));
        line.setBeforeValue(BigDecimal.valueOf(beforeQty * retailRate));
        line.setAfterValue(BigDecimal.valueOf(afterQty * retailRate));
        // Disclose when a value actually came from a CURRENT batch rate. Testing
        // "no snapshot existed" instead both over- and under-reports: a point-in-time rate
        // on the bill item needs no disclosure, while a snapshot that happens to carry a
        // zero cost rate falls through to the current rate and does.
        line.setRatesApproximated(retail.fromCurrentBatchRate
                || cost.fromCurrentBatchRate
                || purchase.fromCurrentBatchRate);
        return line;
    }

    /**
     * Rate adjustments hold either unit rates or extended values in before/after,
     * depending on which writer created the bill. {@code billItem.netValue} is the
     * signed change value and is written correctly by both, so it is used as the
     * referee: whichever interpretation reproduces it is the one in force.
     *
     * When neither reproduces it the line is left UNRESOLVED. That is the case for
     * bills from the legacy single-item page, which never sets {@code billItem.qty}
     * and stores totals rather than a delta in netValue — guessing there would write a
     * confident wrong number into a financial report.
     */
    private LineComputation computeRateAdjustmentLine(BillTypeAtomic bta, BillItem billItem,
            PharmaceuticalBillItem pbi) {

        LineComputation line = new LineComputation();

        double before = pbi.getBeforeAdjustmentValue();
        double after = pbi.getAfterAdjustmentValue();
        double qty = billItem.getQty() == null ? 0.0 : billItem.getQty();
        double recordedChange = billItem.getNetValue();

        AuditValueSemantics semantics = resolveRateSemantics(before, after, qty, recordedChange);
        if (semantics == AuditValueSemantics.UNRESOLVED) {
            line.setSemantics(AuditValueSemantics.UNRESOLVED);
            line.setNote("Neither reading of beforeAdjustmentValue/afterAdjustmentValue"
                    + " reproduces billItem.netValue (before=" + before + ", after=" + after
                    + ", qty=" + qty + ", netValue=" + recordedChange
                    + "; as rates=" + ((after - before) * qty) + ", as values=" + (after - before) + ")");
            return line;
        }

        double change = recordedChange;
        double beforeExtended = (semantics == AuditValueSemantics.RATE) ? before * qty : before;
        double afterExtended = (semantics == AuditValueSemantics.RATE) ? after * qty : after;

        line.setSemantics(semantics);
        line.setQuantity(BigDecimal.valueOf(qty));
        line.setBeforeValue(BigDecimal.valueOf(beforeExtended));
        line.setAfterValue(BigDecimal.valueOf(afterExtended));

        // A retail rate change moves retail value only; a purchase rate change moves
        // purchase value only. Neither touches quantity, so cost value is unaffected.
        // The change is recorded in its own dimension AND as the bill's primary value —
        // writing it into the retail column for a purchase rate adjustment would report
        // the same movement twice and contradict what the save path stores.
        line.setPrimaryValue(BigDecimal.valueOf(change));
        if (bta == BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT) {
            line.setPurchaseValue(BigDecimal.valueOf(change));
        } else {
            line.setRetailValue(BigDecimal.valueOf(change));
        }
        return line;
    }

    /**
     * Decides how to read a rate adjustment line's before/after audit values, by testing
     * each candidate reading against {@code recordedChange} — {@code billItem.netValue},
     * the signed value change, which both writers persist correctly.
     *
     * <p>Package-private so the decision can be tested directly against real production
     * figures without a database.</p>
     *
     * @param before        pbi.beforeAdjustmentValue as stored
     * @param after         pbi.afterAdjustmentValue as stored
     * @param qty           billItem.qty
     * @param recordedChange billItem.netValue
     * @return RATE, VALUE, or UNRESOLVED when neither reading reproduces recordedChange
     */
    AuditValueSemantics resolveRateSemantics(double before, double after, double qty, double recordedChange) {
        double asRate = (after - before) * qty;
        double asValue = after - before;

        if (reconciles(asRate, recordedChange)) {
            // A single-unit line satisfies both readings, but they agree numerically, so
            // which one is named makes no difference to the value written.
            return AuditValueSemantics.RATE;
        }
        if (reconciles(asValue, recordedChange)) {
            return AuditValueSemantics.VALUE;
        }
        return AuditValueSemantics.UNRESOLVED;
    }

    // -------------------------------------------------------------------------
    // Rate resolution
    // -------------------------------------------------------------------------

    /**
     * Point-in-time rate first, then the StockHistory snapshot written by
     * {@code PharmacyBean.addToStockHistory} at the moment of the adjustment, then the
     * item batch's current rate. The batch rate is last because it may have moved since
     * — using it for a bill from weeks ago silently produces the wrong value.
     */
    private ResolvedRate resolveRetailRate(PharmaceuticalBillItem pbi, StockHistory snapshot,
            BillItem billItem, ItemBatch itemBatch) {
        if (pbi.getRetailRate() > 0) {
            return ResolvedRate.pointInTime(pbi.getRetailRate());
        }
        if (snapshot != null && snapshot.getRetailRate() > 0) {
            return ResolvedRate.pointInTime(snapshot.getRetailRate());
        }
        // The rate the line was billed at, captured when the adjustment was made.
        if (billItem != null && billItem.getNetRate() > 0) {
            return ResolvedRate.pointInTime(billItem.getNetRate());
        }
        if (itemBatch != null) {
            return ResolvedRate.currentBatchRate(itemBatch.getRetailsaleRate());
        }
        return ResolvedRate.pointInTime(0.0);
    }

    private ResolvedRate resolveCostRate(PharmaceuticalBillItem pbi, StockHistory snapshot, ItemBatch itemBatch) {
        if (pbi.getCostRate() > 0) {
            return ResolvedRate.pointInTime(pbi.getCostRate());
        }
        if (snapshot != null && snapshot.getCostRate() > 0) {
            return ResolvedRate.pointInTime(snapshot.getCostRate());
        }
        if (itemBatch == null) {
            return ResolvedRate.pointInTime(0.0);
        }
        if (itemBatch.getCostRate() != null && itemBatch.getCostRate() > 0) {
            return ResolvedRate.currentBatchRate(itemBatch.getCostRate());
        }
        return ResolvedRate.currentBatchRate(itemBatch.getPurcahseRate());
    }

    private ResolvedRate resolvePurchaseRate(StockHistory snapshot, ItemBatch itemBatch) {
        if (snapshot != null && snapshot.getPurchaseRate() > 0) {
            return ResolvedRate.pointInTime(snapshot.getPurchaseRate());
        }
        if (itemBatch != null) {
            return ResolvedRate.currentBatchRate(itemBatch.getPurcahseRate());
        }
        return ResolvedRate.pointInTime(0.0);
    }

    /**
     * A rate together with whether it had to be taken from the item batch's CURRENT value
     * rather than a point-in-time record — which is the difference between a figure and an
     * estimate, and is disclosed to the caller.
     */
    private static class ResolvedRate {

        private final double rate;
        private final boolean fromCurrentBatchRate;

        private ResolvedRate(double rate, boolean fromCurrentBatchRate) {
            this.rate = rate;
            // A zero rate contributes nothing, so it is not worth disclosing as an estimate.
            this.fromCurrentBatchRate = fromCurrentBatchRate && rate != 0.0;
        }

        static ResolvedRate pointInTime(double rate) {
            return new ResolvedRate(rate, false);
        }

        static ResolvedRate currentBatchRate(double rate) {
            return new ResolvedRate(rate, true);
        }
    }

    /**
     * Relative comparison of two money/quantity figures. The values being compared were
     * computed by different code paths, some through a float field, so an exact equality
     * test would reject correct matches.
     */
    private boolean reconciles(double computed, double recorded) {
        double tolerance = Math.max(0.01, Math.abs(recorded) * 0.001);
        return Math.abs(computed - recorded) <= tolerance;
    }

    // -------------------------------------------------------------------------
    // Audit trail
    // -------------------------------------------------------------------------

    private void appendAuditLog(Bill bill, String auditComment, String approvedBy, WebUser performedBy,
            boolean isNew, AuditValueSemantics semantics, BigDecimal grossTotal, BigDecimal netTotal) {

        String existing = bill.getComments();
        String correctedBy = (performedBy != null) ? performedBy.getName() : "Unknown user";
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        StringBuilder sb = new StringBuilder();
        if (existing != null && !existing.trim().isEmpty()) {
            sb.append(existing.trim()).append("\n\n");
        }
        sb.append("[BFD Backfill]")
                .append("\nTime: ").append(now)
                .append("\nBillType: ").append(bill.getBillTypeAtomic())
                .append("\nAction: ").append(isNew ? "Created new BFD" : "Updated existing BFD")
                .append("\nAuditValueSemantics: ").append(semantics)
                .append("\nGrossTotal: ").append(grossTotal)
                .append("\nNetTotal: ").append(netTotal)
                .append("\nPerformedBy: ").append(correctedBy)
                .append("\nApprovedBy: ").append(approvedBy)
                .append("\nAuditComment: ").append(auditComment);

        bill.setComments(sb.toString());
    }

    // -------------------------------------------------------------------------
    // Result types
    // -------------------------------------------------------------------------

    public enum BackfillStatus {
        /** Dry run: this bill would be corrected. */
        WOULD_UPDATE,
        /** Applied: BFD created or corrected. */
        UPDATED,
        /** Nothing to do — no items, or the computed value is zero. */
        SKIPPED,
        /** The stored audit values could not be interpreted; left untouched. */
        UNRESOLVED,
        /** An exception was thrown while processing this bill. */
        ERROR
    }

    /**
     * One row of the preview / result table, holding both what F15 shows now and what
     * the backfill computes, so the two can be compared before anything is written.
     */
    public static class BillBackfillResult implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long billId;
        private String deptId;
        private Date billDate;
        private String departmentName;
        private BillTypeAtomic billTypeAtomic;
        private AuditValueSemantics semantics;
        private BackfillStatus status = BackfillStatus.SKIPPED;
        private String note;
        private boolean ratesApproximated;

        private BigDecimal existingNetTotal;
        private BigDecimal computedNetTotal = BigDecimal.ZERO;
        private BigDecimal computedGrossTotal = BigDecimal.ZERO;
        private BigDecimal computedRetailValue = BigDecimal.ZERO;
        private BigDecimal computedCostValue = BigDecimal.ZERO;
        private BigDecimal computedPurchaseValue = BigDecimal.ZERO;
        private BigDecimal computedQuantity = BigDecimal.ZERO;

        public BillBackfillResult() {
        }

        public BillBackfillResult(Bill bill) {
            if (bill == null) {
                return;
            }
            this.billId = bill.getId();
            this.deptId = bill.getDeptId();
            this.billDate = bill.getCreatedAt();
            this.billTypeAtomic = bill.getBillTypeAtomic();
            this.departmentName = bill.getDepartment() == null ? null : bill.getDepartment().getName();
            this.existingNetTotal = bill.hasBillFinanceDetails()
                    ? bill.getBillFinanceDetails().getNetTotal()
                    : null;
        }

        public Long getBillId() {
            return billId;
        }

        public void setBillId(Long billId) {
            this.billId = billId;
        }

        public String getDeptId() {
            return deptId;
        }

        public void setDeptId(String deptId) {
            this.deptId = deptId;
        }

        public Date getBillDate() {
            return billDate;
        }

        public void setBillDate(Date billDate) {
            this.billDate = billDate;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public void setDepartmentName(String departmentName) {
            this.departmentName = departmentName;
        }

        public BillTypeAtomic getBillTypeAtomic() {
            return billTypeAtomic;
        }

        public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
            this.billTypeAtomic = billTypeAtomic;
        }

        public AuditValueSemantics getSemantics() {
            return semantics;
        }

        public void setSemantics(AuditValueSemantics semantics) {
            this.semantics = semantics;
        }

        public BackfillStatus getStatus() {
            return status;
        }

        public void setStatus(BackfillStatus status) {
            this.status = status;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public boolean isRatesApproximated() {
            return ratesApproximated;
        }

        public void setRatesApproximated(boolean ratesApproximated) {
            this.ratesApproximated = ratesApproximated;
        }

        public BigDecimal getExistingNetTotal() {
            return existingNetTotal;
        }

        public void setExistingNetTotal(BigDecimal existingNetTotal) {
            this.existingNetTotal = existingNetTotal;
        }

        public BigDecimal getComputedNetTotal() {
            return computedNetTotal;
        }

        public void setComputedNetTotal(BigDecimal computedNetTotal) {
            this.computedNetTotal = computedNetTotal;
        }

        public BigDecimal getComputedGrossTotal() {
            return computedGrossTotal;
        }

        public void setComputedGrossTotal(BigDecimal computedGrossTotal) {
            this.computedGrossTotal = computedGrossTotal;
        }

        public BigDecimal getComputedRetailValue() {
            return computedRetailValue;
        }

        public void setComputedRetailValue(BigDecimal computedRetailValue) {
            this.computedRetailValue = computedRetailValue;
        }

        public BigDecimal getComputedCostValue() {
            return computedCostValue;
        }

        public void setComputedCostValue(BigDecimal computedCostValue) {
            this.computedCostValue = computedCostValue;
        }

        public BigDecimal getComputedPurchaseValue() {
            return computedPurchaseValue;
        }

        public void setComputedPurchaseValue(BigDecimal computedPurchaseValue) {
            this.computedPurchaseValue = computedPurchaseValue;
        }

        public BigDecimal getComputedQuantity() {
            return computedQuantity;
        }

        public void setComputedQuantity(BigDecimal computedQuantity) {
            this.computedQuantity = computedQuantity;
        }
    }

    /**
     * Aggregate outcome of a backfill run, with the per-bill rows kept so a dry run can
     * be reviewed line by line before it is applied.
     */
    public static class BackfillReport implements Serializable {

        private static final long serialVersionUID = 1L;

        private boolean dryRun;
        private int candidatesFound;
        private final List<BillBackfillResult> results = new ArrayList<>();

        public void add(BillBackfillResult result) {
            results.add(result);
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }

        public int getCandidatesFound() {
            return candidatesFound;
        }

        public void setCandidatesFound(int candidatesFound) {
            this.candidatesFound = candidatesFound;
        }

        public List<BillBackfillResult> getResults() {
            return results;
        }

        public int countByStatus(BackfillStatus status) {
            int n = 0;
            for (BillBackfillResult r : results) {
                if (r.getStatus() == status) {
                    n++;
                }
            }
            return n;
        }

        public int getBackfilled() {
            return countByStatus(BackfillStatus.UPDATED);
        }

        public int getWouldUpdate() {
            return countByStatus(BackfillStatus.WOULD_UPDATE);
        }

        public int getSkipped() {
            return countByStatus(BackfillStatus.SKIPPED);
        }

        public int getUnresolved() {
            return countByStatus(BackfillStatus.UNRESOLVED);
        }

        public int getErrors() {
            return countByStatus(BackfillStatus.ERROR);
        }

        /** Net movement the run would add to (or has added to) the F15 adjustment section. */
        public BigDecimal getTotalNetChange() {
            BigDecimal total = BigDecimal.ZERO;
            for (BillBackfillResult r : results) {
                if (r.getStatus() == BackfillStatus.UPDATED || r.getStatus() == BackfillStatus.WOULD_UPDATE) {
                    total = total.add(r.getComputedNetTotal());
                }
            }
            return total;
        }

        public List<String> getErrorMessages() {
            List<String> messages = new ArrayList<>();
            for (BillBackfillResult r : results) {
                if (r.getStatus() == BackfillStatus.ERROR || r.getStatus() == BackfillStatus.UNRESOLVED) {
                    messages.add("Bill " + r.getBillId() + " (" + r.getDeptId() + "): " + r.getNote());
                }
            }
            return messages;
        }
    }

    private static class LineComputation {

        private AuditValueSemantics semantics = AuditValueSemantics.UNRESOLVED;
        private String note;
        private boolean ratesApproximated;
        /** The bill's headline change, whichever dimension carries it. Drives gross/net. */
        private BigDecimal primaryValue = BigDecimal.ZERO;
        private BigDecimal retailValue = BigDecimal.ZERO;
        private BigDecimal costValue = BigDecimal.ZERO;
        private BigDecimal purchaseValue = BigDecimal.ZERO;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal beforeValue = BigDecimal.ZERO;
        private BigDecimal afterValue = BigDecimal.ZERO;

        AuditValueSemantics getSemantics() {
            return semantics;
        }

        void setSemantics(AuditValueSemantics semantics) {
            this.semantics = semantics;
        }

        String getNote() {
            return note;
        }

        void setNote(String note) {
            this.note = note;
        }

        boolean isRatesApproximated() {
            return ratesApproximated;
        }

        void setRatesApproximated(boolean ratesApproximated) {
            this.ratesApproximated = ratesApproximated;
        }

        BigDecimal getPrimaryValue() {
            return primaryValue;
        }

        void setPrimaryValue(BigDecimal primaryValue) {
            this.primaryValue = primaryValue;
        }

        BigDecimal getRetailValue() {
            return retailValue;
        }

        void setRetailValue(BigDecimal retailValue) {
            this.retailValue = retailValue;
        }

        BigDecimal getCostValue() {
            return costValue;
        }

        void setCostValue(BigDecimal costValue) {
            this.costValue = costValue;
        }

        BigDecimal getPurchaseValue() {
            return purchaseValue;
        }

        void setPurchaseValue(BigDecimal purchaseValue) {
            this.purchaseValue = purchaseValue;
        }

        BigDecimal getQuantity() {
            return quantity;
        }

        void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        BigDecimal getBeforeValue() {
            return beforeValue;
        }

        void setBeforeValue(BigDecimal beforeValue) {
            this.beforeValue = beforeValue;
        }

        BigDecimal getAfterValue() {
            return afterValue;
        }

        void setAfterValue(BigDecimal afterValue) {
            this.afterValue = afterValue;
        }
    }
}
