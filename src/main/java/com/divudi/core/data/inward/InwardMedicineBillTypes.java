package com.divudi.core.data.inward;

import com.divudi.core.data.BillTypeAtomic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Helpers over the Inward Medicine bill types. The interim bill sums every
 * Medicine bill without looking at the cancelled flag, so a cancelled issue and
 * its cancellation net to zero. A report that skips cancelled bills must also
 * skip the issue cancellation bills, or the cancellation is deducted with
 * nothing to offset it (#26093).
 */
public final class InwardMedicineBillTypes {

    /** Bill types that cancel a whole issue bill. Returns are not cancellations. */
    public static final List<BillTypeAtomic> ISSUE_CANCELLATIONS = Collections.unmodifiableList(Arrays.asList(
            BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
            BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_CANCELLATION,
            BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
            BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION
    ));

    /** A copy of the given bill types without the issue cancellations. */
    public static List<BillTypeAtomic> withoutIssueCancellations(Collection<BillTypeAtomic> types) {
        List<BillTypeAtomic> l = new ArrayList<>();
        if (types != null) {
            l.addAll(types);
        }
        l.removeAll(ISSUE_CANCELLATIONS);
        return l;
    }

    private InwardMedicineBillTypes() {
    }
}
