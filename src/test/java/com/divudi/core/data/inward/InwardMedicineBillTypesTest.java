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
        // Issue 100, return 30, second issue 50 later cancelled (cancelled issue and its
        // cancellation bill are both skipped by the list); interim bill: 100 - 30 + 50 - 50 = 70.
        double total = InwardMedicineBillTypes.itemListSignedValue(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE, 100.0)
                + InwardMedicineBillTypes.itemListSignedValue(BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN, -30.0);
        assertEquals(70.0, total, 0.0001);
    }
}
