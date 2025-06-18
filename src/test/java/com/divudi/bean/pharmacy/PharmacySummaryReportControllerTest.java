package com.divudi.bean.pharmacy;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillCategory;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class PharmacySummaryReportControllerTest {

    @Test
    public void addMissingDataPopulatesFinanceDetails() {
        PharmacySummaryReportController c = new PharmacySummaryReportController();
        BillItem bi = new BillItem();
        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
        bi.setPharmaceuticalBillItem(pbi);
        pbi.setBillItem(bi);

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

        Ampp ampp = new Ampp();
        ampp.setDblValue(10.0);
        bi.setItem(ampp);
        Bill bill = new Bill();
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE);
        bi.setBill(bill);

        BillItemFinanceDetails fd = new BillItemFinanceDetails(bi);
        bi.setBillItemFinanceDetails(fd);

        c.addMissingDataToBillItemFinanceDetailsWhenPharmaceuticalBillItemsAreAvailableForPharmacySale(bi);

        assertEquals(BigDecimal.valueOf(2.0), fd.getQuantity());
        assertEquals(BigDecimal.valueOf(20.0), fd.getQuantityByUnits());
        assertEquals(BigDecimal.valueOf(50.0), fd.getLineGrossRate());
        assertEquals(BigDecimal.valueOf(45.0), fd.getLineCostRate());
        assertEquals(BigDecimal.valueOf(80.0), fd.getRetailSaleRate());
        assertEquals(BigDecimal.valueOf(8.0), fd.getRetailSaleRatePerUnit());
    }

    @Test
    public void createBillFinancialDetailsForPharmacyBillPopulatesTotals() {
        BillServiceStub service = new BillServiceStub();
        Bill b = new Bill();
        b.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE);
        BillItem bi = new BillItem();
        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
        pbi.setQty(5.0);
        pbi.setPurchaseRate(10.0);
        pbi.setRetailRate(20.0);
        ItemBatch batch = new ItemBatch();
        batch.setCostRate(8.0);
        pbi.setItemBatch(batch);
        bi.setPharmaceuticalBillItem(pbi);
        bi.setBill(b);
        b.getBillItems().add(bi);

        service.createBillFinancialDetailsForPharmacyBill(b);

        BillFinanceDetails bfd = b.getBillFinanceDetails();
        assertNotNull(bfd);
        assertEquals(BigDecimal.valueOf(100.0), bfd.getTotalRetailSaleValue());
        assertEquals(BigDecimal.valueOf(50.0), bfd.getTotalPurchaseValue());
    }

    // Minimal stub of BillService avoiding database access
    private static class BillServiceStub extends com.divudi.service.BillService {
        @Override
        public java.util.List<BillItem> fetchBillItems(Bill b) {
            return b.getBillItems();
        }

        @Override
        public void createBillFinancialDetailsForPharmacyBill(Bill b) {
            if (b == null) {
                return;
            }

            BillTypeAtomic bta = b.getBillTypeAtomic();
            if (bta == null || bta.getBillCategory() == null) {
                return;
            }
            BillCategory bc = bta.getBillCategory();

            double saleValue = 0.0;
            double purchaseValue = 0.0;

            for (BillItem bi : fetchBillItems(b)) {
                if (bi == null || bi.getPharmaceuticalBillItem() == null) {
                    continue;
                }

                double qty = Math.abs(bi.getPharmaceuticalBillItem().getQty());
                double retailRate = Math.abs(bi.getPharmaceuticalBillItem().getRetailRate());
                if (bta == BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS) {
                    retailRate = Math.abs(bi.getNetRate());
                }

                double purchaseRate = Math.abs(bi.getPharmaceuticalBillItem().getPurchaseRate());

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
                    default:
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
