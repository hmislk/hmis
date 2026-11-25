package com.divudi.core.data.inward;

import com.divudi.core.data.BillTypeAtomic;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class InwardMedicineBillTypesTest {

    @Test
    void issueCancellationsAreRemovedAndReturnsStay() {
        List<BillTypeAtomic> in = Arrays.asList(
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN,
                BillTypeAtomic.RETURN_MEDICINE_INWARD,
                BillTypeAtomic.RETURN_MEDICINE_INWARD_CANCELLATION);
        List<BillTypeAtomic> out = InwardMedicineBillTypes.withoutIssueCancellations(in);
        assertEquals(4, out.size());
        assertFalse(out.contains(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION));
        assertFalse(out.contains(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION));
        assertTrue(out.contains(BillTypeAtomic.RETURN_MEDICINE_INWARD));
        assertTrue(out.contains(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN));
        assertEquals(6, in.size());
    }

    @Test
    void nullGivesEmptyList() {
        assertTrue(InwardMedicineBillTypes.withoutIssueCancellations(null).isEmpty());
    }
}
