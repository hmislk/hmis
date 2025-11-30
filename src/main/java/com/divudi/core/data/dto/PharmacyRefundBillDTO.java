package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy refund bills to avoid loading full entity graphs.
 * Used in pharmacy_search_pre_refund_bill_for_return_cash.xhtml
 */
public class PharmacyRefundBillDTO implements Serializable {

    private Long id;
    private String deptId;
    private Date createdAt;
    private Boolean cancelled;
    private Date cancelledBillCreatedAt;
    private String creatorName;
    private String cancelledBillCreatorName;
    private Double netTotal;

    // New fields for paid bill information
    private Boolean paid;
    private Date paidBillCreatedAt;
    private String paidBillCreatorName;
    private PaymentMethod paymentMethod;

    /**
     * Constructor for DTO projection query (original)
     */
    public PharmacyRefundBillDTO(
            Long id,
            String deptId,
            Date createdAt,
            Boolean cancelled,
            Date cancelledBillCreatedAt,
            String creatorName,
            String cancelledBillCreatorName,
            Double netTotal) {
        this.id = id;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.cancelled = cancelled;
        this.cancelledBillCreatedAt = cancelledBillCreatedAt;
        this.creatorName = creatorName;
        this.cancelledBillCreatorName = cancelledBillCreatorName;
        this.netTotal = netTotal;
    }

    /**
     * Constructor for DTO projection query with paid bill information
     */
    public PharmacyRefundBillDTO(
            Long id,
            String deptId,
            Date createdAt,
            Boolean cancelled,
            Date cancelledBillCreatedAt,
            String creatorName,
            String cancelledBillCreatorName,
            Double netTotal,
            Boolean paid,
            Date paidBillCreatedAt,
            String paidBillCreatorName,
            PaymentMethod paymentMethod) {
        this.id = id;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.cancelled = cancelled;
        this.cancelledBillCreatedAt = cancelledBillCreatedAt;
        this.creatorName = creatorName;
        this.cancelledBillCreatorName = cancelledBillCreatorName;
        this.netTotal = netTotal;
        this.paid = paid;
        this.paidBillCreatedAt = paidBillCreatedAt;
        this.paidBillCreatorName = paidBillCreatorName;
        this.paymentMethod = paymentMethod;
    }

    public PharmacyRefundBillDTO() {
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

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public Date getPaidBillCreatedAt() {
        return paidBillCreatedAt;
    }

    public void setPaidBillCreatedAt(Date paidBillCreatedAt) {
        this.paidBillCreatedAt = paidBillCreatedAt;
    }

    public String getPaidBillCreatorName() {
        return paidBillCreatorName;
    }

    public void setPaidBillCreatorName(String paidBillCreatorName) {
        this.paidBillCreatorName = paidBillCreatorName;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
