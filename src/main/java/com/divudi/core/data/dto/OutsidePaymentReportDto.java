package com.divudi.core.data.dto;

import com.divudi.core.data.inward.InwardChargeType;
import java.io.Serializable;
import java.util.Date;

/**
 * One row of the Outside Payment report (Issue #17867). Grain is one row per
 * BillItem on an InwardOutSideBill — Invoice No / Paid Date / Paid Amount /
 * Due Amount are Bill-level figures and therefore repeat across rows that
 * belong to the same invoice.
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
    private final Double paidAmount;
    private final Double invoiceTotal;
    private final String memo;
    private final Boolean paid;

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

    public Double getDueAmount() {
        return invoiceTotal - paidAmount;
    }

    public String getMemo() {
        return memo;
    }

    public Boolean getPaid() {
        return paid;
    }

    /** "Cancelled", "Refunded", "Cancelled / Refunded" or "" — for display only. */
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
