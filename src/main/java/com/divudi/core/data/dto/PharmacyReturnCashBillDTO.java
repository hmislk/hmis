package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy return cash bills to avoid loading full entity graphs.
 * Used in pharmacy_search_pre_refund_bill_for_return_cash.xhtml for nested table display.
 */
public class PharmacyReturnCashBillDTO implements Serializable {

    private Long id;
    private Long parentBillId;  // Links back to PharmacyRefundBillDTO
    private String deptId;
    private Date createdAt;
    private Boolean cancelled;
    private Date cancelledBillCreatedAt;
    private String creatorName;
    private String cancelledBillCreatorName;
    private Double netTotal;
    private Long refundedBillId;  // To check if null

    /**
     * Constructor for DTO projection query
     */
    public PharmacyReturnCashBillDTO(
            Long id,
            Long parentBillId,
            String deptId,
            Date createdAt,
            Boolean cancelled,
            Date cancelledBillCreatedAt,
            String creatorName,
            String cancelledBillCreatorName,
            Double netTotal,
            Long refundedBillId) {
        this.id = id;
        this.parentBillId = parentBillId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.cancelled = cancelled;
        this.cancelledBillCreatedAt = cancelledBillCreatedAt;
        this.creatorName = creatorName;
        this.cancelledBillCreatorName = cancelledBillCreatorName;
        this.netTotal = netTotal;
        this.refundedBillId = refundedBillId;
    }

    public PharmacyReturnCashBillDTO() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentBillId() {
        return parentBillId;
    }

    public void setParentBillId(Long parentBillId) {
        this.parentBillId = parentBillId;
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

    public Long getRefundedBillId() {
        return refundedBillId;
    }

    public void setRefundedBillId(Long refundedBillId) {
        this.refundedBillId = refundedBillId;
    }
}
