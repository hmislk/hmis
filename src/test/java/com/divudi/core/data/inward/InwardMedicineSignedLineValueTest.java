package com.divudi.core.data.inward;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class InwardMedicineSignedLineValueTest {

    private static BillItem line(BillTypeAtomic bta, double net) {
        Bill b = new Bill();
        b.setBillTypeAtomic(bta);
        BillItem bi = new BillItem();
        bi.setBill(b);
        bi.setNetValue(net);
        return bi;
    }

    @Test
    void issueLessPositivelyStoredReturnIsIssueMinusReturn() {
        double total = InwardMedicineBillTypes.signedLineValue(line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, 100))
                + InwardMedicineBillTypes.signedLineValue(line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, 30));
        assertEquals(70.0, total, 0.0001);
    }

    @Test
    void negativelyStoredReturnIsStillDeductedOnce() {
        assertEquals(-30.0, InwardMedicineBillTypes.signedLineValue(
                line(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN, -30)), 0.0001);
    }

    @Test
    void acceptedWardReturnIsDeducted() {
        assertEquals(-20.0, InwardMedicineBillTypes.signedLineValue(
                line(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD, 20)), 0.0001);
    }

    @Test
    void issueCancellationOffsetsCancelledIssue() {
        // the report drops both the cancelled issue and its cancellation; if the
        // cancellation is counted it deducts the issue value
        assertEquals(0.0, InwardMedicineBillTypes.signedLineValue(
                line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, 40))
                + InwardMedicineBillTypes.signedLineValue(
                        line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION, 40)), 0.0001);
    }

    @Test
    void reversalPairNetsToZeroAndOtherTypesKeepStoredValue() {
        assertEquals(0.0, InwardMedicineBillTypes.signedLineValue(line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, 50))
                + InwardMedicineBillTypes.signedLineValue(line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, -50)), 0.0001);
        assertEquals(15.0, InwardMedicineBillTypes.signedLineValue(line(null, 15)), 0.0001);
        assertEquals(0.0, InwardMedicineBillTypes.signedLineValue(null), 0.0001);
    }
}
