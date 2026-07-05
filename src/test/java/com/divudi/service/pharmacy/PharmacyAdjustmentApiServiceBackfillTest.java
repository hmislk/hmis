package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.adjustment.BackfillResultDTO;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PharmacyAdjustmentApiServiceBackfillTest {

    private static class DummyBillFacade extends BillFacade {
        List<Bill> edited = new ArrayList<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public void edit(Bill entity) { edited.add(entity); }
    }

    private PharmacyAdjustmentApiService service;
    private DummyBillFacade billFacade;

    @BeforeEach
    public void setUp() throws Exception {
        service = new PharmacyAdjustmentApiService();
        billFacade = new DummyBillFacade();
        Field f = PharmacyAdjustmentApiService.class.getDeclaredField("billFacade");
        f.setAccessible(true);
        f.set(service, billFacade);
    }

    private Bill buildHistoricalQuantityAdjustmentBill(double beforeQty, double afterQty, double netRate) {
        Bill bill = new Bill();
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);
        bill.setBillFinanceDetails(null); // fingerprint of a pre-fix bill

        ItemBatch itemBatch = new ItemBatch();
        itemBatch.setItem(new Item());
        itemBatch.setRetailsaleRate(netRate);
        itemBatch.setCostRate(netRate * 0.6);
        itemBatch.setPurcahseRate(netRate * 0.7);

        Stock stock = new Stock();
        stock.setItemBatch(itemBatch);

        BillItem billItem = new BillItem();
        billItem.setNetRate(netRate);
        billItem.setBill(bill);
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setCreatedAt(Calendar.getInstance().getTime());

        PharmaceuticalBillItem phItem = new PharmaceuticalBillItem();
        phItem.setStock(stock);
        phItem.setItemBatch(itemBatch);
        phItem.setBeforeAdjustmentValue(beforeQty);
        phItem.setAfterAdjustmentValue(afterQty);
        billItem.setPharmaceuticalBillItem(phItem);
        phItem.setBillItem(billItem);

        bill.getBillItems().add(billItem);
        return bill;
    }

    @Test
    @DisplayName("Backfill computes delta value from stored before/after quantities and writes BFD + bill totals")
    public void testBackfillPopulatesFinanceDetailsFromStoredAuditFields() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0); // delta = 15 * 100 = 1500

        BackfillResultDTO result = service.backfillFinanceDetails(bill, true /* apply */);

        assertTrue(result.isApplied());
        assertEquals(1500.0, bill.getNetTotal(), 0.001);
        assertNotNull(bill.getBillFinanceDetails());
        assertEquals(0, java.math.BigDecimal.valueOf(1500.0).compareTo(bill.getBillFinanceDetails().getTotalRetailSaleValue()));
        assertEquals(1, billFacade.edited.size());

        // Finding 2: totalBeforeAdjustmentValue/totalAfterAdjustmentValue must be populated,
        // computed as quantity * netRate (before=10*100=1000, after=25*100=2500).
        assertNotNull(bill.getBillFinanceDetails().getTotalBeforeAdjustmentValue());
        assertNotNull(bill.getBillFinanceDetails().getTotalAfterAdjustmentValue());
        assertEquals(0, java.math.BigDecimal.valueOf(1000.0).compareTo(bill.getBillFinanceDetails().getTotalBeforeAdjustmentValue()));
        assertEquals(0, java.math.BigDecimal.valueOf(2500.0).compareTo(bill.getBillFinanceDetails().getTotalAfterAdjustmentValue()));

        // Finding 1: cost-value approximation must be disclosed via the note field for this bill type.
        assertNotNull(result.getNote());
        assertTrue(result.getNote().contains("Cost and purchase values approximated using current item batch rates"),
                "Expected note to disclose cost/purchase-value approximation, got: " + result.getNote());
    }

    @Test
    @DisplayName("Backfill for retail-rate adjustment bills uses stored before/after values directly and does not add the cost-approximation note")
    public void testBackfillRetailRateAdjustmentPopulatesBeforeAfterTotalsWithoutApproximationNote() {
        Bill bill = new Bill();
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT);
        bill.setBillFinanceDetails(null);

        ItemBatch itemBatch = new ItemBatch();
        itemBatch.setItem(new Item());

        Stock stock = new Stock();
        stock.setItemBatch(itemBatch);

        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setCreatedAt(Calendar.getInstance().getTime());
        billItem.setQty(50.0);

        PharmaceuticalBillItem phItem = new PharmaceuticalBillItem();
        phItem.setStock(stock);
        phItem.setItemBatch(itemBatch);
        // before/after are already total values (qty * rate) for this bill type.
        phItem.setBeforeAdjustmentValue(50.0 * 10.0); // 500
        phItem.setAfterAdjustmentValue(50.0 * 12.0);  // 600
        billItem.setPharmaceuticalBillItem(phItem);
        phItem.setBillItem(billItem);

        bill.getBillItems().add(billItem);

        BackfillResultDTO result = service.backfillFinanceDetails(bill, true /* apply */);

        assertTrue(result.isApplied());
        assertNotNull(bill.getBillFinanceDetails());
        assertEquals(0, java.math.BigDecimal.valueOf(500.0).compareTo(bill.getBillFinanceDetails().getTotalBeforeAdjustmentValue()));
        assertEquals(0, java.math.BigDecimal.valueOf(600.0).compareTo(bill.getBillFinanceDetails().getTotalAfterAdjustmentValue()));

        // Cost/purchase-value approximation disclosure only applies to PHARMACY_STOCK_ADJUSTMENT bills.
        assertNotNull(result.getNote());
        assertFalse(result.getNote().contains("approximated using current item batch rates"),
                "Retail-rate adjustment backfill should not carry the cost/purchase-approximation note, got: " + result.getNote());
    }

    @Test
    @DisplayName("Backfill for a stock-quantity bill also populates totalPurchaseValue, "
            + "without double-counting it into the bill's netTotal")
    public void testBackfillStockAdjustmentPopulatesPurchaseValueWithoutDoubleCountingNetTotal() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0); // delta qty = 15, purcahseRate = 70.0

        BackfillResultDTO result = service.backfillFinanceDetails(bill, true /* apply */);

        assertTrue(result.isApplied());
        // totalPurchaseValue = 15 * 70.0 = 1050.0 (F15 report's "Stock Value (Purchase)" column)
        assertEquals(0, java.math.BigDecimal.valueOf(1050.0).compareTo(bill.getBillFinanceDetails().getTotalPurchaseValue()));
        // netTotal/computedNetTotal stay scoped to the retail dimension (1500.0) - must NOT
        // become 1500.0 + 1050.0, which would double-count the same quantity change twice.
        assertEquals(1500.0, bill.getNetTotal(), 0.001);
        assertEquals(1500.0, result.getComputedNetTotal(), 0.001);
    }

    @Test
    @DisplayName("Dry run computes the result but does not call billFacade.edit")
    public void testDryRunDoesNotPersist() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0);

        BackfillResultDTO result = service.backfillFinanceDetails(bill, false /* dry run */);

        assertFalse(result.isApplied());
        assertEquals(1500.0, result.getComputedNetTotal(), 0.001);
        assertTrue(billFacade.edited.isEmpty(), "Dry run must not persist changes");
    }

    @Test
    @DisplayName("Bills that already have BillFinanceDetails are skipped, not overwritten")
    public void testAlreadyBackfilledBillIsSkipped() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0);
        bill.setBillFinanceDetails(new com.divudi.core.entity.BillFinanceDetails(bill));
        bill.setNetTotal(999.0); // pretend it was already correctly fixed once

        BackfillResultDTO result = service.backfillFinanceDetails(bill, true);

        assertFalse(result.isApplied());
        assertEquals(999.0, bill.getNetTotal(), 0.001, "Existing BFD must not be recomputed/overwritten");
        assertTrue(billFacade.edited.isEmpty());
    }

    @Test
    @DisplayName("Dry run for stock adjustment includes cost-approximation disclosure in the note")
    public void testDryRunStockAdjustmentIncludesCostApproximationDisclosure() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0);

        BackfillResultDTO result = service.backfillFinanceDetails(bill, false /* dry run */);

        assertFalse(result.isApplied());
        assertEquals(1500.0, result.getComputedNetTotal(), 0.001);
        assertTrue(billFacade.edited.isEmpty(), "Dry run must not persist changes");

        // Verify that dry-run note includes the same cost-approximation disclosure as apply path.
        assertNotNull(result.getNote());
        assertTrue(result.getNote().contains("Dry run: not persisted"),
                "Expected dry-run base message, got: " + result.getNote());
        assertTrue(result.getNote().contains("Cost and purchase values approximated using current item batch rates"),
                "Expected dry-run note to disclose cost/purchase-value approximation, got: " + result.getNote());
    }

    @Test
    @DisplayName("endOfDay normalizes a date to 23:59:59.999 of the same calendar day, "
            + "so a bill created later that day falls within a between(from, endOfDay(toDate)) range")
    public void testEndOfDayIncludesSameDayBillsInBackfillRange() throws Exception {
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dayTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date parsedToDate = dayFormat.parse("2026-07-04"); // midnight, as parseDate() produces
        Date normalizedToDate = service.endOfDay(parsedToDate);

        Calendar cal = Calendar.getInstance();
        cal.setTime(normalizedToDate);
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
        assertEquals(999, cal.get(Calendar.MILLISECOND));
        // Still the same calendar day - only the time-of-day component changed.
        Calendar original = Calendar.getInstance();
        original.setTime(parsedToDate);
        assertEquals(original.get(Calendar.YEAR), cal.get(Calendar.YEAR));
        assertEquals(original.get(Calendar.DAY_OF_YEAR), cal.get(Calendar.DAY_OF_YEAR));

        // A bill created at 14:30:00 on the same day must fall within [from, normalizedToDate],
        // whereas it would have been excluded by the un-normalized midnight bound.
        Date billCreatedAt = dayTimeFormat.parse("2026-07-04 14:30:00");
        assertTrue(billCreatedAt.after(parsedToDate),
                "Sanity check: bill timestamp is after the un-normalized midnight bound");
        assertFalse(billCreatedAt.after(normalizedToDate),
                "Bill created at 14:30:00 on toDate must be included once toDate is normalized to end-of-day");
    }
}
