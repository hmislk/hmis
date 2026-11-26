package com.divudi.core.data.inward;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class InwardPharmacyItemListNettingTest {

    private static long nextId = 1;

    private static BillItem line(BillTypeAtomic bta, boolean cancelled, double qty, double net, BillItem ref) {
        Bill b = new Bill();
        b.setBillTypeAtomic(bta);
        b.setCancelled(cancelled);
        BillItem bi = new BillItem();
        bi.setId(nextId++);
        bi.setBill(b);
        bi.setQty(qty);
        bi.setNetValue(net);
        bi.setReferanceBillItem(ref);
        return bi;
    }

    /** The interim bill: every bill of the types, cancelled or not, no filter. */
    private static double interim(List<BillItem> all) {
        double sum = 0.0;
        for (BillItem bi : all) {
            BillTypeAtomic t = bi.getBill().getBillTypeAtomic();
            boolean deducted = InwardMedicineBillTypes.isItemListReturn(t)
                    || InwardMedicineBillTypes.ISSUE_CANCELLATIONS.contains(t);
            sum += deducted ? -Math.abs(bi.getNetValue()) : Math.abs(bi.getNetValue());
        }
        return sum;
    }

    private static List<BillItem> liveTypesOnly(List<BillItem> all) {
        List<BillItem> l = new ArrayList<>();
        for (BillItem bi : all) {
            if (InwardMedicineBillTypes.ALL_EXCLUDING_ISSUE_CANCELLATIONS.contains(bi.getBill().getBillTypeAtomic())) {
                l.add(bi);
            }
        }
        return l;
    }

    @Test
    void cancelledReturnWithTheStoredSignsNetsToTheIssue() {
        // As BhtIssueReturnController and the return cancel write them: the return line is
        // stored positive under a bill of -40, the cancellation line is the return line
        // inverted (-40) under a bill of +40. Only the bill type may decide the sign.
        BillItem issue = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, 10, 100, null);
        BillItem ret = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, true, 4, 40, issue);
        ret.getBill().setNetTotal(-40.0);
        BillItem cancel = line(BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION, false, -4, -40, issue);
        cancel.getBill().setNetTotal(40.0);
        List<BillItem> all = Arrays.asList(issue, ret, cancel);

        assertEquals(100.0, InwardPharmacyItemListNetting.total(InwardPharmacyItemListNetting.rows(liveTypesOnly(all))), 0.0001);

        // legacy list: no netting, rows read through signedRowValue, footer through listSignedValueWithCancellations
        // (the netting above changed the issue line in place, so use a fresh one)
        BillItem legacyIssue = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, 10, 100, null);
        double rows = 0.0;
        double footer = 0.0;
        for (BillItem bi : Arrays.asList(legacyIssue, ret, cancel)) {
            rows += InwardMedicineBillTypes.signedRowValue(bi.getBill().getBillTypeAtomic(), bi.getNetValue());
            footer += InwardMedicineBillTypes.listSignedValueWithCancellations(bi.getBill().getBillTypeAtomic(), bi.getNetValue());
        }
        assertEquals(100.0, rows, 0.0001);
        assertEquals(100.0, footer, 0.0001);

        // the same with a return line stored negative
        BillItem retNegative = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, true, -4, -40, issue);
        BillItem cancelPositive = line(BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION, false, 4, 40, issue);
        double rows2 = 0.0;
        for (BillItem bi : Arrays.asList(legacyIssue, retNegative, cancelPositive)) {
            rows2 += InwardMedicineBillTypes.signedRowValue(bi.getBill().getBillTypeAtomic(), bi.getNetValue());
        }
        assertEquals(100.0, rows2, 0.0001);
    }

    @Test
    void listTotalEqualsInterimForIssueRequestIssueCancelledIssuePartialReturnAndAcceptedReturn() {
        BillItem direct = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, 10, 100, null);
        BillItem directReturn = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, false, -2, -20, direct);
        BillItem request = line(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD, false, 5, 50, null);
        BillItem requestReturn = line(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN, false, 1, 10, request);
        BillItem cancelledIssue = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, true, 4, 40, null);
        BillItem cancellation = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION, false, -4, -40, cancelledIssue);
        BillItem accepted = line(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD, false, -1, -7, null);
        List<BillItem> all = Arrays.asList(direct, directReturn, request, requestReturn, cancelledIssue, cancellation, accepted);

        double interim = interim(all);
        assertEquals(100 - 20 + 50 - 10 + 40 - 40 - 7, interim, 0.0001);

        List<BillItem> rows = InwardPharmacyItemListNetting.rows(liveTypesOnly(all));
        assertEquals(interim, InwardPharmacyItemListNetting.total(rows), 0.0001);
        // direct, request, and the accepted return as its own row
        assertEquals(3, rows.size());
        assertTrue(rows.contains(accepted));
        assertFalse(rows.contains(cancelledIssue));
    }

    @Test
    void cancelledReturnStaysAndTheInterimBillStillDeductsIt() {
        BillItem issue = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, 10, 100, null);
        BillItem cancelledReturn = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, true, -3, -30, issue);
        List<BillItem> all = Arrays.asList(issue, cancelledReturn);
        double interim = interim(all);
        assertEquals(70.0, interim, 0.0001);
        assertEquals(interim, InwardPharmacyItemListNetting.total(InwardPharmacyItemListNetting.rows(all)), 0.0001);
    }

    @Test
    void returnOfACancelledIssueIsKeptAsItsOwnRow() {
        BillItem issue = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, true, 10, 100, null);
        BillItem cancelledReturn = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, true, -3, -30, issue);
        BillItem cancellation = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION, false, -10, -100, issue);
        List<BillItem> all = Arrays.asList(issue, cancelledReturn, cancellation);
        List<BillItem> rows = InwardPharmacyItemListNetting.rows(liveTypesOnly(all));
        assertEquals(1, rows.size());
        assertEquals(interim(all), InwardPharmacyItemListNetting.total(rows), 0.0001);
    }

    @Test
    void countedInLiveListKeepsCancelledReturnsOnly() {
        assertTrue(InwardMedicineBillTypes.isCountedInLiveList(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false));
        assertFalse(InwardMedicineBillTypes.isCountedInLiveList(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, true));
        assertTrue(InwardMedicineBillTypes.isCountedInLiveList(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, true));
        assertTrue(InwardMedicineBillTypes.isCountedInLiveList(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN, true));
        assertTrue(InwardMedicineBillTypes.isCountedInLiveList(BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE_RETURN, true));
    }

    @Test
    void issuesListHoldsNoReturnsOrCancellations() {
        assertTrue(InwardMedicineBillTypes.ISSUES.contains(BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD));
        assertFalse(InwardMedicineBillTypes.ISSUES.contains(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD));
        assertFalse(InwardMedicineBillTypes.ISSUES.contains(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION));
    }

    @Test
    void issueRowNettedBelowZeroKeepsItsSign() {
        // A return valued above its issue line (for example after a rate change)
        // nets the issue row below zero; the row must show that, not a positive value.
        BillItem issue = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, 2, 10, null);
        BillItem ret = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, false, -2, -15, issue);
        double interimTotal = interim(Arrays.asList(issue, ret));
        List<BillItem> rows = InwardPharmacyItemListNetting.rows(Arrays.asList(issue, ret));
        assertEquals(1, rows.size());
        assertEquals(-5.0, InwardPharmacyItemListNetting.rowValue(rows.get(0)), 0.0001);
        assertEquals(-5.0, InwardPharmacyItemListNetting.total(rows), 0.0001);
        assertEquals(interimTotal, InwardPharmacyItemListNetting.total(rows), 0.0001);
    }

    @Test
    void signedRowValueDeductsReturnsAndKeepsIssueSign() {
        assertEquals(-7.0, InwardMedicineBillTypes.signedRowValue(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, -7.0), 0.0001);
        assertEquals(7.0, InwardMedicineBillTypes.signedRowValue(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, 7.0), 0.0001);
        assertEquals(-7.0, InwardMedicineBillTypes.signedRowValue(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, 7.0), 0.0001);
        assertEquals(-7.0, InwardMedicineBillTypes.signedRowValue(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION, -7.0), 0.0001);
        assertEquals(0.0, InwardMedicineBillTypes.signedRowValue(null, null), 0.0001);
    }

    /** An original issue line, its data-admin reversal (same type, negative) and the cancelled flag set. */
    private static BillItem[] reversedPair(double net) {
        BillItem original = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, 3, net, null);
        BillItem reversal = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, -3, -net, null);
        original.getBill().setId(nextId++);
        reversal.getBill().setId(nextId++);
        reversal.getBill().setReferenceBill(original.getBill());
        original.getBill().setCancelled(true);
        original.getBill().setCancelledBill(reversal.getBill());
        return new BillItem[]{original, reversal};
    }

    @Test
    void reversalBillIsRecognisedAndTheOriginalIsNot() {
        BillItem[] pair = reversedPair(40);
        assertTrue(InwardMedicineBillTypes.isReversalBill(pair[1].getBill()));
        assertFalse(InwardMedicineBillTypes.isReversalBill(pair[0].getBill()));
        assertFalse(InwardMedicineBillTypes.isReversalBill(null));
    }

    @Test
    void retiredReversalIsNotAReversal() {
        BillItem[] pair = reversedPair(40);
        pair[1].getBill().setRetired(true);
        assertFalse(InwardMedicineBillTypes.isReversalBill(pair[1].getBill()));
    }

    @Test
    void cancellationBillOfAnotherTypeIsNotAReversal() {
        BillItem[] pair = reversedPair(40);
        pair[1].getBill().setBillTypeAtomic(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
        assertFalse(InwardMedicineBillTypes.isReversalBill(pair[1].getBill()));
    }

    @Test
    void reversedIssueNetsToZeroInTheNettingLikeTheInterimBill() {
        BillItem[] pair = reversedPair(40);
        BillItem other = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, 1, 25, null);
        List<BillItem> all = Arrays.asList(pair[0], pair[1], other);
        List<BillItem> rows = InwardPharmacyItemListNetting.rows(all);
        assertEquals(1, rows.size());
        assertEquals(25.0, InwardPharmacyItemListNetting.total(rows), 0.0001);
        // The interim bill sums every bill and ignores the cancelled flag: 40 - 40 + 25.
        double interimSigned = 0.0;
        for (BillItem bi : all) {
            interimSigned += InwardMedicineBillTypes.signedRowValue(bi.getBill().getBillTypeAtomic(), bi.getNetValue());
        }
        assertEquals(interimSigned, InwardPharmacyItemListNetting.total(rows), 0.0001);
    }

    @Test
    void legacyFooterEqualsTheRowsWhenAReversalIsStoredNegative() {
        BillItem[] pair = reversedPair(40);
        BillItem returned = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, false, -1, -5, null);
        double footer = 0.0;
        double rowsSum = 0.0;
        for (BillItem bi : Arrays.asList(pair[0], pair[1], returned)) {
            footer += InwardMedicineBillTypes.signedRowValue(bi.getBill().getBillTypeAtomic(), bi.getNetValue());
            rowsSum += InwardPharmacyItemListNetting.rowValue(bi);
        }
        assertEquals(-5.0, footer, 0.0001);
        assertEquals(rowsSum, footer, 0.0001);
        assertEquals(0.0, InwardMedicineBillTypes.signedRowValue(pair[0].getBill().getBillTypeAtomic(), pair[0].getNetValue())
                + InwardMedicineBillTypes.signedRowValue(pair[1].getBill().getBillTypeAtomic(), pair[1].getNetValue()), 0.0001);
    }

    @Test
    void liveListCountsRule() {
        assertFalse(InwardMedicineBillTypes.isCountedInLiveList(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, true));
        assertTrue(InwardMedicineBillTypes.isCountedInLiveList(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, false));
        assertFalse(InwardMedicineBillTypes.isCountedInLiveList(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, true, false));
    }

    @Test
    void reversalAndCancelledReturnInTheSameListNetToTheInterimBill() {
        // O1 + its reversal R1 net to 0; O2 stays charged in full: its return was cancelled,
        // and the cancellation bill (the return's value inverted) offsets the return.
        BillItem[] pair = reversedPair(40);
        BillItem o2 = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, 10, 100, null);
        BillItem cancelledReturn = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, true, -4, -40, o2);
        BillItem offset = line(BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION, false, 4, 40, cancelledReturn);
        List<BillItem> all = Arrays.asList(pair[0], pair[1], o2, cancelledReturn, offset);

        // The interim bill is summed first: rows() changes the issue lines in place.
        double interimSigned = 0.0;
        for (BillItem bi : all) {
            interimSigned += InwardMedicineBillTypes.signedRowValue(bi.getBill().getBillTypeAtomic(), bi.getNetValue());
        }

        List<BillItem> rows = InwardPharmacyItemListNetting.rows(liveTypesOnly(all));

        assertEquals(100.0, interimSigned, 0.0001);
        assertEquals(100.0, InwardPharmacyItemListNetting.total(rows), 0.0001);
        assertFalse(rows.contains(pair[0]));
        assertFalse(rows.contains(pair[1]));
    }

    @Test
    void reversalAndCancelledReturnNetTheSameInAnyListOrder() {
        BillItem[] pair = reversedPair(40);
        BillItem o2 = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE, false, 10, 100, null);
        BillItem cancelledReturn = line(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN, true, -4, -40, o2);
        BillItem offset = line(BillTypeAtomic.PHARMACY_RETURN_ITEMS_AND_PAYMENTS_CANCELLATION, false, 4, 40, cancelledReturn);
        List<BillItem> rows = InwardPharmacyItemListNetting.rows(liveTypesOnly(
                Arrays.asList(offset, cancelledReturn, o2, pair[1], pair[0])));
        assertEquals(100.0, InwardPharmacyItemListNetting.total(rows), 0.0001);
    }
}
