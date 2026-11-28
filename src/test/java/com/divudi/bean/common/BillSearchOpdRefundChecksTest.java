package com.divudi.bean.common;

import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.RefundBill;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BillSearchOpdRefundChecksTest {

    private Bill refundBillWithFeeValue(double feeValue, BillFee original) {
        BillItem bi = new BillItem(); // netValue stays 0: the totals step has not run
        BillFee bf = new BillFee();
        bf.setFeeValue(feeValue);
        bf.setReferenceBillFee(original);
        bf.setBillItem(bi);
        bi.getBillFees().add(bf);
        Bill rb = new RefundBill();
        rb.setBillItems(new ArrayList<>());
        rb.getBillItems().add(bi);
        return rb;
    }

    @Test
    public void alreadyRefundedCheckSeesFeeValueWhenNetValueIsZero() {
        BillFee original = new BillFee();
        BillSearch bs = new BillSearch();
        bs.setBillController(new BillController() {
            @Override
            public boolean hasRefunded(BillFee bf) {
                return bf == original;
            }
        });
        assertTrue(bs.billFeeIsAlreadyRefunded(refundBillWithFeeValue(50.0, original)));
        assertFalse(bs.billFeeIsAlreadyRefunded(refundBillWithFeeValue(0.0, original)));
    }

    @Test
    public void paidToStaffCheckSeesFeeValueWhenNetValueIsZero() {
        BillFee original = new BillFee();
        BillSearch bs = new BillSearch();
        bs.setBillController(new BillController() {
            @Override
            public boolean hasPaidToStaff(BillFee bf) {
                return bf == original;
            }
        });
        assertTrue(bs.billFeeIsAlreadyPaidToStaff(refundBillWithFeeValue(50.0, original)));
        assertFalse(bs.billFeeIsAlreadyPaidToStaff(refundBillWithFeeValue(0.0, original)));
    }

    @Test
    public void sampleCheckSeesFeeValueWhenNetValueIsZero() {
        BillSearch bs = new BillSearch();
        bs.setPatientInvestigationController(new PatientInvestigationController() {
            @Override
            public boolean sampledForBillItem(BillItem billItem) {
                return true;
            }
        });
        assertTrue(bs.sampleHasBeenCollected(refundBillWithFeeValue(50.0, new BillFee())));
        assertFalse(bs.sampleHasBeenCollected(refundBillWithFeeValue(0.0, new BillFee())));
    }
}
