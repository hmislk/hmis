package com.divudi.service.pharmacy;

import com.divudi.service.pharmacy.PharmacyBfdBackfillService.AuditValueSemantics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the rule that decides how a rate adjustment's stored before/after audit values
 * are to be read.
 *
 * <p>Two writers disagreed about the meaning of those two columns: the UI page stored unit
 * rates, the adjustment REST API stored extended values (qty x rate). Reading one
 * convention's rows with the other's formula does not produce a small error — on coop
 * production it would have written -262,874,342.50 into the F15 report for a bill whose
 * real value change was -242,280.50. Every figure below is a real production row, so a
 * regression here is caught with the numbers that actually exposed the bug.</p>
 *
 * <p>Issue #23411.</p>
 */
class PharmacyBfdBackfillSemanticsTest {

    private final PharmacyBfdBackfillService service = new PharmacyBfdBackfillService();

    /** billId, before, after, qty, netValue. */
    private static final double[][] UI_WRITTEN_ROWS = {
        {9932470, 47.83, 51.50, 69, 253.23},
        {9932476, 1971.00, 2365.00, 5, 1970.00},
        {9951192, 28750.00, 1150.00, 20, -552000.00},
        {9958331, 53.63, 50.61, 178, -537.56},
        {10639537, 1650.00, 1100.00, 12, -6600.00},
        {10639563, 4000.00, 3979.80, 5, -101.00},
        {11789159, 2302.34, 149.85, 29, -62422.21}
    };

    private static final double[][] API_WRITTEN_ROWS = {
        {13980412, 32503.20, 7472.00, 467, -25031.20},
        {13980413, 273745.50, 31465.00, 1085, -242280.50},
        {13985737, 9600.00, 736.00, 32, -8864.00},
        {13989824, 61325.00, 62700.00, 55, 1375.00},
        {13989839, 17600.00, 15600.00, 800, -2000.00},
        {13989848, 60700.00, 66405.80, 1214, 5705.80},
        {13993961, 44863.00, 79866.00, 145, 35003.00},
        {13993969, 44802.00, 51510.90, 570, 6708.90},
        {13993971, 347500.00, 79925.00, 5, -267575.00}
    };

    @Test
    @DisplayName("Bills written by the UI page are read as unit rates")
    void readsUiWrittenRowsAsRates() {
        for (double[] row : UI_WRITTEN_ROWS) {
            assertEquals(AuditValueSemantics.RATE,
                    service.resolveRateSemantics(row[1], row[2], row[3], row[4]),
                    "bill " + (long) row[0] + " was written by the UI page and stores unit rates");
        }
    }

    @Test
    @DisplayName("Bills written by the adjustment API are read as extended values")
    void readsApiWrittenRowsAsValues() {
        for (double[] row : API_WRITTEN_ROWS) {
            assertEquals(AuditValueSemantics.VALUE,
                    service.resolveRateSemantics(row[1], row[2], row[3], row[4]),
                    "bill " + (long) row[0] + " was written by the adjustment API and stores extended values");
        }
    }

    @Test
    @DisplayName("The old formula would have overstated bill 13980413 by 1085x")
    void demonstratesTheDefectTheResolverPrevents() {
        double before = 273745.50;
        double after = 31465.00;
        double qty = 1085;
        double recordedChange = -242280.50;

        // What the previous RATE-only backfill computed for this API-written bill.
        double oldFormula = (after - before) * qty;
        assertEquals(-262874342.50, oldFormula, 0.01,
                "the unconditional rate formula produces this figure on API-written rows");

        // The resolver refuses that reading, so the value written is the recorded change.
        assertEquals(AuditValueSemantics.VALUE,
                service.resolveRateSemantics(before, after, qty, recordedChange));
    }

    @Test
    @DisplayName("A single-unit line resolves without ambiguity because both readings agree")
    void singleUnitLineIsNotAmbiguous() {
        // coop bill 13989835: 1 unit, 1495.00 -> 1380.00, netValue -115.00.
        // (after - before) * 1 and (after - before) are the same number, so whichever
        // reading is named, the value written is identical.
        assertEquals(AuditValueSemantics.RATE,
                service.resolveRateSemantics(1495.00, 1380.00, 1, -115.00));
    }

    @Test
    @DisplayName("Bills from the legacy single-item page are refused, not guessed at")
    void legacySingleItemPageRowsAreUnresolved() {
        // That page never set billItem.qty and stored the new extended total in netValue
        // rather than a delta: rate 100 -> 120 on 50 units, qty 0, netValue 6000.
        // Reading as rates gives 0 and as values gives 20 — neither is 6000, so the bill
        // must be reported rather than valued.
        assertEquals(AuditValueSemantics.UNRESOLVED,
                service.resolveRateSemantics(100.00, 120.00, 0, 6000.00));
    }

    @Test
    @DisplayName("A reading is not accepted when it disagrees with the recorded change")
    void mismatchedRowIsUnresolved() {
        // Rates 10 -> 12 over 100 units implies 200, but netValue says 500. Something
        // else edited this bill; valuing it either way would be a guess.
        assertEquals(AuditValueSemantics.UNRESOLVED,
                service.resolveRateSemantics(10.00, 12.00, 100, 500.00));
    }

    @Test
    @DisplayName("Float-rounded rate changes still reconcile")
    void toleratesFloatRoundingInStoredRates() {
        // pbi.freeQty is a float, so rates that round-trip through it lose precision.
        // coop bill 13989842: 5.70 -> 6.90 on 200 units; the float path yields
        // 1.2000000476837158 per unit, i.e. 240.00000953674316 against a netValue of 240.
        assertEquals(AuditValueSemantics.RATE,
                service.resolveRateSemantics(5.70, 6.90, 200, 240.00000953674316));
    }

    @Test
    @DisplayName("A zero-value adjustment is still readable")
    void zeroChangeResolves() {
        assertEquals(AuditValueSemantics.RATE,
                service.resolveRateSemantics(50.00, 50.00, 10, 0.00));
    }
}
