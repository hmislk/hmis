package com.divudi.core.data.inward;

import com.divudi.core.data.BillTypeAtomic;
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
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION
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

    static {
        List<BillTypeAtomic> l = new java.util.ArrayList<>(ALL);
        l.removeAll(ISSUE_CANCELLATIONS);
        ALL_EXCLUDING_ISSUE_CANCELLATIONS = Collections.unmodifiableList(l);
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

    private InwardMedicineBillTypes() {
    }
}
