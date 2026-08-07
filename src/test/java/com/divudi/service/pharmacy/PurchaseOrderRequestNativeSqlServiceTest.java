package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurchaseOrderRequestNativeSqlServiceTest {

    private final PurchaseOrderRequestNativeSqlService service = new PurchaseOrderRequestNativeSqlService();

    @Test
    void computeLineValues_nonAmpp_purchaseRateTimesQtyIsGrossAndNetValue() {
        PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
        line.setAmpp(false);
        line.setQuantity(new BigDecimal("10"));
        line.setFreeQuantity(new BigDecimal("1"));
        line.setPurchaseRate(new BigDecimal("25.50"));
        line.setRetailRate(new BigDecimal("30.00"));
        line.setUnitsPerPack(BigDecimal.ONE);

        PurchaseOrderRequestNativeSqlService.LineValues v = service.computeLineValues(line);

        assertEquals(new BigDecimal("255.00"), v.grossValue.setScale(2));
        assertEquals(new BigDecimal("255.00"), v.netValue.setScale(2));
        assertEquals(new BigDecimal("280.50"), v.purchaseValue.setScale(2)); // 25.50 * (10+1)
        assertEquals(new BigDecimal("330.00"), v.retailValue.setScale(2));  // 30.00 * (10+1)
        assertEquals(25.50, v.pbiPurchaseRate, 0.0001); // non-AMPP: no pack conversion
        assertEquals(10.0, v.pbiQty, 0.0001);
    }

    @Test
    void computeLineValues_ampp_convertsQtyAndRateByUnitsPerPack() {
        PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
        line.setAmpp(true);
        line.setQuantity(new BigDecimal("2")); // 2 packs
        line.setFreeQuantity(BigDecimal.ZERO);
        line.setPurchaseRate(new BigDecimal("100")); // rate per pack
        line.setRetailRate(new BigDecimal("120"));
        line.setUnitsPerPack(new BigDecimal("10")); // 10 units per pack

        PurchaseOrderRequestNativeSqlService.LineValues v = service.computeLineValues(line);

        assertEquals(20.0, v.pbiQty, 0.0001); // 2 packs * 10 units/pack
        assertEquals(10.0, v.pbiPurchaseRate, 0.0001); // 100 / 10 units per pack
        assertEquals(12.0, v.pbiRetailRate, 0.0001); // 120 / 10
    }

    @Test
    void computeLineValues_zeroQuantity_netRateIsZeroNotDivideByZero() {
        PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
        line.setAmpp(false);
        line.setQuantity(BigDecimal.ZERO);
        line.setFreeQuantity(BigDecimal.ZERO);
        line.setPurchaseRate(new BigDecimal("50"));
        line.setRetailRate(new BigDecimal("60"));
        line.setUnitsPerPack(BigDecimal.ONE);

        PurchaseOrderRequestNativeSqlService.LineValues v = service.computeLineValues(line);

        assertEquals(BigDecimal.ZERO.setScale(2), v.netRate.setScale(2));
    }

    @Test
    void computeLineValues_nullFields_defaultToZeroOrOne() {
        PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
        // quantity, freeQuantity, purchaseRate, retailRate, unitsPerPack all left null

        PurchaseOrderRequestNativeSqlService.LineValues v = service.computeLineValues(line);

        assertEquals(BigDecimal.ZERO.setScale(2), v.grossValue.setScale(2));
        assertEquals(BigDecimal.ONE, v.unitsPerPack);
    }
}
