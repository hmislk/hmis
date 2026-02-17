package com.divudi.core.data;

import com.divudi.core.data.dto.BillItemReportDTO;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillCategory;

import java.io.Serializable;
import java.util.*;

/**
 * Bundle class for Bill Item reports.
 * Provides payment method and bill category grouping with calculation logic.
 */
public class BillItemReportBundle implements Serializable {
    private Map<PaymentMethod, List<BillItemReportDTO>> paymentMethodGroups = new HashMap<>();
    private Map<PaymentMethod, Double> paymentMethodSubtotals = new HashMap<>();
    private Map<PaymentMethod, Double> paymentMethodHospitalFeeSubtotals = new HashMap<>();
    private Map<PaymentMethod, Double> paymentMethodStaffFeeSubtotals = new HashMap<>();

    // Enhanced nested grouping structure
    private Map<PaymentMethod, Map<BillCategory, List<BillItemReportDTO>>> nestedPaymentMethodGroups = new HashMap<>();
    private Map<PaymentMethod, Map<BillCategory, Double>> nestedSubtotals = new HashMap<>();
    private Map<PaymentMethod, Map<BillCategory, Double>> nestedHospitalFeeSubtotals = new HashMap<>();
    private Map<PaymentMethod, Map<BillCategory, Double>> nestedStaffFeeSubtotals = new HashMap<>();

    private Double grandTotal = 0.0;
    private Double grandHospitalFeeTotal = 0.0;
    private Double grandStaffFeeTotal = 0.0;
    private Long totalCount = 0L;

    public BillItemReportBundle() {
    }

    public BillItemReportBundle(List<BillItemReportDTO> dtos) {
        if (dtos != null) {
            processData(dtos);
        }
    }

    private void processData(List<BillItemReportDTO> dtos) {
        // Group by payment method
        for (BillItemReportDTO dto : dtos) {
            PaymentMethod paymentMethod = dto.getPaymentMethod();
            if (paymentMethod == null) {
                paymentMethod = PaymentMethod.Cash; // Default fallback
            }

            paymentMethodGroups.computeIfAbsent(paymentMethod, k -> new ArrayList<>()).add(dto);
        }

        // Create nested grouping by bill category within each payment method
        for (Map.Entry<PaymentMethod, List<BillItemReportDTO>> pmEntry : paymentMethodGroups.entrySet()) {
            PaymentMethod paymentMethod = pmEntry.getKey();
            List<BillItemReportDTO> groupDtos = pmEntry.getValue();

            Map<BillCategory, List<BillItemReportDTO>> categoryMap = new HashMap<>();

            // Group DTOs by bill category within this payment method
            for (BillItemReportDTO dto : groupDtos) {
                BillCategory category = getBillCategoryFromBillTypeAtomic(dto.getBillTypeAtomic());
                categoryMap.computeIfAbsent(category, k -> new ArrayList<>()).add(dto);
            }

            nestedPaymentMethodGroups.put(paymentMethod, categoryMap);

            // Calculate category subtotals for this payment method
            calculateCategorySubtotals(paymentMethod, categoryMap);
        }

        // Calculate payment method subtotals for each payment method (existing logic)
        for (Map.Entry<PaymentMethod, List<BillItemReportDTO>> entry : paymentMethodGroups.entrySet()) {
            PaymentMethod paymentMethod = entry.getKey();
            List<BillItemReportDTO> groupDtos = entry.getValue();

            double netTotal = 0.0;
            double hospitalFeeTotal = 0.0;
            double staffFeeTotal = 0.0;

            for (BillItemReportDTO dto : groupDtos) {
                // DTOs already have correct signs (negative for cancellations), so no multiplier needed
                netTotal += (dto.getNetValue() != null ? dto.getNetValue() : 0.0);
                hospitalFeeTotal += (dto.getHospitalFee() != null ? dto.getHospitalFee() : 0.0);
                staffFeeTotal += (dto.getStaffFee() != null ? dto.getStaffFee() : 0.0);
            }

            paymentMethodSubtotals.put(paymentMethod, netTotal);
            paymentMethodHospitalFeeSubtotals.put(paymentMethod, hospitalFeeTotal);
            paymentMethodStaffFeeSubtotals.put(paymentMethod, staffFeeTotal);

            // Add to grand totals
            grandTotal += netTotal;
            grandHospitalFeeTotal += hospitalFeeTotal;
            grandStaffFeeTotal += staffFeeTotal;
        }

        totalCount = (long) dtos.size();
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
            // OPD Batch Bill Types - Normal Bills
            case OPD_BATCH_BILL_WITH_PAYMENT:
            case OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER:
            case OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER:
            // Package OPD Batch Bill Types - Normal Bills
            case PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT:
            case PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER:
            case PACKAGE_OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER:
                return BillCategory.BILL;
            case OPD_BILL_CANCELLATION:
            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
            // OPD Batch Bill Types - Cancellations
            case OPD_BATCH_BILL_CANCELLATION:
            case PACKAGE_OPD_BATCH_BILL_CANCELLATION:
                return BillCategory.CANCELLATION;
            case OPD_BILL_REFUND:
            case INWARD_SERVICE_BILL_REFUND:
            // Inward Service Batch Refund
            case INWARD_SERVICE_BATCH_BILL_REFUND:
                return BillCategory.REFUND;
            default:
                return BillCategory.BILL; // Default to BILL for unknown types
        }
    }

    /**
     * Calculate subtotals for each bill category within a payment method
     */
    private void calculateCategorySubtotals(PaymentMethod paymentMethod,
                                            Map<BillCategory, List<BillItemReportDTO>> categoryMap) {
        Map<BillCategory, Double> netSubtotals = new HashMap<>();
        Map<BillCategory, Double> hospitalFeeSubtotals = new HashMap<>();
        Map<BillCategory, Double> staffFeeSubtotals = new HashMap<>();

        for (Map.Entry<BillCategory, List<BillItemReportDTO>> categoryEntry : categoryMap.entrySet()) {
            BillCategory category = categoryEntry.getKey();
            List<BillItemReportDTO> categoryDtos = categoryEntry.getValue();

            double categoryNetTotal = 0.0;
            double categoryHospitalFeeTotal = 0.0;
            double categoryStaffFeeTotal = 0.0;

            for (BillItemReportDTO dto : categoryDtos) {
                // DTOs already have correct signs (negative for cancellations), so no multiplier needed
                categoryNetTotal += (dto.getNetValue() != null ? dto.getNetValue() : 0.0);
                categoryHospitalFeeTotal += (dto.getHospitalFee() != null ? dto.getHospitalFee() : 0.0);
                categoryStaffFeeTotal += (dto.getStaffFee() != null ? dto.getStaffFee() : 0.0);
            }

            netSubtotals.put(category, categoryNetTotal);
            hospitalFeeSubtotals.put(category, categoryHospitalFeeTotal);
            staffFeeSubtotals.put(category, categoryStaffFeeTotal);
        }

        nestedSubtotals.put(paymentMethod, netSubtotals);
        nestedHospitalFeeSubtotals.put(paymentMethod, hospitalFeeSubtotals);
        nestedStaffFeeSubtotals.put(paymentMethod, staffFeeSubtotals);
    }

    public Map<PaymentMethod, List<BillItemReportDTO>> getPaymentMethodGroups() {
        return paymentMethodGroups;
    }

    public void setPaymentMethodGroups(Map<PaymentMethod, List<BillItemReportDTO>> paymentMethodGroups) {
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

    public Map<PaymentMethod, Double> getPaymentMethodStaffFeeSubtotals() {
        return paymentMethodStaffFeeSubtotals;
    }

    public void setPaymentMethodStaffFeeSubtotals(Map<PaymentMethod, Double> paymentMethodStaffFeeSubtotals) {
        this.paymentMethodStaffFeeSubtotals = paymentMethodStaffFeeSubtotals;
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

    public Double getGrandStaffFeeTotal() {
        return grandStaffFeeTotal;
    }

    public void setGrandStaffFeeTotal(Double grandStaffFeeTotal) {
        this.grandStaffFeeTotal = grandStaffFeeTotal;
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
     * Get staff fee subtotal for a specific payment method
     */
    public Double getStaffFeeSubtotalForPaymentMethod(PaymentMethod paymentMethod) {
        return paymentMethodStaffFeeSubtotals.getOrDefault(paymentMethod, 0.0);
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
    public Map<PaymentMethod, Map<BillCategory, List<BillItemReportDTO>>> getNestedPaymentMethodGroups() {
        return nestedPaymentMethodGroups;
    }

    public void setNestedPaymentMethodGroups(Map<PaymentMethod, Map<BillCategory, List<BillItemReportDTO>>> nestedPaymentMethodGroups) {
        this.nestedPaymentMethodGroups = nestedPaymentMethodGroups;
    }

    /**
     * Get bill category subtotal for a specific payment method and category
     * @param paymentMethod The payment method
     * @param category The bill category
     * @param feeType "NET", "HOSPITAL", or "STAFF"
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
            case "STAFF":
            case "DOCTOR":
                return nestedStaffFeeSubtotals.getOrDefault(paymentMethod, new HashMap<>()).getOrDefault(category, 0.0);
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
     * Get nested staff fee subtotals map
     */
    public Map<PaymentMethod, Map<BillCategory, Double>> getNestedStaffFeeSubtotals() {
        return nestedStaffFeeSubtotals;
    }

    public void setNestedStaffFeeSubtotals(Map<PaymentMethod, Map<BillCategory, Double>> nestedStaffFeeSubtotals) {
        this.nestedStaffFeeSubtotals = nestedStaffFeeSubtotals;
    }
}
