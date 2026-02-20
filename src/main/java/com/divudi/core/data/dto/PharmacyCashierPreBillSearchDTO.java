package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for cashier pharmacy pre-bill search to avoid loading full entity graphs.
 * Used in pharmacy/pharmacy_search_sale_pre_bill.xhtml
 *
 * This DTO contains all fields needed to display pre-bill search results including:
 * - Pre-bill details (number, department, creator, dates, status)
 * - Payment bill details (if paid)
 * - Client information (patient/staff/department/institution)
 * - Financial data (total, discount, netTotal)
 * - Status information for both pre-bill and payment bill
 */
public class PharmacyCashierPreBillSearchDTO implements Serializable {

    // Pre-Bill Core Fields
    private Long id;
    private String deptId;
    private String departmentName;
    private Date createdAt;
    private String creatorName;

    // Pre-Bill Status Fields
    private Boolean refunded;
    private Date refundedBillCreatedAt;
    private String refundedBillCreatorName;
    private String refundedBillComments;
    private Boolean retired;
    private Date retiredAt;
    private Boolean cancelled;
    private Date cancelledBillCreatedAt;
    private String cancelledBillCreatorName;
    private String cancelledBillComments;

    // Financial Fields
    private Double total;
    private Double discount;
    private Double netTotal;

    // Payment Fields
    private PaymentMethod paymentMethod;
    private String paymentSchemeName;

    // Client Fields (mutually exclusive - only one will be populated)
    private String patientName;
    private String toStaffName;
    private String toDepartmentName;
    private String toInstitutionName;

    // Reference Bill (Payment Bill) Fields
    private Long referenceBillId;
    private String referenceBillDeptId;
    private Date referenceBillCreatedAt;
    private String referenceBillCreatorName;
    private Boolean referenceBillCancelled;
    private Date referenceBillCancelledBillCreatedAt;
    private String referenceBillCancelledBillCreatorName;
    private Boolean referenceBillRefunded;
    private Date referenceBillRefundedBillCreatedAt;
    private String referenceBillRefundedBillCreatorName;

    /**
     * Default constructor
     */
    public PharmacyCashierPreBillSearchDTO() {
    }

    /**
     * Simplified constructor with only essential fields
     * Used for basic pharmacy cashier pre-bill search functionality
     */
    public PharmacyCashierPreBillSearchDTO(
            Long id,
            String deptId,
            String departmentName,
            Date createdAt,
            Boolean refunded,
            Boolean cancelled,
            Double total,
            Double discount,
            Double netTotal,
            PaymentMethod paymentMethod,
            String patientName) {
        this.id = id;
        this.deptId = deptId;
        this.departmentName = departmentName;
        this.createdAt = createdAt;
        this.refunded = refunded;
        this.cancelled = cancelled;
        this.total = total;
        this.discount = discount;
        this.netTotal = netTotal;
        this.paymentMethod = paymentMethod;
        this.patientName = patientName;
    }

    /**
     * Full constructor for DTO projection query (legacy)
     * All parameters must match the order in the JPQL SELECT NEW query
     */
    public PharmacyCashierPreBillSearchDTO(
            Long id,
            String deptId,
            String departmentName,
            Date createdAt,
            String creatorName,
            Boolean refunded,
            Date refundedBillCreatedAt,
            String refundedBillCreatorName,
            String refundedBillComments,
            Boolean retired,
            Date retiredAt,
            Boolean cancelled,
            Date cancelledBillCreatedAt,
            String cancelledBillCreatorName,
            String cancelledBillComments,
            Double total,
            Double discount,
            Double netTotal,
            PaymentMethod paymentMethod,
            String paymentSchemeName,
            String patientName,
            String toStaffName,
            String toDepartmentName,
            String toInstitutionName,
            Long referenceBillId,
            String referenceBillDeptId,
            Date referenceBillCreatedAt,
            String referenceBillCreatorName,
            Boolean referenceBillCancelled,
            Date referenceBillCancelledBillCreatedAt,
            String referenceBillCancelledBillCreatorName,
            Boolean referenceBillRefunded,
            Date referenceBillRefundedBillCreatedAt,
            String referenceBillRefundedBillCreatorName) {
        this.id = id;
        this.deptId = deptId;
        this.departmentName = departmentName;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.refunded = refunded;
        this.refundedBillCreatedAt = refundedBillCreatedAt;
        this.refundedBillCreatorName = refundedBillCreatorName;
        this.refundedBillComments = refundedBillComments;
        this.retired = retired;
        this.retiredAt = retiredAt;
        this.cancelled = cancelled;
        this.cancelledBillCreatedAt = cancelledBillCreatedAt;
        this.cancelledBillCreatorName = cancelledBillCreatorName;
        this.cancelledBillComments = cancelledBillComments;
        this.total = total;
        this.discount = discount;
        this.netTotal = netTotal;
        this.paymentMethod = paymentMethod;
        this.paymentSchemeName = paymentSchemeName;
        this.patientName = patientName;
        this.toStaffName = toStaffName;
        this.toDepartmentName = toDepartmentName;
        this.toInstitutionName = toInstitutionName;
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
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
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

    public String getCancelledBillCreatorName() {
        return cancelledBillCreatorName;
    }

    public void setCancelledBillCreatorName(String cancelledBillCreatorName) {
        this.cancelledBillCreatorName = cancelledBillCreatorName;
    }

    public String getCancelledBillComments() {
        return cancelledBillComments;
    }

    public void setCancelledBillComments(String cancelledBillComments) {
        this.cancelledBillComments = cancelledBillComments;
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

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentSchemeName() {
        return paymentSchemeName;
    }

    public void setPaymentSchemeName(String paymentSchemeName) {
        this.paymentSchemeName = paymentSchemeName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
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

    public Long getReferenceBillId() {
        return referenceBillId;
    }

    public void setReferenceBillId(Long referenceBillId) {
        this.referenceBillId = referenceBillId;
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
