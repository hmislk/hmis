package com.divudi.core.data.dto.channel;

import com.divudi.core.data.PaymentMethod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Cashier Shift End Collection Summary segregated into Cash, Credit Card,
 * Agent and Other sections (issue #24248).
 *
 * Each bill is placed by its own payment method, so a cancellation or refund
 * lands in the section the money actually moved through (e.g. an agent booking
 * cancelled with a cash refund reduces the Cash section). Bills paid with
 * multiple payment methods are split into one row per method, with doctor and
 * hospital fees prorated so the split rows add up exactly to the bill.
 *
 * Grand Total = sum of every section's net total
 *             = total collections + cancel/refund total (negative).
 */
public class ChannelShiftCollectionReportDTO {

    private final ChannelShiftCollectionSectionDTO cashSection = new ChannelShiftCollectionSectionDTO("Cash Transactions");
    private final ChannelShiftCollectionSectionDTO cardSection = new ChannelShiftCollectionSectionDTO("Credit Card Transactions");
    private final ChannelShiftCollectionSectionDTO agentSection = new ChannelShiftCollectionSectionDTO("Agent Transactions");
    private final ChannelShiftCollectionSectionDTO otherSection = new ChannelShiftCollectionSectionDTO("Other Payment Method Transactions");

    private final Set<Long> validAppointmentBillIds = new HashSet<>();
    private final Set<Long> cancelRefundBillIds = new HashSet<>();
    private final TreeMap<Date, DateSummary> dateSummaries = new TreeMap<>();

    /**
     * Adds one bill to the report.
     *
     * @param row the bill row from the shift query
     * @param multiplePaymentPortions for a MultiplePaymentMethods bill, the
     * paid value per payment method (from its Payment records); ignored for
     * other bills. When missing for a multiple payment bill, the whole bill
     * goes to the Other section so it is never dropped.
     */
    public void addRow(ChannelShiftCollectionRowDTO row, Map<PaymentMethod, Double> multiplePaymentPortions) {
        if (row == null) {
            return;
        }
        if (row.isReversal()) {
            cancelRefundBillIds.add(row.getBillId());
        } else if (!row.isCancelled() && !row.isRefunded()) {
            validAppointmentBillIds.add(row.getBillId());
        }

        if (row.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods
                && multiplePaymentPortions != null && !multiplePaymentPortions.isEmpty()) {
            for (ChannelShiftCollectionRowDTO portion : splitByPortions(row, multiplePaymentPortions)) {
                place(portion);
            }
        } else {
            place(row);
        }
    }

    /**
     * Splits a multiple payment bill into one row per payment method. Fees
     * are prorated by each method's share of the bill; the last portion takes
     * the remainder so the portions sum exactly to the bill's values.
     */
    static List<ChannelShiftCollectionRowDTO> splitByPortions(ChannelShiftCollectionRowDTO row, Map<PaymentMethod, Double> portions) {
        List<ChannelShiftCollectionRowDTO> result = new ArrayList<>();
        double billTotal = row.getTotalAmount();
        double sign = billTotal < 0 ? -1.0 : 1.0;
        double absTotal = Math.abs(billTotal);

        double usedDoc = 0;
        double usedHos = 0;
        double usedTotal = 0;
        int index = 0;
        int last = portions.size() - 1;
        for (Map.Entry<PaymentMethod, Double> e : portions.entrySet()) {
            double paid = e.getValue() != null ? Math.abs(e.getValue()) : 0.0;
            double doc;
            double hos;
            double total;
            if (index == last) {
                doc = round2(row.getDoctorFee() - usedDoc);
                hos = round2(row.getHospitalFee() - usedHos);
                total = round2(billTotal - usedTotal);
            } else {
                double share = absTotal == 0 ? 0 : paid / absTotal;
                doc = round2(row.getDoctorFee() * share);
                hos = round2(row.getHospitalFee() * share);
                total = round2(sign * paid);
            }
            usedDoc += doc;
            usedHos += hos;
            usedTotal += total;
            result.add(row.portion(e.getKey(), doc, hos, total));
            index++;
        }
        return result;
    }

    private void place(ChannelShiftCollectionRowDTO row) {
        PaymentMethod pm = row.getPaymentMethod();
        if (pm == PaymentMethod.Cash) {
            cashSection.addRow(row);
        } else if (pm == PaymentMethod.Card) {
            cardSection.addRow(row);
        } else if (pm == PaymentMethod.Agent) {
            agentSection.addRow(row);
        } else {
            otherSection.addRow(row);
        }
        addToDateSummary(row);
    }

    private void addToDateSummary(ChannelShiftCollectionRowDTO row) {
        Date key = row.getAppointmentDate() != null ? row.getAppointmentDate() : new Date(0);
        DateSummary s = dateSummaries.computeIfAbsent(key, k -> new DateSummary(row.getAppointmentDate()));
        PaymentMethod pm = row.getPaymentMethod();
        if (pm == PaymentMethod.Cash) {
            s.cashTotal += row.getTotalAmount();
        } else if (pm == PaymentMethod.Card) {
            s.cardTotal += row.getTotalAmount();
        } else if (pm == PaymentMethod.Agent) {
            s.agentTotal += row.getTotalAmount();
        } else {
            s.otherTotal += row.getTotalAmount();
        }
        s.doctorFee += row.getDoctorFee();
        s.hospitalFee += row.getHospitalFee();
        s.totalAmount += row.getTotalAmount();
        if (!row.isReversal() && !row.isCancelled() && !row.isRefunded()) {
            s.validBillIds.add(row.getBillId());
        }
    }

    static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public ChannelShiftCollectionSectionDTO getCashSection() {
        return cashSection;
    }

    public ChannelShiftCollectionSectionDTO getCardSection() {
        return cardSection;
    }

    public ChannelShiftCollectionSectionDTO getAgentSection() {
        return agentSection;
    }

    public ChannelShiftCollectionSectionDTO getOtherSection() {
        return otherSection;
    }

    /**
     * True when any section has at least one row.
     */
    public boolean isHasTransactions() {
        return cashSection.isHasTransactions() || cardSection.isHasTransactions()
                || agentSection.isHasTransactions() || otherSection.isHasTransactions();
    }

    public List<DateSummary> getDateSummaries() {
        return new ArrayList<>(dateSummaries.values());
    }

    public long getValidAppointmentCount() {
        return validAppointmentBillIds.size();
    }

    public long getCancelRefundAppointmentCount() {
        return cancelRefundBillIds.size();
    }

    public double getTotalCashCollection() {
        return cashSection.getCollectionTotal();
    }

    public double getTotalCardCollection() {
        return cardSection.getCollectionTotal();
    }

    public double getTotalAgentCollection() {
        return agentSection.getCollectionTotal();
    }

    public double getTotalOtherCollection() {
        return otherSection.getCollectionTotal();
    }

    /**
     * Sum of all cancellations and refunds across sections (zero or negative).
     */
    public double getCancelRefundTotal() {
        return cashSection.getReversalTotal() + cardSection.getReversalTotal()
                + agentSection.getReversalTotal() + otherSection.getReversalTotal();
    }

    public double getGrandTotal() {
        return cashSection.getNetTotal() + cardSection.getNetTotal()
                + agentSection.getNetTotal() + otherSection.getNetTotal();
    }

    public double getGrandDoctorFee() {
        return cashSection.getNetDoctorFee() + cardSection.getNetDoctorFee()
                + agentSection.getNetDoctorFee() + otherSection.getNetDoctorFee();
    }

    public double getGrandHospitalFee() {
        return cashSection.getNetHospitalFee() + cardSection.getNetHospitalFee()
                + agentSection.getNetHospitalFee() + otherSection.getNetHospitalFee();
    }

    /**
     * Net collection per appointment date, split by payment type.
     */
    public static class DateSummary {

        private final Date appointmentDate;
        private double cashTotal;
        private double cardTotal;
        private double agentTotal;
        private double otherTotal;
        private double doctorFee;
        private double hospitalFee;
        private double totalAmount;
        private final Set<Long> validBillIds = new HashSet<>();

        public DateSummary(Date appointmentDate) {
            this.appointmentDate = appointmentDate;
        }

        public Date getAppointmentDate() {
            return appointmentDate;
        }

        public double getCashTotal() {
            return cashTotal;
        }

        public double getCardTotal() {
            return cardTotal;
        }

        public double getAgentTotal() {
            return agentTotal;
        }

        public double getOtherTotal() {
            return otherTotal;
        }

        public double getDoctorFee() {
            return doctorFee;
        }

        public double getHospitalFee() {
            return hospitalFee;
        }

        public double getTotalAmount() {
            return totalAmount;
        }

        public long getValidAppointments() {
            return validBillIds.size();
        }
    }
}
