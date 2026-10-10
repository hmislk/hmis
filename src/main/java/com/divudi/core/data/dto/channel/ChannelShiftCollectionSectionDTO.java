package com.divudi.core.data.dto.channel;

import java.util.ArrayList;
import java.util.List;

/**
 * One payment-type section (Cash, Credit Card, Agent or Other) of the Cashier
 * Shift End Collection Summary (issue #24248). Payments are listed first,
 * then cancellations and refunds (negative amounts), each with its own
 * subtotal; the net total is their sum.
 */
public class ChannelShiftCollectionSectionDTO {

    private final String title;
    private final List<ChannelShiftCollectionRowDTO> paymentRows = new ArrayList<>();
    private final List<ChannelShiftCollectionRowDTO> reversalRows = new ArrayList<>();

    private double collectionDoctorFee;
    private double collectionHospitalFee;
    private double collectionTotal;
    private double reversalDoctorFee;
    private double reversalHospitalFee;
    private double reversalTotal;

    public ChannelShiftCollectionSectionDTO(String title) {
        this.title = title;
    }

    public void addRow(ChannelShiftCollectionRowDTO row) {
        if (row == null) {
            return;
        }
        if (row.isReversal()) {
            reversalRows.add(row);
            reversalDoctorFee += row.getDoctorFee();
            reversalHospitalFee += row.getHospitalFee();
            reversalTotal += row.getTotalAmount();
        } else {
            paymentRows.add(row);
            collectionDoctorFee += row.getDoctorFee();
            collectionHospitalFee += row.getHospitalFee();
            collectionTotal += row.getTotalAmount();
        }
    }

    /**
     * Payments followed by cancellations/refunds, for display and export.
     */
    public List<ChannelShiftCollectionRowDTO> getRows() {
        List<ChannelShiftCollectionRowDTO> rows = new ArrayList<>(paymentRows);
        rows.addAll(reversalRows);
        return rows;
    }

    public boolean isEmpty() {
        return paymentRows.isEmpty() && reversalRows.isEmpty();
    }

    /**
     * For EL: "empty" is a reserved EL keyword, so views must use
     * #{section.hasTransactions} rather than #{section.empty}.
     */
    public boolean isHasTransactions() {
        return !isEmpty();
    }

    public String getTitle() {
        return title;
    }

    public List<ChannelShiftCollectionRowDTO> getPaymentRows() {
        return paymentRows;
    }

    public List<ChannelShiftCollectionRowDTO> getReversalRows() {
        return reversalRows;
    }

    public double getCollectionDoctorFee() {
        return collectionDoctorFee;
    }

    public double getCollectionHospitalFee() {
        return collectionHospitalFee;
    }

    public double getCollectionTotal() {
        return collectionTotal;
    }

    public double getReversalDoctorFee() {
        return reversalDoctorFee;
    }

    public double getReversalHospitalFee() {
        return reversalHospitalFee;
    }

    public double getReversalTotal() {
        return reversalTotal;
    }

    public double getNetDoctorFee() {
        return collectionDoctorFee + reversalDoctorFee;
    }

    public double getNetHospitalFee() {
        return collectionHospitalFee + reversalHospitalFee;
    }

    public double getNetTotal() {
        return collectionTotal + reversalTotal;
    }
}
