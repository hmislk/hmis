package com.divudi.core.data;

import com.divudi.core.data.dto.HospitalDoctorFeeReportDTO;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillCategory;

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

    // Enhanced nested grouping structure
    private Map<PaymentMethod, Map<BillCategory, List<HospitalDoctorFeeReportDTO>>> nestedPaymentMethodGroups = new HashMap<>();
    private Map<PaymentMethod, Map<BillCategory, Double>> nestedSubtotals = new HashMap<>();
    private Map<PaymentMethod, Map<BillCategory, Double>> nestedHospitalFeeSubtotals = new HashMap<>();
    private Map<PaymentMethod, Map<BillCategory, Double>> nestedDoctorFeeSubtotals = new HashMap<>();

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

        // Create nested grouping by bill category within each payment method
        for (Map.Entry<PaymentMethod, List<HospitalDoctorFeeReportDTO>> pmEntry : paymentMethodGroups.entrySet()) {
            PaymentMethod paymentMethod = pmEntry.getKey();
            List<HospitalDoctorFeeReportDTO> groupDtos = pmEntry.getValue();

            Map<BillCategory, List<HospitalDoctorFeeReportDTO>> categoryMap = new HashMap<>();

            // Group DTOs by bill category within this payment method
            for (HospitalDoctorFeeReportDTO dto : groupDtos) {
                BillCategory category = getBillCategoryFromBillTypeAtomic(dto.getBillTypeAtomic());
                categoryMap.computeIfAbsent(category, k -> new ArrayList<>()).add(dto);
            }

            nestedPaymentMethodGroups.put(paymentMethod, categoryMap);

            // Calculate category subtotals for this payment method
            calculateCategorySubtotals(paymentMethod, categoryMap);
        }

        // Calculate payment method subtotals for each payment method (existing logic)
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

    /**
     * Maps BillTypeAtomic to BillCategory for grouping purposes
     */
    private BillCategory getBillCategoryFromBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        if (billTypeAtomic == null) {
            return BillCategory.BILL; // Default fallback
        }

        switch (billTypeAtomic) {
            case OPD_BILL_WITH_PAYMENT:
            case OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER:
            case INWARD_SERVICE_BILL:
                return BillCategory.BILL;
            case OPD_BILL_CANCELLATION:
            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                return BillCategory.CANCELLATION;
            case OPD_BILL_REFUND:
            case INWARD_SERVICE_BILL_REFUND:
                return BillCategory.REFUND;
            default:
                return BillCategory.BILL; // Default to BILL for unknown types
        }
    }

    /**
     * Calculate subtotals for each bill category within a payment method
     */
    private void calculateCategorySubtotals(PaymentMethod paymentMethod,
                                            Map<BillCategory, List<HospitalDoctorFeeReportDTO>> categoryMap) {
        Map<BillCategory, Double> netSubtotals = new HashMap<>();
        Map<BillCategory, Double> hospitalFeeSubtotals = new HashMap<>();
        Map<BillCategory, Double> doctorFeeSubtotals = new HashMap<>();

        for (Map.Entry<BillCategory, List<HospitalDoctorFeeReportDTO>> categoryEntry : categoryMap.entrySet()) {
            BillCategory category = categoryEntry.getKey();
            List<HospitalDoctorFeeReportDTO> categoryDtos = categoryEntry.getValue();

            double categoryNetTotal = 0.0;
            double categoryHospitalFeeTotal = 0.0;
            double categoryDoctorFeeTotal = 0.0;

            for (HospitalDoctorFeeReportDTO dto : categoryDtos) {
                double multiplier = getMultiplier(dto.getBillTypeAtomic());

                categoryNetTotal += (dto.getNetTotal() != null ? dto.getNetTotal() : 0.0) * multiplier;
                categoryHospitalFeeTotal += (dto.getHospitalFee() != null ? dto.getHospitalFee() : 0.0) * multiplier;
                categoryDoctorFeeTotal += (dto.getDoctorFee() != null ? dto.getDoctorFee() : 0.0) * multiplier;
            }

            netSubtotals.put(category, categoryNetTotal);
            hospitalFeeSubtotals.put(category, categoryHospitalFeeTotal);
            doctorFeeSubtotals.put(category, categoryDoctorFeeTotal);
        }

        nestedSubtotals.put(paymentMethod, netSubtotals);
        nestedHospitalFeeSubtotals.put(paymentMethod, hospitalFeeSubtotals);
        nestedDoctorFeeSubtotals.put(paymentMethod, doctorFeeSubtotals);
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

    // ============ ENHANCED NESTED GROUPING ACCESS METHODS ============

    /**
     * Get the nested grouping structure: PaymentMethod -> BillCategory -> List<DTO>
     */
    public Map<PaymentMethod, Map<BillCategory, List<HospitalDoctorFeeReportDTO>>> getNestedPaymentMethodGroups() {
        return nestedPaymentMethodGroups;
    }

    public void setNestedPaymentMethodGroups(Map<PaymentMethod, Map<BillCategory, List<HospitalDoctorFeeReportDTO>>> nestedPaymentMethodGroups) {
        this.nestedPaymentMethodGroups = nestedPaymentMethodGroups;
    }

    /**
     * Get bill category subtotal for a specific payment method and category
     * @param paymentMethod The payment method
     * @param category The bill category
     * @param feeType "NET", "HOSPITAL", or "DOCTOR"
     */
    public Double getBillCategorySubtotal(PaymentMethod paymentMethod, BillCategory category, String feeType) {
        if (paymentMethod == null || category == null || feeType == null) {
            return 0.0;
        }

        switch (feeType.toUpperCase()) {
            case "NET":
                return nestedSubtotals.getOrDefault(paymentMethod, new HashMap<>()).getOrDefault(category, 0.0);
            case "HOSPITAL":
                return nestedHospitalFeeSubtotals.getOrDefault(paymentMethod, new HashMap<>()).getOrDefault(category, 0.0);
            case "DOCTOR":
                return nestedDoctorFeeSubtotals.getOrDefault(paymentMethod, new HashMap<>()).getOrDefault(category, 0.0);
            default:
                return 0.0;
        }
    }

    /**
     * Get all bill categories available for a specific payment method
     */
    public Set<BillCategory> getAvailableBillCategories(PaymentMethod paymentMethod) {
        return nestedPaymentMethodGroups.getOrDefault(paymentMethod, new HashMap<>()).keySet();
    }

    /**
     * Get category totals for a specific payment method
     */
    public Map<BillCategory, Double> getCategoryTotalsForPaymentMethod(PaymentMethod paymentMethod) {
        return nestedSubtotals.getOrDefault(paymentMethod, new HashMap<>());
    }

    /**
     * Get nested subtotals map
     */
    public Map<PaymentMethod, Map<BillCategory, Double>> getNestedSubtotals() {
        return nestedSubtotals;
    }

    public void setNestedSubtotals(Map<PaymentMethod, Map<BillCategory, Double>> nestedSubtotals) {
        this.nestedSubtotals = nestedSubtotals;
    }

    /**
     * Get nested hospital fee subtotals map
     */
    public Map<PaymentMethod, Map<BillCategory, Double>> getNestedHospitalFeeSubtotals() {
        return nestedHospitalFeeSubtotals;
    }

    public void setNestedHospitalFeeSubtotals(Map<PaymentMethod, Map<BillCategory, Double>> nestedHospitalFeeSubtotals) {
        this.nestedHospitalFeeSubtotals = nestedHospitalFeeSubtotals;
    }

    /**
     * Get nested doctor fee subtotals map
     */
    public Map<PaymentMethod, Map<BillCategory, Double>> getNestedDoctorFeeSubtotals() {
        return nestedDoctorFeeSubtotals;
    }

    public void setNestedDoctorFeeSubtotals(Map<PaymentMethod, Map<BillCategory, Double>> nestedDoctorFeeSubtotals) {
        this.nestedDoctorFeeSubtotals = nestedDoctorFeeSubtotals;
    }
}