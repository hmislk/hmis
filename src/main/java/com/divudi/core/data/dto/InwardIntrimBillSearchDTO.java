package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for inward interim bill (BillType.InwardIntrimBill) search.
 * Used by InwardIntrimBillSearchController / inward/inward_search_intrim.xhtml.
 *
 * Loaded directly via a SELECT NEW JPQL constructor query
 * (InwardIntrimBillSearchController#search) - never build this from an
 * entity-to-DTO conversion loop.
 */
public class InwardIntrimBillSearchDTO implements Serializable {

    private Long id;
    private String deptId;
    private String bhtNo;
    private Date createdAt;
    private Boolean cancelled;
    private Date cancelledAt;
    private String cancelledByName;
    private String createrName;
    private String patientName;
    private PaymentMethod paymentMethod;
    private Double total;
    private String cancelledComments;
    private String refundedComments;

    public InwardIntrimBillSearchDTO() {
    }

    public InwardIntrimBillSearchDTO(
            Long id,
            String deptId,
            String bhtNo,
            Date createdAt,
            Boolean cancelled,
            Date cancelledAt,
            String cancelledByName,
            String createrName,
            String patientName,
            PaymentMethod paymentMethod,
            Double total,
            String cancelledComments,
            String refundedComments) {
        this.id = id;
        this.deptId = deptId;
        this.bhtNo = bhtNo;
        this.createdAt = createdAt;
        this.cancelled = cancelled;
        this.cancelledAt = cancelledAt;
        this.cancelledByName = cancelledByName;
        this.createrName = createrName;
        this.patientName = patientName;
        this.paymentMethod = paymentMethod;
        this.total = total;
        this.cancelledComments = cancelledComments;
        this.refundedComments = refundedComments;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public String getBhtNo() { return bhtNo; }
    public void setBhtNo(String bhtNo) { this.bhtNo = bhtNo; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }

    public Date getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Date cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelledByName() { return cancelledByName; }
    public void setCancelledByName(String cancelledByName) { this.cancelledByName = cancelledByName; }

    public String getCreaterName() { return createrName; }
    public void setCreaterName(String createrName) { this.createrName = createrName; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public String getCancelledComments() { return cancelledComments; }
    public void setCancelledComments(String cancelledComments) { this.cancelledComments = cancelledComments; }

    public String getRefundedComments() { return refundedComments; }
    public void setRefundedComments(String refundedComments) { this.refundedComments = refundedComments; }
}
