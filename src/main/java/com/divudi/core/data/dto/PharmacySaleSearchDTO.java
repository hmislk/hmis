package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy retail sale bill search.
 * Used in pharmacy/pharmacy_search_sale_bill.xhtml
 * Covers both PreBill-based sales and native PHARMACY_RETAIL_SALE bills.
 */
public class PharmacySaleSearchDTO implements Serializable {

    private Long id;
    private String deptId;
    private String departmentName;
    private Date createdAt;
    private String creatorName;
    private String patientName;
    private String referredByName;
    private PaymentMethod paymentMethod;
    private String paymentSchemeName;
    private Double total;
    private Double discount;
    private Double netTotal;
    private Boolean refunded;
    private Boolean cancelled;
    private String comments;

    public PharmacySaleSearchDTO() {
    }

    // Original 14-parameter constructor — kept for backward compatibility
    public PharmacySaleSearchDTO(
            Long id,
            String deptId,
            String departmentName,
            Date createdAt,
            String creatorName,
            String patientName,
            String referredByName,
            PaymentMethod paymentMethod,
            String paymentSchemeName,
            Double total,
            Double discount,
            Double netTotal,
            Boolean refunded,
            Boolean cancelled) {
        this.id = id;
        this.deptId = deptId;
        this.departmentName = departmentName;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.patientName = patientName;
        this.referredByName = referredByName;
        this.paymentMethod = paymentMethod;
        this.paymentSchemeName = paymentSchemeName;
        this.total = total;
        this.discount = discount;
        this.netTotal = netTotal;
        this.refunded = refunded;
        this.cancelled = cancelled;
    }

    // New 15-parameter constructor that includes comments
    // delegates to the 14-parameter constructor via this(...)
    public PharmacySaleSearchDTO(
            Long id,
            String deptId,
            String departmentName,
            Date createdAt,
            String creatorName,
            String patientName,
            String referredByName,
            PaymentMethod paymentMethod,
            String paymentSchemeName,
            Double total,
            Double discount,
            Double netTotal,
            Boolean refunded,
            Boolean cancelled,
            String comments) {
        this(id, deptId, departmentName, createdAt, creatorName, patientName,
                referredByName, paymentMethod, paymentSchemeName, total, discount,
                netTotal, refunded, cancelled);
        this.comments = comments;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getReferredByName() { return referredByName; }
    public void setReferredByName(String referredByName) { this.referredByName = referredByName; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentSchemeName() { return paymentSchemeName; }
    public void setPaymentSchemeName(String paymentSchemeName) { this.paymentSchemeName = paymentSchemeName; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public Double getNetTotal() { return netTotal; }
    public void setNetTotal(Double netTotal) { this.netTotal = netTotal; }

    public Boolean getRefunded() { return refunded; }
    public void setRefunded(Boolean refunded) { this.refunded = refunded; }

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
