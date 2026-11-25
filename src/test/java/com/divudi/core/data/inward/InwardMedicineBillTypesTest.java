package com.divudi.core.data.inward;

import com.divudi.core.data.BillTypeAtomic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class InwardMedicineBillTypesTest {

    @Test
    void acceptedWardReturnIsAReturnInItemLists() {
        assertTrue(InwardMedicineBillTypes.isItemListReturn(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD));
    }

    @Test
    void existingReturnTypesStayReturns() {
        assertTrue(InwardMedicineBillTypes.isItemListReturn(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN));
        assertTrue(InwardMedicineBillTypes.isItemListReturn(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN));
        assertTrue(InwardMedicineBillTypes.isItemListReturn(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN));
    }

    @Test
    void issuesAndTheWardsOwnReturnBillAreNotDeducted() {
        assertFalse(InwardMedicineBillTypes.isItemListReturn(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD));
        assertFalse(InwardMedicineBillTypes.isItemListReturn(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE));
        // The BHT is credited on pharmacy accept (#25509); counting this too would deduct twice.
        assertFalse(InwardMedicineBillTypes.isItemListReturn(BillTypeAtomic.RETURN_MEDICINE_INWARD));
        assertFalse(InwardMedicineBillTypes.isItemListReturn(null));
    }

    @Test
    void wardReturnBillIsNotInTheInterimMedicineList() {
        assertFalse(InwardMedicineBillTypes.ALL.contains(BillTypeAtomic.RETURN_MEDICINE_INWARD));
        assertTrue(InwardMedicineBillTypes.ALL.contains(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD));
    }

    @Test
    void everyItemListReturnIsInTheInterimMedicineList() {
        assertTrue(InwardMedicineBillTypes.ALL.containsAll(InwardMedicineBillTypes.ITEM_LIST_RETURNS));
    }

    @Test
    void liveOnlyListDropsEveryIssueCancellationAndKeepsReturns() {
        for (BillTypeAtomic c : InwardMedicineBillTypes.ISSUE_CANCELLATIONS) {
            assertTrue(InwardMedicineBillTypes.ALL.contains(c));
            assertFalse(InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.contains(c));
        }
        assertTrue(InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.contains(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD));
        assertTrue(InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.contains(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN));
        assertTrue(InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.contains(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD));
        assertEquals(InwardMedicineBillTypes.ALL.size() - InwardMedicineBillTypes.ISSUE_CANCELLATIONS.size(),
                InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.size());
    }

    @Test
    void theatreMedicineIsListedAndItsReturnIsDeducted() {
        assertTrue(InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.contains(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE));
        assertTrue(InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.contains(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN));
        assertFalse(InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.contains(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION));
        assertTrue(InwardMedicineBillTypes.isItemListReturn(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN));
        assertFalse(InwardMedicineBillTypes.isItemListReturn(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE));
    }

    @Test
    void signedValueDeductsAReturnOnceWhateverItsStoredSign() {
        assertEquals(100.0, InwardMedicineBillTypes.itemListSignedValue(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE, 100.0), 0.0001);
        assertEquals(100.0, InwardMedicineBillTypes.itemListSignedValue(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE, -100.0), 0.0001);
        assertEquals(-40.0, InwardMedicineBillTypes.itemListSignedValue(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN, 40.0), 0.0001);
        assertEquals(-40.0, InwardMedicineBillTypes.itemListSignedValue(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN, -40.0), 0.0001);
        assertEquals(0.0, InwardMedicineBillTypes.itemListSignedValue(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD, null), 0.0001);
    }

    @Test
    void listTotalMatchesInterimBillForIssueReturnAndCancelledPair() {
        // Issue 100, return 30 (stored negative), issue 50 later cancelled.
        // Interim bill: sums every bill of ALL by type, ignoring the cancelled flag, so the
        // cancelled issue (+50) and its cancellation bill (-50) net to 0: 100 - 30 + 50 - 50 = 70.
        // List: skips cancelled issues and has no cancellation types, so the pair drops out.
        Object[][] bills = {
            {BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE, 100.0, false},
            {BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN, -30.0, false},
            {BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE, 50.0, true},
            {BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION, -50.0, false}
        };
        double interim = 0.0;
        double list = 0.0;
        for (Object[] b : bills) {
            BillTypeAtomic type = (BillTypeAtomic) b[0];
            double net = (Double) b[1];
            boolean cancelled = (Boolean) b[2];
            if (InwardMedicineBillTypes.ALL.contains(type)) {
                interim += net;
            }
            if (InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.contains(type) && !cancelled) {
                list += InwardMedicineBillTypes.itemListSignedValue(type, net);
            }
        }
        assertEquals(70.0, interim, 0.0001);
        assertEquals(interim, list, 0.0001);
    }

    @Test
    void legacyListKeepsCancellationsAndNetsToTheInterimTotal() {
        // Same bills as above, but the legacy list does not skip cancelled issues: the
        // cancelled issue (+50) and its cancellation (-50) both stay and net to 0.
        Object[][] bills = {
            {BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE, 100.0},
            {BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN, -30.0},
            {BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE, 50.0},
            {BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION, -50.0},
            {BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD, 10.0}
        };
        double interim = 100.0 - 30.0 + 50.0 - 50.0 - 10.0;
        double list = 0.0;
        for (Object[] b : bills) {
            list += InwardMedicineBillTypes.listSignedValueWithCancellations((BillTypeAtomic) b[0], (Double) b[1]);
        }
        assertEquals(60.0, interim, 0.0001);
        assertEquals(interim, list, 0.0001);
        assertEquals(0.0, InwardMedicineBillTypes.listSignedValueWithCancellations(null, null), 0.0001);
    }

    @Test
    void cancelledReturnNetsToZeroAndIsNotAnIssue() {
        BillTypeAtomic cancel = BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION;
        assertTrue(InwardMedicineBillTypes.ALL.contains(cancel));
        assertTrue(InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.contains(cancel));
        assertFalse(InwardMedicineBillTypes.ISSUES.contains(cancel));
        // 100 issued, 40 returned (deducted), the return cancelled (added back): 100.
        double total = InwardMedicineBillTypes.itemListSignedValue(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, 100.0)
                + InwardMedicineBillTypes.itemListSignedValue(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, -40.0)
                + InwardMedicineBillTypes.itemListSignedValue(cancel, 40.0);
        assertEquals(100.0, total, 0.0001);
    }

    @Test
    void liveBillsListLeavesOutTheReturnCancellationAndTheIssueCancellations() {
        // The Charge Type Breakdown and Detail skip cancelled bills, so the cancelled return is
        // already out; its cancellation must be out too.
        assertFalse(InwardMedicineBillTypes.ALL_LIVE_BILLS.contains(BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION));
        for (BillTypeAtomic t : InwardMedicineBillTypes.ISSUE_CANCELLATIONS) {
            assertFalse(InwardMedicineBillTypes.ALL_LIVE_BILLS.contains(t));
        }
        assertTrue(InwardMedicineBillTypes.ALL_LIVE_BILLS.contains(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN));
        assertTrue(InwardMedicineBillTypes.ALL_LIVE_BILLS.contains(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD));
        assertTrue(InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.contains(BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION));
    }

    @Test
    void returnCancellationIsAddedBackByMagnitude() {
        BillTypeAtomic cancel = BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION;
        assertTrue(InwardMedicineBillTypes.isReturnCancellation(cancel));
        assertEquals(40.0, InwardMedicineBillTypes.signedRowValue(cancel, -40.0), 0.0001);
        assertEquals(40.0, InwardMedicineBillTypes.signedRowValue(cancel, 40.0), 0.0001);
        assertEquals(40.0, InwardMedicineBillTypes.listSignedValueWithCancellations(cancel, -40.0), 0.0001);
    }
}
