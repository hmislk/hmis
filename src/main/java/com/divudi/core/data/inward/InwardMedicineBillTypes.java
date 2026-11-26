package com.divudi.core.data.inward;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The complete set of {@link BillTypeAtomic} values that make up the Inward
 * Medicine charge category, across every issue/return flow (Direct Issue,
 * Direct Issue Discharge, Issue on Request, the porter-based ward return, and
 * Theatre medicine) plus their cancellations.
 * <p>
 * Single source of truth for the interim bill's Medicine total and issue list
 * (InwardBeanController) and the inward charge aggregation service behind the
 * Invoice Journal and debtor reports, so they cannot drift apart (issues
 * #23871, #24717).
 */
public final class InwardMedicineBillTypes {

    public static final List<BillTypeAtomic> ALL = Collections.unmodifiableList(Arrays.asList(
            BillTypeAtomic.PHARMACY_DIRECT_ISSUE,
            BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION,
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN,
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
            // Porter-based ward return flow (#21466/#21470) - see issue #22990. The BHT is
            // credited when the pharmacy ACCEPTS the return (#25509), not when the ward sends
            // it, so the accept bill (negative value, accepted quantity) is counted and
            // RETURN_MEDICINE_INWARD / RETURN_MEDICINE_INWARD_CANCELLATION are not. A return
            // can only be cancelled before any acceptance, so no accept bill exists then.
            BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION,
            // Cancelling a BHT issue return (#26105): the patient keeps the medicines, so the
            // cancellation (positive, the return's value inverted) offsets the return that
            // stays in the total. The same type is also written by the retail and OPD return
            // cancels, so a report that covers more than inpatients must count it only for a
            // bill with a patient encounter.
            BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION
    ));

    /**
     * Medicine bill types that take value off the BHT's medicine total in a
     * per-item pharmacy list (a return, as opposed to an issue). The accept bill
     * is the one that carries the credit for a porter-based ward return (#25509),
     * so the ward's own return bill is not in this list.
     */
    public static final List<BillTypeAtomic> ITEM_LIST_RETURNS = Collections.unmodifiableList(Arrays.asList(
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN,
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN,
            BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN
    ));

    /**
     * The bill types that cancel a whole issue bill. The interim bill sums every
     * bill of {@link #ALL} without looking at the cancelled flag, so an issue and
     * its cancellation net to zero. A report that skips cancelled issue bills
     * (cancelled = true) must skip these as well, or the cancellation is deducted
     * with nothing to offset it (#26093). Returns and the ward accept bill are not
     * cancellations and stay counted.
     */
    public static final List<BillTypeAtomic> ISSUE_CANCELLATIONS = Collections.unmodifiableList(Arrays.asList(
            BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION,
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION
    ));

    /**
     * Medicine bill types for a report that lists only live (not cancelled) issue
     * bills: {@link #ALL} without the issue cancellations, so a cancelled issue and
     * its cancellation both drop out and the report agrees with the interim bill.
     */
    public static final List<BillTypeAtomic> ALL_EXCLUDING_ISSUE_CANCELLATIONS;

    /**
     * Medicine bill types for a report that skips every cancelled bill
     * (b.cancelled = false): {@link #ALL_EXCLUDING_ISSUE_CANCELLATIONS} without the
     * cancellation of a return. The cancelled return already drops out of such a
     * report, so its cancellation must too, or the return is added back twice (#26105).
     */
    public static final List<BillTypeAtomic> ALL_LIVE_BILLS;

    /**
     * The issue bill types only: {@link #ALL} without the returns, the accept bill
     * and the issue cancellations.
     */
    public static final List<BillTypeAtomic> ISSUES;

    static {
        List<BillTypeAtomic> l = new java.util.ArrayList<>(ALL);
        l.removeAll(ISSUE_CANCELLATIONS);
        ALL_EXCLUDING_ISSUE_CANCELLATIONS = Collections.unmodifiableList(l);
        List<BillTypeAtomic> live = new java.util.ArrayList<>(l);
        live.remove(BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION);
        ALL_LIVE_BILLS = Collections.unmodifiableList(live);
        List<BillTypeAtomic> issues = new java.util.ArrayList<>(l);
        issues.removeAll(ITEM_LIST_RETURNS);
        issues.remove(BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION);
        ISSUES = Collections.unmodifiableList(issues);
    }

    /**
     * Whether a bill of this type stays in a list that skips cancelled issue bills.
     * A cancelled issue drops out (with its cancellation). A cancelled return stays
     * in the list, and its cancellation bill, which is one of {@link #ALL}, offsets
     * it, as the interim bill does (#26105).
     */
    public static boolean isCountedInLiveList(BillTypeAtomic billTypeAtomic, boolean billCancelled) {
        return !billCancelled || isItemListReturn(billTypeAtomic);
    }

    /**
     * Whether a bill of this type stays in a list that skips cancelled issue bills.
     * As {@link #isCountedInLiveList(BillTypeAtomic, boolean)}, and a reversal bill
     * drops out too: the data-admin reversal of an issue writes a bill of the same
     * issue type with negative values and flags the original as cancelled. The
     * interim bill sums both and nets them to zero, so a list that drops the
     * original must drop the reversal (#26107).
     */
    public static boolean isCountedInLiveList(BillTypeAtomic billTypeAtomic, boolean billCancelled, boolean billIsReversal) {
        return !billIsReversal && isCountedInLiveList(billTypeAtomic, billCancelled);
    }

    /**
     * True for the bill the data-admin "reverse a direct issue to a BHT" action
     * writes: the original's cancelled bill, with the same bill type as the
     * original. A retired reversal does not count: the original stands again. A normal cancellation has its own cancellation bill type.
     */
    public static boolean isReversalBill(Bill bill) {
        if (bill == null || bill.getBillTypeAtomic() == null || bill.isRetired()) {
            return false;
        }
        Bill original = bill.getReferenceBill();
        return original != null
                && original.getCancelledBill() != null
                && original.getCancelledBill().equals(bill)
                && bill.getBillTypeAtomic() == original.getBillTypeAtomic();
    }

    /**
     * JPQL condition for a query over {@code BillItem bi}: drops a reversal bill
     * (see {@link #isReversalBill(Bill)}). Pair it with a filter that drops the
     * cancelled original, so the pair nets to zero as in the interim bill (#26107).
     */
    public static final String JPQL_NOT_REVERSAL_BILL
            = "NOT EXISTS (SELECT rv.id FROM Bill rv WHERE rv.cancelledBill.id = bi.bill.id "
            + "AND rv.retired = false AND rv.billTypeAtomic = bi.bill.billTypeAtomic) ";

    /**
     * JPQL condition for a query over {@code BillItem bi} that does not filter on
     * the cancelled flag: drops an original that a reversal bill (same bill type)
     * cancelled (#26107).
     */
    public static final String JPQL_NOT_REVERSED_ORIGINAL
            = "NOT EXISTS (SELECT rv.id FROM Bill rv WHERE rv.id = bi.bill.cancelledBill.id "
            + "AND rv.retired = false AND rv.billTypeAtomic = bi.bill.billTypeAtomic) ";

    /**
     * True for the cancellation of a BHT issue return. Its line is stored with
     * either sign (a return line is usually stored positive and the cancellation
     * inverts it), so it is always added back by magnitude.
     */
    public static boolean isReturnCancellation(BillTypeAtomic billTypeAtomic) {
        return billTypeAtomic == BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION;
    }

    /**
     * True when a line of this bill type is deducted, not added, in an
     * inpatient pharmacy item list total.
     */
    public static boolean isItemListReturn(BillTypeAtomic billTypeAtomic) {
        return billTypeAtomic != null && ITEM_LIST_RETURNS.contains(billTypeAtomic);
    }

    /**
     * The amount a line adds to an inpatient pharmacy item list total: the
     * magnitude of its net value, positive for an issue and negative for a
     * return. Return lines are not stored with a consistent sign, so only the
     * bill type decides.
     */
    public static double itemListSignedValue(BillTypeAtomic billTypeAtomic, Double netValue) {
        double magnitude = netValue != null ? Math.abs(netValue) : 0.0;
        return isItemListReturn(billTypeAtomic) ? -magnitude : magnitude;
    }

    /**
     * The amount a bill line adds to a pharmacy item list that keeps the issue
     * cancellations in its query (the legacy list), so a cancelled issue and its
     * cancellation net to zero as in the interim bill: positive for an issue,
     * negative for a return, an accept bill or a cancellation. The magnitude of
     * the net value is used, as return lines are not stored with a consistent sign.
     */
    public static double listSignedValueWithCancellations(BillTypeAtomic billTypeAtomic, Double netValue) {
        double magnitude = netValue != null ? Math.abs(netValue) : 0.0;
        boolean deducted = isItemListReturn(billTypeAtomic)
                || (billTypeAtomic != null && ISSUE_CANCELLATIONS.contains(billTypeAtomic));
        return deducted ? -magnitude : magnitude;
    }

    /**
     * The value a displayed row adds to a list total. A return, an accept bill
     * or a cancellation is deducted (negative magnitude). An issue row keeps the
     * sign it holds, because a netted issue row (issue less the returns that
     * refer to it) can be below zero and must not be shown as positive (#26105).
     */
    public static double signedRowValue(BillTypeAtomic billTypeAtomic, Double netValue) {
        double value = netValue != null ? netValue : 0.0;
        if (isReturnCancellation(billTypeAtomic)) {
            return Math.abs(value);
        }
        boolean deducted = isItemListReturn(billTypeAtomic)
                || (billTypeAtomic != null && ISSUE_CANCELLATIONS.contains(billTypeAtomic));
        return deducted ? -Math.abs(value) : value;
    }

    /**
     * The value a bill line adds to a charge total (Charge Type Breakdown and
     * Detail). A return line is often stored positive, so it is signed by its bill
     * type through {@link #signedRowValue}; a line of any other bill type keeps its
     * stored value (#26123).
     */
    public static double signedLineValue(com.divudi.core.entity.BillItem bi) {
        return bi == null ? 0.0 : signedByBillType(bi, bi.getNetValue());
    }

    /**
     * Signs any amount of a bill line (gross, discount or net) by the line's bill
     * type, so gross - discount = net holds for every row of the Detail report.
     */
    public static double signedByBillType(com.divudi.core.entity.BillItem bi, Double value) {
        BillTypeAtomic bta = bi != null && bi.getBill() != null ? bi.getBill().getBillTypeAtomic() : null;
        return signedRowValue(bta, value);
    }

    private InwardMedicineBillTypes() {
    }
}
