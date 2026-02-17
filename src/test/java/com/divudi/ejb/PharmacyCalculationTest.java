package com.divudi.ejb;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.data.BillType;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PharmacyCalculationTest {

    @Test
    public void skipsItemsWithoutPharmaceuticalBillItem() {
        PharmacyCalculation calc = new PharmacyCalculation();

        Bill bill = new Bill();
        bill.setBillType(BillType.PharmacyGrnBill);

        List<BillItem> items = new ArrayList<>();

        BillItem withPh = new BillItem();
        PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
        ph.setBillItem(withPh);
        ph.setQty(2);
        ph.setFreeQty(1);
        ph.setRetailRate(10.0);
        ph.setPurchaseRate(5.0);
        withPh.setPharmaceuticalBillItem(ph);
        items.add(withPh);

        BillItem withoutPh = new BillItem();
        items.add(withoutPh);

        bill.setBillItems(items);

        calc.calculateRetailSaleValueAndFreeValueAtPurchaseRate(bill);

        assertEquals(30.0, bill.getSaleValue(), 0.001);
        assertEquals(5.0, bill.getFreeValue(), 0.001);
    }
}
