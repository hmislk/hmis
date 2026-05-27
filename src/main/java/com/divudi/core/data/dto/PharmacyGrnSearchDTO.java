package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy GRN search list (issue #21012 / #20299).
 * Eliminates N+1 lazy-load storms from the old Bill-entity approach.
 */
public class PharmacyGrnSearchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deptId;
    private String referenceBillDeptId;
    private String invoiceNumber;
    private Date invoiceDate;
    private String fromInstitutionName;
    private Date createdAt;
    private String creatorName;
    private String paymentMethod;
    private Double netTotal;
    private Double saleValue;
    private boolean cancelled;

    public PharmacyGrnSearchDTO() {
    }

    public PharmacyGrnSearchDTO(
            Long id,
            String deptId,
            String referenceBillDeptId,
            String invoiceNumber,
            Date invoiceDate,
            String fromInstitutionName,
            Date createdAt,
            String creatorName,
            Object paymentMethod,
            Double netTotal,
            Double saleValue,
            Boolean cancelled) {
        this.id = id;
        this.deptId = deptId;
        this.referenceBillDeptId = referenceBillDeptId;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.fromInstitutionName = fromInstitutionName;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.paymentMethod = paymentMethod != null ? paymentMethod.toString() : null;
        this.netTotal = netTotal;
        this.saleValue = saleValue;
        this.cancelled = cancelled != null ? cancelled : false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public String getReferenceBillDeptId() { return referenceBillDeptId; }
    public void setReferenceBillDeptId(String referenceBillDeptId) { this.referenceBillDeptId = referenceBillDeptId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public Date getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(Date invoiceDate) { this.invoiceDate = invoiceDate; }

    public String getFromInstitutionName() { return fromInstitutionName; }
    public void setFromInstitutionName(String fromInstitutionName) { this.fromInstitutionName = fromInstitutionName; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Double getNetTotal() { return netTotal; }
    public void setNetTotal(Double netTotal) { this.netTotal = netTotal; }

    public Double getSaleValue() { return saleValue; }
    public void setSaleValue(Double saleValue) { this.saleValue = saleValue; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
