package com.divudi.ejb;

import com.divudi.core.data.BillTypeAtomic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PharmacyServiceAdjustmentBillTypesTest {

    @Test
    public void adjustmentBillTypesIncludeExpiryDateAdjustment() {
        PharmacyService service = new PharmacyService();
        assertTrue(service.getPharmacyAdjustmentBillTypes()
                .contains(BillTypeAtomic.PHARMACY_STOCK_EXPIRY_DATE_AJUSTMENT),
                "Expiry-date adjustment bills must appear in the F15 Adjustment Transactions section");
    }
}
