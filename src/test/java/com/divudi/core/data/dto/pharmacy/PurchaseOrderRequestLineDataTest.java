package com.divudi.core.data.dto.pharmacy;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class PurchaseOrderRequestLineDataTest {
    @Test
    void gettersReturnValuesSetByConstructorArgs() {
        var d = new PurchaseOrderRequestLineData();
        d.setBillItemId(5L);
        d.setPharmaceuticalBillItemId(7L);
        d.setItemId(100L);
        d.setAmpp(true);
        d.setQuantity(BigDecimal.TEN);
        d.setFreeQuantity(BigDecimal.ONE);
        d.setPurchaseRate(BigDecimal.valueOf(12.5));
        d.setRetailRate(BigDecimal.valueOf(15.0));
        d.setUnitsPerPack(BigDecimal.valueOf(10));
        d.setSerialNo(2);
        d.setCreaterId(1L);

        assertEquals(Long.valueOf(5L), d.getBillItemId());
        assertEquals(Long.valueOf(7L), d.getPharmaceuticalBillItemId());
        assertEquals(Long.valueOf(100L), d.getItemId());
        assertEquals(true, d.isAmpp());
        assertEquals(BigDecimal.TEN, d.getQuantity());
        assertEquals(BigDecimal.ONE, d.getFreeQuantity());
        assertEquals(BigDecimal.valueOf(12.5), d.getPurchaseRate());
        assertEquals(BigDecimal.valueOf(15.0), d.getRetailRate());
        assertEquals(BigDecimal.valueOf(10), d.getUnitsPerPack());
        assertEquals(2, d.getSerialNo());
        assertEquals(Long.valueOf(1L), d.getCreaterId());
    }

    @Test
    void projectionConstructorSetsOnlyProjectedFields() {
        var d = new PurchaseOrderRequestLineData(100L, true, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.valueOf(12.5), BigDecimal.valueOf(15.0), BigDecimal.valueOf(10), 3);

        assertEquals(Long.valueOf(100L), d.getItemId());
        assertTrue(d.isAmpp());
        assertEquals(BigDecimal.TEN, d.getQuantity());
        assertEquals(BigDecimal.ONE, d.getFreeQuantity());
        assertEquals(BigDecimal.valueOf(12.5), d.getPurchaseRate());
        assertEquals(BigDecimal.valueOf(15.0), d.getRetailRate());
        assertEquals(BigDecimal.valueOf(10), d.getUnitsPerPack());
        assertEquals(3, d.getSerialNo());
        assertNull(d.getBillItemId());
        assertNull(d.getPharmaceuticalBillItemId());
        assertNull(d.getCreaterId());
    }
}
