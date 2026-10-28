package com.divudi.bean.pharmacy;

import com.divudi.core.entity.BillItem;
import com.divudi.ejb.PharmacyCalculation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PreReturnController Return Value Calculation Tests")
public class PreReturnControllerReturnValueTest {

    private PreReturnController controller;
    private PharmacyCalculation stubPharmacyCalculation;

    @BeforeEach
    public void setUp() throws Exception {
        controller = new PreReturnController();

        stubPharmacyCalculation = new PharmacyCalculation() {
            @Override
            public double calQty3(BillItem bil) {
                return 10.0; // allows returning up to 10 units
            }
        };

        Field field = PreReturnController.class.getDeclaredField("pharmacyRecieveBean");
        field.setAccessible(true);
        field.set(controller, stubPharmacyCalculation);
    }

    @Test
    @DisplayName("onEdit should calculate return value as netRate * qty when valid qty is entered")
    public void testOnEditCalculatesReturnValue() {
        BillItem bi = new BillItem();
        bi.setNetRate(45.50);
        bi.setQty(2.0);

        List<BillItem> items = new ArrayList<>();
        items.add(bi);
        controller.setBillItems(items);

        controller.onEdit(bi);

        assertEquals(91.0, bi.getNetValue(), 0.001, "Return netValue should equal netRate * qty");
        assertEquals(91.0, bi.getGrossValue(), 0.001, "Return grossValue should equal netRate * qty");
        assertEquals(91.0, controller.getReturnBill().getNetTotal(), 0.001, "Return bill netTotal should equal 91.0");
    }

    @Test
    @DisplayName("onEdit should reset netValue and grossValue to 0 when qty is set to 0")
    public void testOnEditWithZeroQty() {
        BillItem bi = new BillItem();
        bi.setNetRate(45.50);
        bi.setQty(0.0);
        bi.setNetValue(45.50); // previously held non-zero from original line

        List<BillItem> items = new ArrayList<>();
        items.add(bi);
        controller.setBillItems(items);

        controller.onEdit(bi);

        assertEquals(0.0, bi.getNetValue(), 0.001, "Return netValue should reset to 0.0 when qty is 0");
        assertEquals(0.0, bi.getGrossValue(), 0.001, "Return grossValue should reset to 0.0 when qty is 0");
        assertEquals(0.0, controller.getReturnBill().getNetTotal(), 0.001, "Return bill netTotal should be 0.0");
    }

    @Test
    @DisplayName("onEdit should calculate partial return quantity value correctly")
    public void testOnEditWithPartialQty() {
        BillItem bi = new BillItem();
        bi.setNetRate(120.0);
        bi.setQty(0.5);

        List<BillItem> items = new ArrayList<>();
        items.add(bi);
        controller.setBillItems(items);

        controller.onEdit(bi);

        assertEquals(60.0, bi.getNetValue(), 0.001, "Return netValue should equal 120.0 * 0.5 = 60.0");
        assertEquals(60.0, bi.getGrossValue(), 0.001, "Return grossValue should equal 120.0 * 0.5 = 60.0");
        assertEquals(60.0, controller.getReturnBill().getNetTotal(), 0.001, "Return bill netTotal should equal 60.0");
    }

    @Test
    @DisplayName("Multiple items onEdit should update each item value and total correctly")
    public void testMultipleItemsOnEdit() {
        BillItem bi1 = new BillItem();
        bi1.setNetRate(20.0);
        bi1.setQty(3.0);

        BillItem bi2 = new BillItem();
        bi2.setNetRate(50.0);
        bi2.setQty(1.0);

        List<BillItem> items = new ArrayList<>();
        items.add(bi1);
        items.add(bi2);
        controller.setBillItems(items);

        controller.onEdit(bi1);
        controller.onEdit(bi2);

        assertEquals(60.0, bi1.getNetValue(), 0.001);
        assertEquals(50.0, bi2.getNetValue(), 0.001);
        assertEquals(110.0, controller.getReturnBill().getNetTotal(), 0.001);
    }
}
