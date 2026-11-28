package com.divudi.bean.opd;

import com.divudi.core.entity.BillItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpdOrderControllerTest {

    @Test
    void isPersistedBillItem_returnsFalse_whenNullOrNew() {
        assertFalse(OpdOrderController.isPersistedBillItem(null));
        assertFalse(OpdOrderController.isPersistedBillItem(new BillItem()));
    }

    @Test
    void isPersistedBillItem_returnsTrue_whenSavedAndNotRetired() {
        BillItem bi = new BillItem();
        bi.setId(1L);
        assertTrue(OpdOrderController.isPersistedBillItem(bi));
    }

    @Test
    void isPersistedBillItem_returnsFalse_whenRetired() {
        BillItem bi = new BillItem();
        bi.setId(1L);
        bi.setRetired(true);
        assertFalse(OpdOrderController.isPersistedBillItem(bi));
    }
}
