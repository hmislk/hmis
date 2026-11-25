package com.divudi.core.data.inward;

import com.divudi.core.data.BillTypeAtomic;
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
}
