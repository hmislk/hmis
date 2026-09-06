package com.divudi.bean.opd;

import com.divudi.core.entity.BillItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpdBillControllerTest {

    @Test
    void isPersistedBillItem_returnsFalse_whenNull() {
        assertFalse(OpdBillController.isPersistedBillItem(null));
    }

    @Test
    void isPersistedBillItem_returnsFalse_whenIdIsNull() {
        assertFalse(OpdBillController.isPersistedBillItem(new BillItem()));
    }

    @Test
    void isPersistedBillItem_returnsTrue_whenIdIsSet() {
        BillItem bi = new BillItem();
        bi.setId(1L);
        assertTrue(OpdBillController.isPersistedBillItem(bi));
    }
}
