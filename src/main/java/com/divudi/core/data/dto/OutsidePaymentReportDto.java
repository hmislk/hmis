package com.divudi.core.data.dto;

import com.divudi.core.data.inward.InwardChargeType;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Thisara Samuditha | github - thisarasamuditha |  thellamburavithanagethisarasam@gmail.com
 */
public class OutsidePaymentReportDto implements Serializable {

    private final Long billItemId;
    private final Long billId;
    private final String invoiceNo;
    private final String mrn;
    private final String patientName;
    private final String bhtNo;
    private final String description;
    private final Date dischargedOn;
    private final Boolean cancelled;
    private final Boolean refunded;
    private final InwardChargeType inwardChargeType;
    private final String createrName;
    private final Date createdDate;
    private final Double invoiceAddedAmount;
    private final Date paidDate;
    private Double paidAmount;
    private final Double invoiceTotal;
    private String memo;
    private Boolean paid;

    public OutsidePaymentReportDto(
            Long billItemId,
            Long billId,
            String invoiceNo,
            String mrn,
            String patientName,
            String bhtNo,
            String description,
            Date dischargedOn,
            Boolean cancelled,
            Boolean refunded,
            InwardChargeType inwardChargeType,
            String createrName,
            Date createdDate,
            Double invoiceAddedAmount,
            Date paidDate,
            Double paidAmount,
            Double invoiceTotal,
            String memo,
            Boolean paid) {
        this.billItemId = billItemId;
        this.billId = billId;
        this.invoiceNo = invoiceNo;
        this.mrn = mrn;
        this.patientName = patientName;
        this.bhtNo = bhtNo;
        this.description = description;
        this.dischargedOn = dischargedOn;
        this.cancelled = cancelled != null ? cancelled : false;
        this.refunded = refunded != null ? refunded : false;
        this.inwardChargeType = inwardChargeType;
        this.createrName = createrName;
        this.createdDate = createdDate;
        this.invoiceAddedAmount = invoiceAddedAmount != null ? invoiceAddedAmount : 0.0;
        this.paidDate = paidDate;
        this.paidAmount = paidAmount != null ? paidAmount : 0.0;
        this.invoiceTotal = invoiceTotal != null ? invoiceTotal : 0.0;
        this.memo = memo;
        this.paid = paid != null ? paid : false;
    }

    public Long getBillItemId() {
        return billItemId;
    }

    public Long getBillId() {
        return billId;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public String getMrn() {
        return mrn;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public String getDescription() {
        return description;
    }

    public Date getDischargedOn() {
        return dischargedOn;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public Boolean getRefunded() {
        return refunded;
    }

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }

    public String getCreaterName() {
        return createrName;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Double getInvoiceAddedAmount() {
        return invoiceAddedAmount;
    }

    public Date getPaidDate() {
        return paidDate;
    }

    public Double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Double getInvoiceTotal() {
        return invoiceTotal;
    }

    public Double getDueAmount() {
        return invoiceTotal - (paidAmount != null ? paidAmount : 0.0);
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    /**
     * "Cancelled", "Refunded", "Cancelled / Refunded" or "" — for display only.
     */
    public String getCancelledOrRefundedLabel() {
        if (Boolean.TRUE.equals(cancelled) && Boolean.TRUE.equals(refunded)) {
            return "Cancelled / Refunded";
        }
        if (Boolean.TRUE.equals(cancelled)) {
            return "Cancelled";
        }
        if (Boolean.TRUE.equals(refunded)) {
            return "Refunded";
        }
        return "";
    }
}