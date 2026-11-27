package com.divudi.bean.inward;

import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.PatientEncounter;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class FinalBillSaveRollbackStateTest {

    @Test
    void restoreUndoesWhatASettleChangesOnThePageCopies() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        PatientEncounter pe = new PatientEncounter();
        pe.setGrantTotal(10.0);
        pe.setDiscount(1.0);
        pe.setNetTotal(9.0);
        pe.setCreditUsedAmount(2.0);

        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, pe);

        // What a settle does before it fails.
        current.setId(77L);
        current.getBillItems().add(new BillItem());
        current.getBillItems().add(new BillItem());
        pe.setFinalBill(current);
        pe.setGrantTotal(500.0);
        pe.setDiscount(50.0);
        pe.setNetTotal(450.0);
        pe.setPaymentFinalized(true);
        pe.setCreditUsedAmount(450.0);

        state.restore();

        assertNull(current.getId());
        assertEquals(0, current.getBillItems().size());
        assertNull(pe.getFinalBill());
        assertEquals(10.0, pe.getGrantTotal());
        assertEquals(1.0, pe.getDiscount());
        assertEquals(9.0, pe.getNetTotal());
        assertEquals(2.0, pe.getCreditUsedAmount());
        assertFalse(pe.isPaymentFinalized());
    }

    @Test
    void restoreKeepsBillItemsThatWereThereBeforeTheSave() {
        Bill current = new BilledBill();
        current.setBillItems(new ArrayList<>());
        BillItem before = new BillItem();
        current.getBillItems().add(before);
        PatientEncounter pe = new PatientEncounter();

        FinalBillSaveRollbackState state = new FinalBillSaveRollbackState(current, pe);
        current.getBillItems().add(new BillItem());
        state.restore();

        assertEquals(1, current.getBillItems().size());
        assertEquals(before, current.getBillItems().get(0));
    }
}
