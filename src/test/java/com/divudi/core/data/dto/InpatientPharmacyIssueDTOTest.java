package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class InpatientPharmacyIssueDTOTest {

    private static InpatientPharmacyIssueDTO dtoOf(BillTypeAtomic bta) {
        InpatientPharmacyIssueDTO dto = new InpatientPharmacyIssueDTO();
        dto.setBillTypeAtomic(bta);
        return dto;
    }

    @Test
    void wardReturnBillShowsAsReturn() {
        assertTrue(dtoOf(BillTypeAtomic.RETURN_MEDICINE_INWARD).isCancellationOrReturn());
    }

    @Test
    void existingReturnsStayReturns() {
        assertTrue(dtoOf(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN).isCancellationOrReturn());
        assertTrue(dtoOf(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN).isCancellationOrReturn());
    }

    @Test
    void issuesAndTheAcceptBillAreNotReturns() {
        assertFalse(dtoOf(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD).isCancellationOrReturn());
        assertFalse(dtoOf(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD).isCancellationOrReturn());
        assertFalse(dtoOf(null).isCancellationOrReturn());
    }
}
