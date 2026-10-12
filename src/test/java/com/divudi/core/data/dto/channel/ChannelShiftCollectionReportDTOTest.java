package com.divudi.core.data.dto.channel;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelShiftCollectionReportDTOTest {

    private static final Date APPOINTMENT = new Date(1_700_000_000_000L);

    private static ChannelShiftCollectionRowDTO row(long billId, BillTypeAtomic bta, PaymentMethod pm,
            double doc, double hos, double total, boolean cancelled, String agent) {
        return new ChannelShiftCollectionRowDTO(billId, "B" + billId, bta, pm, APPOINTMENT, new Date(),
                "Dr A", "Patient", doc, hos, total, cancelled, false, agent, agent != null ? "AG1" : null, agent != null ? "REF" + billId : null, 1);
    }

    @Test
    void rowsArePlacedBySectionAndGrandTotalIsSumOfNets() {
        ChannelShiftCollectionReportDTO report = new ChannelShiftCollectionReportDTO();
        report.addRow(row(1, BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT, PaymentMethod.Cash, 2000, 500, 2500, false, null), null);
        report.addRow(row(2, BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT, PaymentMethod.Card, 3000, 600, 3600, false, null), null);
        report.addRow(row(3, BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT, PaymentMethod.Agent, 1500, 400, 1900, false, "Agent X"), null);
        report.addRow(row(4, BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT, PaymentMethod.Cheque, 100, 50, 150, false, null), null);

        assertEquals(2500, report.getCashSection().getNetTotal());
        assertEquals(3600, report.getCardSection().getNetTotal());
        assertEquals(1900, report.getAgentSection().getNetTotal());
        assertEquals(150, report.getOtherSection().getNetTotal());
        assertEquals(8150, report.getGrandTotal());
        assertEquals(4, report.getValidAppointmentCount());
        assertEquals(0, report.getCancelRefundAppointmentCount());
    }

    @Test
    void cancellationIsNegativeAndNettedWithinItsPaymentSection() {
        ChannelShiftCollectionReportDTO report = new ChannelShiftCollectionReportDTO();
        // original booking (now cancelled) and its cancellation, both in the shift
        report.addRow(row(1, BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT, PaymentMethod.Cash, 2000, 500, 2500, true, null), null);
        // cancellation stored with positive values must still be shown as negative
        report.addRow(row(2, BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT, PaymentMethod.Cash, 2000, 500, 2500, false, null), null);

        ChannelShiftCollectionSectionDTO cash = report.getCashSection();
        assertEquals(2500, cash.getCollectionTotal());
        assertEquals(-2500, cash.getReversalTotal());
        assertEquals(-2000, cash.getReversalDoctorFee());
        assertEquals(0, cash.getNetTotal());
        assertEquals(-2500, report.getCancelRefundTotal());
        assertEquals(0, report.getGrandTotal());
        assertEquals(0, report.getValidAppointmentCount());
        assertEquals(1, report.getCancelRefundAppointmentCount());
        assertEquals("Cancelled", cash.getRows().get(0).getStatus());
        assertEquals("Cancellation", cash.getRows().get(1).getStatus());
    }

    @Test
    void agentBookingCancelledWithCashRefundReducesCashSection() {
        ChannelShiftCollectionReportDTO report = new ChannelShiftCollectionReportDTO();
        report.addRow(row(1, BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT, PaymentMethod.Agent, 1500, 400, 1900, true, "Agent X"), null);
        report.addRow(row(2, BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT, PaymentMethod.Cash, -1500, -400, -1900, false, "Agent X"), null);

        assertEquals(1900, report.getAgentSection().getNetTotal());
        assertEquals(-1900, report.getCashSection().getNetTotal());
        assertEquals(0, report.getGrandTotal());
    }

    @Test
    void multiplePaymentBillIsSplitExactlyAcrossSections() {
        ChannelShiftCollectionReportDTO report = new ChannelShiftCollectionReportDTO();
        Map<PaymentMethod, Double> portions = new LinkedHashMap<>();
        portions.put(PaymentMethod.Cash, 1000.0);
        portions.put(PaymentMethod.Card, 2000.0);
        report.addRow(row(1, BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT, PaymentMethod.MultiplePaymentMethods, 2000.01, 999.99, 3000, false, null), portions);

        ChannelShiftCollectionSectionDTO cash = report.getCashSection();
        ChannelShiftCollectionSectionDTO card = report.getCardSection();
        assertEquals(1000, cash.getNetTotal());
        assertEquals(2000, card.getNetTotal());
        assertEquals(2000.01, cash.getNetDoctorFee() + card.getNetDoctorFee(), 0.0001);
        assertEquals(999.99, cash.getNetHospitalFee() + card.getNetHospitalFee(), 0.0001);
        assertEquals(3000, report.getGrandTotal());
        assertEquals(1, report.getValidAppointmentCount());
        assertTrue(report.getOtherSection().isEmpty());
    }

    @Test
    void multiplePaymentBillWithoutPaymentRecordsGoesToOtherSection() {
        ChannelShiftCollectionReportDTO report = new ChannelShiftCollectionReportDTO();
        report.addRow(row(1, BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT, PaymentMethod.MultiplePaymentMethods, 100, 50, 150, false, null), null);

        assertEquals(150, report.getOtherSection().getNetTotal());
        assertEquals(150, report.getGrandTotal());
    }

    @Test
    void splitOfNegativeMultiplePaymentCancellationKeepsSign() {
        ChannelShiftCollectionRowDTO cancel = row(5, BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT, PaymentMethod.MultiplePaymentMethods, -2000, -1000, -3000, false, null);
        Map<PaymentMethod, Double> portions = new LinkedHashMap<>();
        portions.put(PaymentMethod.Cash, -1000.0);
        portions.put(PaymentMethod.Card, -2000.0);

        List<ChannelShiftCollectionRowDTO> parts = ChannelShiftCollectionReportDTO.splitByPortions(cancel, portions);

        assertEquals(2, parts.size());
        assertEquals(-1000, parts.get(0).getTotalAmount());
        assertEquals(-2000, parts.get(1).getTotalAmount());
        assertEquals(-3000, parts.get(0).getDoctorFee() + parts.get(1).getDoctorFee() + parts.get(0).getHospitalFee() + parts.get(1).getHospitalFee(), 0.0001);
    }

    @Test
    void emptyReportHasZeroTotals() {
        ChannelShiftCollectionReportDTO report = new ChannelShiftCollectionReportDTO();

        assertTrue(report.getCashSection().isEmpty());
        assertEquals(0, report.getGrandTotal());
        assertEquals(0, report.getCancelRefundTotal());
        assertEquals(0, report.getValidAppointmentCount());
    }
}
