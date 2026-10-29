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
            // Porter-based ward return flow (#21466/#21470) - see issue #22990.
            BillTypeAtomic.RETURN_MEDICINE_INWARD,
            BillTypeAtomic.RETURN_MEDICINE_INWARD_CANCELLATION,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION
    ));

    private InwardMedicineBillTypes() {
    }
}
