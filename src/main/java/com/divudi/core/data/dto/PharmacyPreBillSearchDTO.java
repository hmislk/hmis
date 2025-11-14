package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy pre-bill search to avoid loading full entity graphs.
 * Used in pharmacy_search_pre_bill_for_return_item_and_cash.xhtml
 */
public class PharmacyPreBillSearchDTO implements Serializable {

    private Long id;
    private Long referenceBillId;
    private String deptId;
    private Date createdAt;
    private Boolean cancelled;
    private Date cancelledBillCreatedAt;
    private String creatorName;
    private String cancelledBillCreatorName;
    private String patientName;
    private BillTypeAtomic billTypeAtomic;
    private PaymentMethod paymentMethod;
    private Double netTotal;

    // Additional fields for pharmacy_search_sale_pre_bill.xhtml
    private String departmentName;
    private String referenceBillDeptId;
    private Date referenceBillCreatedAt;
    private String referenceBillCreatorName;
    private String paymentSchemeName;
    private Boolean refunded;
    private Date refundedBillCreatedAt;
    private String refundedBillCreatorName;
    private String refundedBillComments;
    private Boolean retired;
    private Date retiredAt;
    private Double total;
    private Double discount;
    private String toStaffName;
    private String toDepartmentName;
    private String toInstitutionName;
    private String cancelledBillComments;
    private Boolean referenceBillCancelled;
    private Date referenceBillCancelledBillCreatedAt;
    private String referenceBillCancelledBillCreatorName;
    private Boolean referenceBillRefunded;
    private Date referenceBillRefundedBillCreatedAt;
    private String referenceBillRefundedBillCreatorName;

    /**
     * Constructor for DTO projection query
     */
    public PharmacyPreBillSearchDTO(
            Long id,
            Long referenceBillId,
            String deptId,
            Date createdAt,
            Boolean cancelled,
            Date cancelledBillCreatedAt,
            String creatorName,
            String cancelledBillCreatorName,
            String patientName,
            BillTypeAtomic billTypeAtomic,
            PaymentMethod paymentMethod,
            Double netTotal) {
        this.id = id;
        this.referenceBillId = referenceBillId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.cancelled = cancelled;
        this.cancelledBillCreatedAt = cancelledBillCreatedAt;
        this.creatorName = creatorName;
        this.cancelledBillCreatorName = cancelledBillCreatorName;
        this.patientName = patientName;
        this.billTypeAtomic = billTypeAtomic;
        this.paymentMethod = paymentMethod;
        this.netTotal = netTotal;
    }

    public PharmacyPreBillSearchDTO() {
    }

    /**
     * Extended constructor for pharmacy_search_sale_pre_bill.xhtml
     * Includes all fields needed for the cashier settlement search page
     */
    public PharmacyPreBillSearchDTO(
            Long id,
            String deptId,
            Date createdAt,
            String creatorName,
            String departmentName,
            Boolean cancelled,
            Date cancelledBillCreatedAt,
            String cancelledBillCreatorName,
            String cancelledBillComments,
            Boolean refunded,
            Date refundedBillCreatedAt,
            String refundedBillCreatorName,
            String refundedBillComments,
            Boolean retired,
            Date retiredAt,
            Long referenceBillId,
            String referenceBillDeptId,
            Date referenceBillCreatedAt,
            String referenceBillCreatorName,
            Boolean referenceBillCancelled,
            Date referenceBillCancelledBillCreatedAt,
            String referenceBillCancelledBillCreatorName,
            Boolean referenceBillRefunded,
            Date referenceBillRefundedBillCreatedAt,
            String referenceBillRefundedBillCreatorName,
            PaymentMethod paymentMethod,
            String paymentSchemeName,
            String patientName,
            String toStaffName,
            String toDepartmentName,
            String toInstitutionName,
            Double total,
            Double discount,
            Double netTotal) {
        this.id = id;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.departmentName = departmentName;
        this.cancelled = cancelled;
        this.cancelledBillCreatedAt = cancelledBillCreatedAt;
        this.cancelledBillCreatorName = cancelledBillCreatorName;
        this.cancelledBillComments = cancelledBillComments;
        this.refunded = refunded;
        this.refundedBillCreatedAt = refundedBillCreatedAt;
        this.refundedBillCreatorName = refundedBillCreatorName;
        this.refundedBillComments = refundedBillComments;
        this.retired = retired;
        this.retiredAt = retiredAt;
        this.referenceBillId = referenceBillId;
        this.referenceBillDeptId = referenceBillDeptId;
        this.referenceBillCreatedAt = referenceBillCreatedAt;
        this.referenceBillCreatorName = referenceBillCreatorName;
        this.referenceBillCancelled = referenceBillCancelled;
        this.referenceBillCancelledBillCreatedAt = referenceBillCancelledBillCreatedAt;
        this.referenceBillCancelledBillCreatorName = referenceBillCancelledBillCreatorName;
        this.referenceBillRefunded = referenceBillRefunded;
        this.referenceBillRefundedBillCreatedAt = referenceBillRefundedBillCreatedAt;
        this.referenceBillRefundedBillCreatorName = referenceBillRefundedBillCreatorName;
        this.paymentMethod = paymentMethod;
        this.paymentSchemeName = paymentSchemeName;
        this.patientName = patientName;
        this.toStaffName = toStaffName;
        this.toDepartmentName = toDepartmentName;
        this.toInstitutionName = toInstitutionName;
        this.total = total;
        this.discount = discount;
        this.netTotal = netTotal;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReferenceBillId() {
        return referenceBillId;
    }

    public void setReferenceBillId(Long referenceBillId) {
        this.referenceBillId = referenceBillId;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Date getCancelledBillCreatedAt() {
        return cancelledBillCreatedAt;
    }

    public void setCancelledBillCreatedAt(Date cancelledBillCreatedAt) {
        this.cancelledBillCreatedAt = cancelledBillCreatedAt;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCancelledBillCreatorName() {
        return cancelledBillCreatorName;
    }

    public void setCancelledBillCreatorName(String cancelledBillCreatorName) {
        this.cancelledBillCreatorName = cancelledBillCreatorName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getReferenceBillDeptId() {
        return referenceBillDeptId;
    }

    public void setReferenceBillDeptId(String referenceBillDeptId) {
        this.referenceBillDeptId = referenceBillDeptId;
    }

    public Date getReferenceBillCreatedAt() {
        return referenceBillCreatedAt;
    }

    public void setReferenceBillCreatedAt(Date referenceBillCreatedAt) {
        this.referenceBillCreatedAt = referenceBillCreatedAt;
    }

    public String getReferenceBillCreatorName() {
        return referenceBillCreatorName;
    }

    public void setReferenceBillCreatorName(String referenceBillCreatorName) {
        this.referenceBillCreatorName = referenceBillCreatorName;
    }

    public String getPaymentSchemeName() {
        return paymentSchemeName;
    }

    public void setPaymentSchemeName(String paymentSchemeName) {
        this.paymentSchemeName = paymentSchemeName;
    }

    public Boolean getRefunded() {
        return refunded;
    }

    public void setRefunded(Boolean refunded) {
        this.refunded = refunded;
    }

    public Date getRefundedBillCreatedAt() {
        return refundedBillCreatedAt;
    }

    public void setRefundedBillCreatedAt(Date refundedBillCreatedAt) {
        this.refundedBillCreatedAt = refundedBillCreatedAt;
    }

    public String getRefundedBillCreatorName() {
        return refundedBillCreatorName;
    }

    public void setRefundedBillCreatorName(String refundedBillCreatorName) {
        this.refundedBillCreatorName = refundedBillCreatorName;
    }

    public String getRefundedBillComments() {
        return refundedBillComments;
    }

    public void setRefundedBillComments(String refundedBillComments) {
        this.refundedBillComments = refundedBillComments;
    }

    public Boolean getRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public String getToStaffName() {
        return toStaffName;
    }

    public void setToStaffName(String toStaffName) {
        this.toStaffName = toStaffName;
    }

    public String getToDepartmentName() {
        return toDepartmentName;
    }

    public void setToDepartmentName(String toDepartmentName) {
        this.toDepartmentName = toDepartmentName;
    }

    public String getToInstitutionName() {
        return toInstitutionName;
    }

    public void setToInstitutionName(String toInstitutionName) {
        this.toInstitutionName = toInstitutionName;
    }

    public String getCancelledBillComments() {
        return cancelledBillComments;
    }

    public void setCancelledBillComments(String cancelledBillComments) {
        this.cancelledBillComments = cancelledBillComments;
    }

    public Boolean getReferenceBillCancelled() {
        return referenceBillCancelled;
    }

    public void setReferenceBillCancelled(Boolean referenceBillCancelled) {
        this.referenceBillCancelled = referenceBillCancelled;
    }

    public Date getReferenceBillCancelledBillCreatedAt() {
        return referenceBillCancelledBillCreatedAt;
    }

    public void setReferenceBillCancelledBillCreatedAt(Date referenceBillCancelledBillCreatedAt) {
        this.referenceBillCancelledBillCreatedAt = referenceBillCancelledBillCreatedAt;
    }

    public String getReferenceBillCancelledBillCreatorName() {
        return referenceBillCancelledBillCreatorName;
    }

    public void setReferenceBillCancelledBillCreatorName(String referenceBillCancelledBillCreatorName) {
        this.referenceBillCancelledBillCreatorName = referenceBillCancelledBillCreatorName;
    }

    public Boolean getReferenceBillRefunded() {
        return referenceBillRefunded;
    }

    public void setReferenceBillRefunded(Boolean referenceBillRefunded) {
        this.referenceBillRefunded = referenceBillRefunded;
    }

    public Date getReferenceBillRefundedBillCreatedAt() {
        return referenceBillRefundedBillCreatedAt;
    }

    public void setReferenceBillRefundedBillCreatedAt(Date referenceBillRefundedBillCreatedAt) {
        this.referenceBillRefundedBillCreatedAt = referenceBillRefundedBillCreatedAt;
    }

    public String getReferenceBillRefundedBillCreatorName() {
        return referenceBillRefundedBillCreatorName;
    }

    public void setReferenceBillRefundedBillCreatorName(String referenceBillRefundedBillCreatorName) {
        this.referenceBillRefundedBillCreatorName = referenceBillRefundedBillCreatorName;
    }
}
