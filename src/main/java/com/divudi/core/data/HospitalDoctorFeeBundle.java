package com.divudi.core.data;

import com.divudi.core.data.dto.HospitalDoctorFeeReportDTO;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.BillTypeAtomic;

import java.io.Serializable;
import java.util.*;

/**
 * Bundle class for Hospital Doctor Fee reports.
 * Provides payment method grouping and calculation logic.
 */
public class HospitalDoctorFeeBundle implements Serializable {
    private Map<PaymentMethod, List<HospitalDoctorFeeReportDTO>> paymentMethodGroups = new HashMap<>();
    private Map<PaymentMethod, Double> paymentMethodSubtotals = new HashMap<>();
    private Map<PaymentMethod, Double> paymentMethodHospitalFeeSubtotals = new HashMap<>();
    private Map<PaymentMethod, Double> paymentMethodDoctorFeeSubtotals = new HashMap<>();
    private Double grandTotal = 0.0;
    private Double grandHospitalFeeTotal = 0.0;
    private Double grandDoctorFeeTotal = 0.0;
    private Long totalCount = 0L;

    public HospitalDoctorFeeBundle() {
    }

    public HospitalDoctorFeeBundle(List<HospitalDoctorFeeReportDTO> dtos) {
        if (dtos != null) {
            processData(dtos);
        }
    }

    private void processData(List<HospitalDoctorFeeReportDTO> dtos) {
        // Group by payment method
        for (HospitalDoctorFeeReportDTO dto : dtos) {
            PaymentMethod paymentMethod = dto.getPaymentMethod();
            if (paymentMethod == null) {
                paymentMethod = PaymentMethod.Cash; // Default fallback
            }

            paymentMethodGroups.computeIfAbsent(paymentMethod, k -> new ArrayList<>()).add(dto);
        }

        // Calculate subtotals for each payment method
        for (Map.Entry<PaymentMethod, List<HospitalDoctorFeeReportDTO>> entry : paymentMethodGroups.entrySet()) {
            PaymentMethod paymentMethod = entry.getKey();
            List<HospitalDoctorFeeReportDTO> groupDtos = entry.getValue();

            double netTotal = 0.0;
            double hospitalFeeTotal = 0.0;
            double doctorFeeTotal = 0.0;

            for (HospitalDoctorFeeReportDTO dto : groupDtos) {
                double multiplier = getMultiplier(dto.getBillTypeAtomic());

                netTotal += (dto.getNetTotal() != null ? dto.getNetTotal() : 0.0) * multiplier;
                hospitalFeeTotal += (dto.getHospitalFee() != null ? dto.getHospitalFee() : 0.0) * multiplier;
                doctorFeeTotal += (dto.getDoctorFee() != null ? dto.getDoctorFee() : 0.0) * multiplier;
            }

            paymentMethodSubtotals.put(paymentMethod, netTotal);
            paymentMethodHospitalFeeSubtotals.put(paymentMethod, hospitalFeeTotal);
            paymentMethodDoctorFeeSubtotals.put(paymentMethod, doctorFeeTotal);

            // Add to grand totals
            grandTotal += netTotal;
            grandHospitalFeeTotal += hospitalFeeTotal;
            grandDoctorFeeTotal += doctorFeeTotal;
        }

        totalCount = (long) dtos.size();
    }

    /**
     * Returns multiplier based on bill type for net calculation
     * Bills = +1, Cancellations = -1, Returns = -1
     */
    private double getMultiplier(BillTypeAtomic billTypeAtomic) {
        if (billTypeAtomic == null) {
            return 1.0;
        }

        switch (billTypeAtomic) {
            case OPD_BILL_WITH_PAYMENT:
            case OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER:
                return 1.0; // Bills - positive
            case OPD_BILL_CANCELLATION:
            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
            case OPD_BILL_REFUND:
            case INWARD_SERVICE_BILL_REFUND:
                return -1.0; // Cancellations and Returns - negative
            default:
                return 1.0; // Default to positive
        }
    }

    public Map<PaymentMethod, List<HospitalDoctorFeeReportDTO>> getPaymentMethodGroups() {
        return paymentMethodGroups;
    }

    public void setPaymentMethodGroups(Map<PaymentMethod, List<HospitalDoctorFeeReportDTO>> paymentMethodGroups) {
        this.paymentMethodGroups = paymentMethodGroups;
    }

    public Map<PaymentMethod, Double> getPaymentMethodSubtotals() {
        return paymentMethodSubtotals;
    }

    public void setPaymentMethodSubtotals(Map<PaymentMethod, Double> paymentMethodSubtotals) {
        this.paymentMethodSubtotals = paymentMethodSubtotals;
    }

    public Map<PaymentMethod, Double> getPaymentMethodHospitalFeeSubtotals() {
        return paymentMethodHospitalFeeSubtotals;
    }

    public void setPaymentMethodHospitalFeeSubtotals(Map<PaymentMethod, Double> paymentMethodHospitalFeeSubtotals) {
        this.paymentMethodHospitalFeeSubtotals = paymentMethodHospitalFeeSubtotals;
    }

    public Map<PaymentMethod, Double> getPaymentMethodDoctorFeeSubtotals() {
        return paymentMethodDoctorFeeSubtotals;
    }

    public void setPaymentMethodDoctorFeeSubtotals(Map<PaymentMethod, Double> paymentMethodDoctorFeeSubtotals) {
        this.paymentMethodDoctorFeeSubtotals = paymentMethodDoctorFeeSubtotals;
    }

    public Double getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(Double grandTotal) {
        this.grandTotal = grandTotal;
    }

    public Double getGrandHospitalFeeTotal() {
        return grandHospitalFeeTotal;
    }

    public void setGrandHospitalFeeTotal(Double grandHospitalFeeTotal) {
        this.grandHospitalFeeTotal = grandHospitalFeeTotal;
    }

    public Double getGrandDoctorFeeTotal() {
        return grandDoctorFeeTotal;
    }

    public void setGrandDoctorFeeTotal(Double grandDoctorFeeTotal) {
        this.grandDoctorFeeTotal = grandDoctorFeeTotal;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * Get subtotal for a specific payment method
     */
    public Double getSubtotalForPaymentMethod(PaymentMethod paymentMethod) {
        return paymentMethodSubtotals.getOrDefault(paymentMethod, 0.0);
    }

    /**
     * Get hospital fee subtotal for a specific payment method
     */
    public Double getHospitalFeeSubtotalForPaymentMethod(PaymentMethod paymentMethod) {
        return paymentMethodHospitalFeeSubtotals.getOrDefault(paymentMethod, 0.0);
    }

    /**
     * Get doctor fee subtotal for a specific payment method
     */
    public Double getDoctorFeeSubtotalForPaymentMethod(PaymentMethod paymentMethod) {
        return paymentMethodDoctorFeeSubtotals.getOrDefault(paymentMethod, 0.0);
    }

    /**
     * Get all payment methods that have data
     */
    public Set<PaymentMethod> getPaymentMethods() {
        return paymentMethodGroups.keySet();
    }
}