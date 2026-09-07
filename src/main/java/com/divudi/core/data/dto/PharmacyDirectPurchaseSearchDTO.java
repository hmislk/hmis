package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy direct purchase bill search (issue #21013 / #20299).
 * Eliminates N+1 lazy-load storms from the old Bill-entity approach.
 */
public class PharmacyDirectPurchaseSearchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deptId;
    private String invoiceNumber;
    private Date invoiceDate;
    private Date createdAt;
    private String creatorName;
    private String fromInstitutionName;
    private String paymentMethod;
    private Double netTotal;
    private Double saleValue;
    private boolean cancelled;
    private Date cancelledAt;
    private String cancelledBy;
    private String cancelComments;
    private boolean refunded;
    private Date refundedAt;
    private String refundedBy;
    private String refundComments;

    public PharmacyDirectPurchaseSearchDTO() {
    }

    public PharmacyDirectPurchaseSearchDTO(
            Long id,
            String deptId,
            String invoiceNumber,
            Date invoiceDate,
            Date createdAt,
            String creatorName,
            String fromInstitutionName,
            Object paymentMethod,
            Double netTotal,
            Double saleValue,
            Boolean cancelled,
            Date cancelledAt,
            String cancelledBy,
            String cancelComments,
            Boolean refunded,
            Date refundedAt,
            String refundedBy,
            String refundComments) {
        this.id = id;
        this.deptId = deptId;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.fromInstitutionName = fromInstitutionName;
        this.paymentMethod = paymentMethod != null ? paymentMethod.toString() : null;
        this.netTotal = netTotal;
        this.saleValue = saleValue;
        this.cancelled = cancelled != null ? cancelled : false;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.cancelComments = cancelComments;
        this.refunded = refunded != null ? refunded : false;
        this.refundedAt = refundedAt;
        this.refundedBy = refundedBy;
        this.refundComments = refundComments;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public Date getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(Date invoiceDate) { this.invoiceDate = invoiceDate; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getFromInstitutionName() { return fromInstitutionName; }
    public void setFromInstitutionName(String fromInstitutionName) { this.fromInstitutionName = fromInstitutionName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Double getNetTotal() { return netTotal; }
    public void setNetTotal(Double netTotal) { this.netTotal = netTotal; }

    public Double getSaleValue() { return saleValue; }
    public void setSaleValue(Double saleValue) { this.saleValue = saleValue; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public Date getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Date cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getCancelComments() { return cancelComments; }
    public void setCancelComments(String cancelComments) { this.cancelComments = cancelComments; }

    public boolean isRefunded() { return refunded; }
    public void setRefunded(boolean refunded) { this.refunded = refunded; }

    public Date getRefundedAt() { return refundedAt; }
    public void setRefundedAt(Date refundedAt) { this.refundedAt = refundedAt; }

    public String getRefundedBy() { return refundedBy; }
    public void setRefundedBy(String refundedBy) { this.refundedBy = refundedBy; }

    public String getRefundComments() { return refundComments; }
    public void setRefundComments(String refundComments) { this.refundComments = refundComments; }
}
