package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.StockHistory;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFinanceDetailsFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.service.pharmacy.PharmacyBfdBackfillService.BackfillStatus;
import com.divudi.service.pharmacy.PharmacyBfdBackfillService.BillBackfillResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the per-bill backfill computation.
 *
 * <p>The derivation moved out of {@code PharmacyAdjustmentApiService} into
 * {@link PharmacyBfdBackfillService} for #23411, so that the admin buttons, the adjustment
 * API and {@code POST /api/pharmacy/backfill_bfd} all compute the same figures. These tests
 * follow it there; the date-boundary helper still belongs to the API service and is still
 * tested against it.</p>
 */
public class PharmacyAdjustmentApiServiceBackfillTest {

    private static class DummyBillFacade extends BillFacade {
        List<Bill> edited = new ArrayList<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public void edit(Bill entity) { edited.add(entity); }
    }

    private static class DummyBfdFacade extends BillFinanceDetailsFacade {
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public void create(com.divudi.core.entity.BillFinanceDetails entity) { }
        @Override public void edit(com.divudi.core.entity.BillFinanceDetails entity) { }
    }

    private static class NoSnapshotStockHistoryFacade extends StockHistoryFacade {
        @Override public StockHistory findByPharmaceuticalBillItem(PharmaceuticalBillItem pbItem) {
            return null;
        }
    }

    private PharmacyBfdBackfillService service;
    private PharmacyAdjustmentApiService apiService;
    private DummyBillFacade billFacade;

    @BeforeEach
    public void setUp() throws Exception {
        service = new PharmacyBfdBackfillService();
        billFacade = new DummyBillFacade();
        inject(service, "billFacade", billFacade);
        inject(service, "billFinanceDetailsFacade", new DummyBfdFacade());
        inject(service, "stockHistoryFacade", new NoSnapshotStockHistoryFacade());

        apiService = new PharmacyAdjustmentApiService();
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private BillBackfillResult run(Bill bill, boolean apply) {
        return service.applyToBill(bill, apply, "unit test", "unit test", null);
    }

    /**
     * A stock-quantity adjustment as the save path writes it: before/after hold quantities,
     * {@code pbi.qty} the signed delta, and the retail rate of the moment is on the bill item.
     */
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
        phItem.setQty(afterQty - beforeQty);
        billItem.setPharmaceuticalBillItem(phItem);
        phItem.setBillItem(billItem);

        bill.getBillItems().add(billItem);
        return bill;
    }

    @Test
    @DisplayName("Backfill prefers the StockHistory rate snapshot at adjustment time over the item batch's "
            + "current rate, and does not disclose an approximation when a snapshot is found")
    public void testBackfillUsesHistoricalStockHistoryRateNotCurrentRate() throws Exception {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0); // delta qty = 15
        PharmaceuticalBillItem ph = bill.getBillItems().get(0).getPharmaceuticalBillItem();
        // Current item batch rates are 60.0 / 70.0. The snapshot taken at adjustment time
        // says 55.0 / 65.0 — simulating a rate change made after this bill but before the
        // backfill runs. Using the current rate would silently value the bill wrongly.
        StockHistory snapshot = new StockHistory();
        snapshot.setCostRate(55.0);
        snapshot.setPurchaseRate(65.0);

        inject(service, "stockHistoryFacade", new StockHistoryFacade() {
            @Override public StockHistory findByPharmaceuticalBillItem(PharmaceuticalBillItem pbItem) {
                return pbItem == ph ? snapshot : null;
            }
        });

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.UPDATED, result.getStatus());
        // Historical: 15 * 55.0 = 825.0, not 15 * 60.0 = 900.0 (the current rate).
        assertEquals(0, BigDecimal.valueOf(825.0).compareTo(bill.getBillFinanceDetails().getTotalCostValue()),
                "Expected cost value from the historical StockHistory snapshot (55.0), not the current rate (60.0)");
        // Historical: 15 * 65.0 = 975.0, not 15 * 70.0 = 1050.0.
        assertEquals(0, BigDecimal.valueOf(975.0).compareTo(bill.getBillFinanceDetails().getTotalPurchaseValue()),
                "Expected purchase value from the historical StockHistory snapshot (65.0), not the current rate (70.0)");

        assertNotNull(result.getNote());
        assertFalse(result.getNote().contains("approximated using current item batch rates"),
                "A real historical snapshot was found, so no approximation should be disclosed, got: " + result.getNote());
    }

    @Test
    @DisplayName("Backfill computes delta value from stored before/after quantities and writes BFD + bill totals")
    public void testBackfillPopulatesFinanceDetailsFromStoredAuditFields() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0); // delta = 15 * 100 = 1500

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.UPDATED, result.getStatus());
        assertEquals(1500.0, bill.getNetTotal(), 0.001);
        assertNotNull(bill.getBillFinanceDetails());
        assertEquals(0, BigDecimal.valueOf(1500.0).compareTo(bill.getBillFinanceDetails().getTotalRetailSaleValue()));
        assertEquals(1, billFacade.edited.size());

        // before/after totals are quantity * rate: 10*100 = 1000, 25*100 = 2500.
        assertNotNull(bill.getBillFinanceDetails().getTotalBeforeAdjustmentValue());
        assertNotNull(bill.getBillFinanceDetails().getTotalAfterAdjustmentValue());
        assertEquals(0, BigDecimal.valueOf(1000.0).compareTo(bill.getBillFinanceDetails().getTotalBeforeAdjustmentValue()));
        assertEquals(0, BigDecimal.valueOf(2500.0).compareTo(bill.getBillFinanceDetails().getTotalAfterAdjustmentValue()));

        // No StockHistory snapshot exists here, so the cost/purchase estimate must be disclosed.
        assertNotNull(result.getNote());
        assertTrue(result.getNote().contains("Cost and purchase values approximated using current item batch rates"),
                "Expected the cost/purchase approximation to be disclosed, got: " + result.getNote());
    }

    @Test
    @DisplayName("A retail-rate bill written by the API is read as extended values, and carries no cost note")
    public void testBackfillRetailRateAdjustmentPopulatesBeforeAfterTotalsWithoutApproximationNote() {
        // The API writer stored before/after as qty * rate and the signed change in netValue.
        Bill bill = buildRetailRateBill(50.0, 50.0 * 10.0, 50.0 * 12.0, 100.0);

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.UPDATED, result.getStatus());
        assertNotNull(bill.getBillFinanceDetails());
        assertEquals(0, BigDecimal.valueOf(500.0).compareTo(bill.getBillFinanceDetails().getTotalBeforeAdjustmentValue()));
        assertEquals(0, BigDecimal.valueOf(600.0).compareTo(bill.getBillFinanceDetails().getTotalAfterAdjustmentValue()));
        assertEquals(0, BigDecimal.valueOf(100.0).compareTo(bill.getBillFinanceDetails().getTotalRetailSaleValue()));

        // A rate change moves no quantity, so there is no cost estimate to disclose.
        assertNotNull(result.getNote());
        assertFalse(result.getNote().contains("approximated using current item batch rates"),
                "A retail-rate backfill should carry no cost approximation note, got: " + result.getNote());
    }

    @Test
    @DisplayName("A retail-rate bill written by the UI page is read as unit rates over the same line")
    public void testBackfillRetailRateAdjustmentReadsUnitRates() {
        // Same economic event as the test above — 50 units moving from 10.00 to 12.00 — but
        // recorded by the UI writer, which stores unit rates. The BFD must come out identical.
        Bill bill = buildRetailRateBill(50.0, 10.0, 12.0, 100.0);

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.UPDATED, result.getStatus());
        assertEquals(0, BigDecimal.valueOf(500.0).compareTo(bill.getBillFinanceDetails().getTotalBeforeAdjustmentValue()));
        assertEquals(0, BigDecimal.valueOf(600.0).compareTo(bill.getBillFinanceDetails().getTotalAfterAdjustmentValue()));
        assertEquals(0, BigDecimal.valueOf(100.0).compareTo(bill.getBillFinanceDetails().getTotalRetailSaleValue()));
    }

    @Test
    @DisplayName("A retail-rate line matching neither reading is reported, not valued")
    public void testUnreadableRetailRateBillIsReportedNotGuessed() {
        // The legacy single-item page shape: qty never set, netValue holding a total.
        Bill bill = buildRetailRateBill(0.0, 100.0, 120.0, 6000.0);

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.UNRESOLVED, result.getStatus());
        // hasBillFinanceDetails(), not getBillFinanceDetails() — the getter auto-creates one.
        assertFalse(bill.hasBillFinanceDetails(), "An unreadable bill must be left untouched");
        assertTrue(billFacade.edited.isEmpty());
        assertTrue(result.getNote().contains("Neither reading"), "got: " + result.getNote());
    }

    private Bill buildRetailRateBill(double qty, double before, double after, double signedChange) {
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
        billItem.setQty(qty);
        billItem.setNetValue(signedChange);
        billItem.setGrossValue(Math.abs(signedChange));

        PharmaceuticalBillItem phItem = new PharmaceuticalBillItem();
        phItem.setStock(stock);
        phItem.setItemBatch(itemBatch);
        phItem.setBeforeAdjustmentValue(before);
        phItem.setAfterAdjustmentValue(after);
        billItem.setPharmaceuticalBillItem(phItem);
        phItem.setBillItem(billItem);

        bill.getBillItems().add(billItem);
        return bill;
    }

    @Test
    @DisplayName("Backfill for a stock-quantity bill also populates totalPurchaseValue, "
            + "without double-counting it into the bill's netTotal")
    public void testBackfillStockAdjustmentPopulatesPurchaseValueWithoutDoubleCountingNetTotal() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0); // delta qty = 15, purcahseRate = 70.0

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.UPDATED, result.getStatus());
        // totalPurchaseValue = 15 * 70.0 = 1050.0 (F15's "Stock Value (Purchase)" column)
        assertEquals(0, BigDecimal.valueOf(1050.0).compareTo(bill.getBillFinanceDetails().getTotalPurchaseValue()));
        // netTotal stays scoped to the retail dimension (1500.0) — it must NOT become
        // 1500.0 + 1050.0, which would count the same quantity change twice.
        assertEquals(1500.0, bill.getNetTotal(), 0.001);
        assertEquals(0, BigDecimal.valueOf(1500.0).compareTo(result.getComputedNetTotal()));
    }

    @Test
    @DisplayName("Dry run computes the result but does not call billFacade.edit")
    public void testDryRunDoesNotPersist() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0);

        BillBackfillResult result = run(bill, false);

        assertEquals(BackfillStatus.WOULD_UPDATE, result.getStatus());
        assertEquals(0, BigDecimal.valueOf(1500.0).compareTo(result.getComputedNetTotal()));
        assertTrue(billFacade.edited.isEmpty(), "Dry run must not persist changes");
        assertFalse(bill.hasBillFinanceDetails(), "Dry run must not attach a BFD to the bill");
    }

    @Test
    @DisplayName("Bills with a populated BillFinanceDetails are skipped, not overwritten")
    public void testAlreadyBackfilledBillIsSkipped() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0);
        // "Already fixed" means a BFD carrying a real value and a non-zero bill total. A BFD
        // row with null/zero values is the broken state the backfill exists to repair, so it
        // does not count as already fixed.
        com.divudi.core.entity.BillFinanceDetails bfd = new com.divudi.core.entity.BillFinanceDetails(bill);
        bfd.setTotalRetailSaleValue(BigDecimal.valueOf(999.0));
        bfd.setNetTotal(BigDecimal.valueOf(999.0));
        bfd.setGrossTotal(BigDecimal.valueOf(999.0));
        bill.setBillFinanceDetails(bfd);
        bill.setTotal(999.0);
        bill.setNetTotal(999.0);

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.SKIPPED, result.getStatus());
        assertEquals(999.0, bill.getNetTotal(), 0.001, "An already-populated BFD must not be recomputed");
        assertTrue(billFacade.edited.isEmpty());
    }

    @Test
    @DisplayName("A BFD row that exists but carries no value is repaired, not treated as done")
    public void testEmptyBillFinanceDetailsIsRepaired() {
        // This is the coop production shape: 26 of 27 retail-rate bills had a BFD row and a
        // bill.total of 0, so an "IS NULL"-only backfill never touched them.
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0);
        bill.setBillFinanceDetails(new com.divudi.core.entity.BillFinanceDetails(bill));

        assertTrue(service.needsCorrection(bill));

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.UPDATED, result.getStatus());
        assertEquals(1500.0, bill.getNetTotal(), 0.001);
    }

    @Test
    @DisplayName("Dry run for stock adjustment includes cost-approximation disclosure in the note")
    public void testDryRunStockAdjustmentIncludesCostApproximationDisclosure() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0);

        BillBackfillResult result = run(bill, false);

        assertEquals(BackfillStatus.WOULD_UPDATE, result.getStatus());
        assertEquals(0, BigDecimal.valueOf(1500.0).compareTo(result.getComputedNetTotal()));
        assertTrue(billFacade.edited.isEmpty(), "Dry run must not persist changes");

        assertNotNull(result.getNote());
        assertTrue(result.getNote().contains("Dry run: not persisted"),
                "Expected the dry-run base message, got: " + result.getNote());
        assertTrue(result.getNote().contains("Cost and purchase values approximated using current item batch rates"),
                "Expected the dry run to disclose the same approximation as the apply path, got: " + result.getNote());
    }

    // ------------------------------------------------------------------
    // Purchase rate adjustments — the dimension that is NOT retail
    // ------------------------------------------------------------------

    private Bill buildPurchaseRateBill(double qty, double before, double after, double signedChange) {
        Bill bill = new Bill();
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT);
        bill.setBillFinanceDetails(null);

        ItemBatch itemBatch = new ItemBatch();
        itemBatch.setItem(new Item());

        Stock stock = new Stock();
        stock.setItemBatch(itemBatch);

        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setCreatedAt(Calendar.getInstance().getTime());
        billItem.setQty(qty);
        billItem.setNetValue(signedChange);
        billItem.setGrossValue(Math.abs(signedChange));

        PharmaceuticalBillItem phItem = new PharmaceuticalBillItem();
        phItem.setStock(stock);
        phItem.setItemBatch(itemBatch);
        phItem.setBeforeAdjustmentValue(before);
        phItem.setAfterAdjustmentValue(after);
        billItem.setPharmaceuticalBillItem(phItem);
        phItem.setBillItem(billItem);

        bill.getBillItems().add(billItem);
        return bill;
    }

    @Test
    @DisplayName("A purchase rate change lands in the purchase column only, never the retail one")
    public void testPurchaseRateChangeDoesNotTouchRetailValue() {
        // 100 units, purchase rate 50.00 -> 45.00, so the change is -500.00.
        Bill bill = buildPurchaseRateBill(100.0, 50.0, 45.0, -500.0);

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.UPDATED, result.getStatus());
        assertEquals(0, BigDecimal.valueOf(-500.0).compareTo(bill.getBillFinanceDetails().getTotalPurchaseValue()));
        // A purchase rate move does not change what the stock sells for. Writing the same
        // figure into the retail column would report one movement as two, and would
        // contradict what the save path stores (an explicit retail value of zero).
        assertEquals(0, BigDecimal.ZERO.compareTo(bill.getBillFinanceDetails().getTotalRetailSaleValue()),
                "A purchase rate adjustment must leave the retail column at zero");
        // The bill's headline value still comes from the dimension that actually moved.
        assertEquals(-500.0, bill.getNetTotal(), 0.001);
        assertEquals(500.0, bill.getTotal(), 0.001);
        assertEquals(0, BigDecimal.valueOf(-500.0).compareTo(result.getComputedNetTotal()));
    }

    @Test
    @DisplayName("A correctly written purchase rate bill is not a backfill candidate, so re-runs cannot corrupt it")
    public void testCorrectPurchaseRateBillIsNotSelectedForRepair() {
        // As the save path writes it: the change in the purchase column, retail explicitly
        // zero, and the same change mirrored into netTotal / bill totals.
        Bill bill = buildPurchaseRateBill(100.0, 50.0, 45.0, -500.0);
        com.divudi.core.entity.BillFinanceDetails bfd = new com.divudi.core.entity.BillFinanceDetails(bill);
        bfd.setTotalPurchaseValue(BigDecimal.valueOf(-500.0));
        bfd.setTotalRetailSaleValue(BigDecimal.ZERO);
        bfd.setNetTotal(BigDecimal.valueOf(-500.0));
        bfd.setGrossTotal(BigDecimal.valueOf(500.0));
        bill.setBillFinanceDetails(bfd);
        bill.setTotal(500.0);
        bill.setNetTotal(-500.0);

        assertFalse(service.needsCorrection(bill),
                "A purchase rate bill's retail value is legitimately zero — that must not read as broken");

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.SKIPPED, result.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(bill.getBillFinanceDetails().getTotalRetailSaleValue()),
                "Re-running the backfill must not write the purchase change into the retail column");
        assertTrue(billFacade.edited.isEmpty());
    }

    @Test
    @DisplayName("A purchase rate bill with no finance details at all is still repaired")
    public void testMissingBfdOnPurchaseRateBillIsStillRepaired() {
        Bill bill = buildPurchaseRateBill(100.0, 50.0, 45.0, -500.0);

        assertTrue(service.needsCorrection(bill));
        assertEquals(BackfillStatus.UPDATED, run(bill, true).getStatus());
    }

    // ------------------------------------------------------------------
    // Rate-source disclosure
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A point-in-time cost rate on the bill item is not reported as an approximation")
    public void testPointInTimeCostRateIsNotDisclosedAsApproximate() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0);
        PharmaceuticalBillItem ph = bill.getBillItems().get(0).getPharmaceuticalBillItem();
        // A real cost rate recorded on the line at the time — the first source in the chain.
        ph.setCostRate(58.0);
        // The purchase rate still has to fall back, so provide a snapshot for it.
        StockHistory snapshot = new StockHistory();
        snapshot.setPurchaseRate(65.0);
        setSnapshot(ph, snapshot);

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.UPDATED, result.getStatus());
        assertEquals(0, BigDecimal.valueOf(870.0).compareTo(bill.getBillFinanceDetails().getTotalCostValue()),
                "15 * 58.0 from the line's own recorded cost rate");
        assertFalse(result.getNote().contains("approximated using current item batch rates"),
                "Every rate came from a point-in-time source, so nothing should be disclosed: " + result.getNote());
    }

    @Test
    @DisplayName("A snapshot that carries no cost rate still falls back to the current rate, and says so")
    public void testSnapshotWithoutCostRateStillDisclosesApproximation() {
        Bill bill = buildHistoricalQuantityAdjustmentBill(10.0, 25.0, 100.0);
        PharmaceuticalBillItem ph = bill.getBillItems().get(0).getPharmaceuticalBillItem();
        // A snapshot exists, but its cost rate was never recorded. Testing only for the
        // snapshot's presence would call the resulting figure exact when it is not.
        StockHistory snapshot = new StockHistory();
        snapshot.setPurchaseRate(65.0);
        snapshot.setCostRate(0.0);
        setSnapshot(ph, snapshot);

        BillBackfillResult result = run(bill, true);

        assertEquals(BackfillStatus.UPDATED, result.getStatus());
        // Fell through to the item batch's current cost rate of 60.0: 15 * 60.0.
        assertEquals(0, BigDecimal.valueOf(900.0).compareTo(bill.getBillFinanceDetails().getTotalCostValue()));
        assertTrue(result.getNote().contains("approximated using current item batch rates"),
                "The cost rate came from the current batch rate and must be disclosed: " + result.getNote());
    }

    private void setSnapshot(PharmaceuticalBillItem target, StockHistory snapshot) {
        try {
            inject(service, "stockHistoryFacade", new StockHistoryFacade() {
                @Override public StockHistory findByPharmaceuticalBillItem(PharmaceuticalBillItem pbItem) {
                    return pbItem == target ? snapshot : null;
                }
            });
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    @DisplayName("endOfDay normalizes a date to 23:59:59.999 of the same calendar day, "
            + "so a bill created later that day falls within a between(from, endOfDay(toDate)) range")
    public void testEndOfDayIncludesSameDayBillsInBackfillRange() throws Exception {
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dayTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date parsedToDate = dayFormat.parse("2026-07-04"); // midnight, as parseDate() produces
        Date normalizedToDate = apiService.endOfDay(parsedToDate);

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
