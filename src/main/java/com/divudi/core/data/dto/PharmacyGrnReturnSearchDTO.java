package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy GRN return bill search (issue #21014 / #20299).
 * Eliminates N+1 lazy-load storms from the old Bill-entity approach.
 */
public class PharmacyGrnReturnSearchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deptId;
    private String referenceBillDeptId;
    private String toInstitutionName;
    private Date createdAt;
    private String creatorName;
    private boolean cancelled;
    private Date cancelledAt;
    private String cancelledBy;
    private boolean refunded;
    private Date refundedAt;
    private String refundedBy;
    private String cancelRefundComments;
    private String paymentMethod;
    private Double netTotal;
    private Double saleValue;

    public PharmacyGrnReturnSearchDTO() {
    }

    public PharmacyGrnReturnSearchDTO(
            Long id,
            String deptId,
            String referenceBillDeptId,
            String toInstitutionName,
            Date createdAt,
            String creatorName,
            Boolean cancelled,
            Date cancelledAt,
            String cancelledBy,
            Boolean refunded,
            Date refundedAt,
            String refundedBy,
            String cancelRefundComments,
            Object paymentMethod,
            Double netTotal,
            Double saleValue) {
        this.id = id;
        this.deptId = deptId;
        this.referenceBillDeptId = referenceBillDeptId;
        this.toInstitutionName = toInstitutionName;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.cancelled = cancelled != null ? cancelled : false;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.refunded = refunded != null ? refunded : false;
        this.refundedAt = refundedAt;
        this.refundedBy = refundedBy;
        this.cancelRefundComments = cancelRefundComments;
        this.paymentMethod = paymentMethod != null ? paymentMethod.toString() : null;
        this.netTotal = netTotal;
        this.saleValue = saleValue;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public String getReferenceBillDeptId() { return referenceBillDeptId; }
    public void setReferenceBillDeptId(String referenceBillDeptId) { this.referenceBillDeptId = referenceBillDeptId; }

    public String getToInstitutionName() { return toInstitutionName; }
    public void setToInstitutionName(String toInstitutionName) { this.toInstitutionName = toInstitutionName; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public Date getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Date cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public boolean isRefunded() { return refunded; }
    public void setRefunded(boolean refunded) { this.refunded = refunded; }

    public Date getRefundedAt() { return refundedAt; }
    public void setRefundedAt(Date refundedAt) { this.refundedAt = refundedAt; }

    public String getRefundedBy() { return refundedBy; }
    public void setRefundedBy(String refundedBy) { this.refundedBy = refundedBy; }

    public String getCancelRefundComments() { return cancelRefundComments; }
    public void setCancelRefundComments(String cancelRefundComments) { this.cancelRefundComments = cancelRefundComments; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Double getNetTotal() { return netTotal; }
    public void setNetTotal(Double netTotal) { this.netTotal = netTotal; }

    public Double getSaleValue() { return saleValue; }
    public void setSaleValue(Double saleValue) { this.saleValue = saleValue; }
}
