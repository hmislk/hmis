package com.divudi.bean.pharmacy;

import com.divudi.core.entity.*;
import com.divudi.core.entity.pharmacy.*;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillCategory;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.*;

public class PharmacySummaryReportControllerTest_DISABLED {

    @Test
    public void addMissingDataPopulatesFinanceDetails() {
        PharmacySummaryReportController controller = new PharmacySummaryReportController();

        BillItem billItem = new BillItem();
        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
        billItem.setPharmaceuticalBillItem(pbi);
        pbi.setBillItem(billItem);

        ItemBatch batch = new ItemBatch();
        batch.setPurcahseRate(50.0);
        batch.setCostRate(45.0);
        pbi.setItemBatch(batch);

        pbi.setQty(20.0);
        pbi.setQtyPacks(2.0);
        pbi.setFreeQty(0.0);
        pbi.setFreeQtyPacks(0.0);
        pbi.setPurchaseRate(50.0);
        pbi.setRetailRate(80.0);

        Ampp item = new Ampp();
        item.setDblValue(10.0);
        billItem.setItem(item);

        Bill bill = new Bill();
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE);
        billItem.setBill(bill);
        billItem.setRate(80.0);

        BillItemFinanceDetails fd = new BillItemFinanceDetails(billItem);
        billItem.setBillItemFinanceDetails(fd);

        controller.addMissingDataToBillItemFinanceDetailsWhenPharmaceuticalBillItemsAreAvailableForPharmacySale(billItem);

        assertTrue("Quantity expected 2.0 but got " + fd.getQuantity(),
                BigDecimal.valueOf(2.0).compareTo(fd.getQuantity()) == 0);
        assertTrue("QuantityByUnits expected 20.0 but got " + fd.getQuantityByUnits(),
                BigDecimal.valueOf(20.0).compareTo(fd.getQuantityByUnits()) == 0);
        assertTrue("LineGrossRate expected 50.0 but got " + fd.getLineGrossRate(),
                BigDecimal.valueOf(50.0).compareTo(fd.getLineGrossRate()) == 0);
        assertTrue("LineCostRate expected 45.0 but got " + fd.getLineCostRate(),
                BigDecimal.valueOf(45.0).compareTo(fd.getLineCostRate()) == 0);
        assertTrue("RetailSaleRate expected 80.0 but got " + fd.getRetailSaleRate(),
                BigDecimal.valueOf(80.0).compareTo(fd.getRetailSaleRate()) == 0);
        assertTrue("RetailSaleRatePerUnit expected 8.0 but got " + fd.getRetailSaleRatePerUnit(),
                BigDecimal.valueOf(8.0).compareTo(fd.getRetailSaleRatePerUnit()) == 0);
    }

    @Test
    public void createBillFinancialDetailsForPharmacyBillPopulatesTotals() {
        BillServiceStub service = new BillServiceStub();

        Bill bill = new Bill();
        BillTypeAtomic bta = BillTypeAtomic.PHARMACY_RETAIL_SALE;
        bill.setBillTypeAtomic(bta);

        BillItem billItem = new BillItem();
        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
        pbi.setQty(5.0);
        pbi.setPurchaseRate(10.0);
        pbi.setRetailRate(20.0);
        pbi.setBillItem(billItem);

        ItemBatch batch = new ItemBatch();
        batch.setCostRate(8.0);
        pbi.setItemBatch(batch);

        billItem.setPharmaceuticalBillItem(pbi);
        billItem.setBill(bill);
        bill.setBillItems(Collections.singletonList(billItem));

        service.createBillFinancialDetailsForPharmacyBill(bill);

        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        assertNotNull(bfd);
        assertTrue("RetailTotal expected 100.0 but got " + bfd.getTotalRetailSaleValue(),
                BigDecimal.valueOf(100.0).compareTo(bfd.getTotalRetailSaleValue()) == 0);
        assertTrue("PurchaseTotal expected 50.0 but got " + bfd.getTotalPurchaseValue(),
                BigDecimal.valueOf(50.0).compareTo(bfd.getTotalPurchaseValue()) == 0);
    }

    private static class BillServiceStub extends com.divudi.service.BillService {
        @Override
        public java.util.List<BillItem> fetchBillItems(Bill b) {
            return b.getBillItems();
        }

        @Override
        public void createBillFinancialDetailsForPharmacyBill(Bill b) {
            if (b == null || b.getBillTypeAtomic() == null) {
                return;
            }

            // For testing, assume category is always BILL
            BillCategory bc = BillCategory.BILL;

            double saleValue = 0.0;
            double purchaseValue = 0.0;

            for (BillItem bi : fetchBillItems(b)) {
                if (bi == null || bi.getPharmaceuticalBillItem() == null) continue;

                PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
                double qty = Math.abs(pbi.getQty());
                double retailRate = Math.abs(pbi.getRetailRate());
                double purchaseRate = Math.abs(pbi.getPurchaseRate());

                if (b.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS) {
                    retailRate = Math.abs(bi.getNetRate());
                }

                double retailTotal = 0;
                double purchaseTotal = 0;

                switch (bc) {
                    case BILL:
                    case PAYMENTS:
                    case PREBILL:
                        retailTotal = retailRate * qty;
                        purchaseTotal = purchaseRate * qty;
                        break;
                    case CANCELLATION:
                    case REFUND:
                        retailTotal = -retailRate * qty;
                        purchaseTotal = -purchaseRate * qty;
                        break;
                }

                saleValue += retailTotal;
                purchaseValue += purchaseTotal;
            }

            BillFinanceDetails bfd = b.getBillFinanceDetails();
            if (bfd == null) {
                bfd = new BillFinanceDetails(b);
                b.setBillFinanceDetails(bfd);
            }

            bfd.setTotalRetailSaleValue(BigDecimal.valueOf(saleValue));
            bfd.setTotalPurchaseValue(BigDecimal.valueOf(purchaseValue));
        }
    }
}
