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
}
