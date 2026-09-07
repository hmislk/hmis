package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Title;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for the "List of Cancelled Lab Bills" report
 * (Lab Analytics → Performance).
 *
 * Projection from CancelledBill rows whose underlying billed bill contains
 * Investigation items.
 */
public class CancelledBillDTO implements Serializable {

    private String originalBillNumber;
    private String cancelBillNumber;
    private Date billedAt;
    private Date cancelledAt;
    private String cancelledBy;
    private String reasonForCancel;
    private PaymentMethod paymentMethod;
    private Double amount;
    private Title cancelledByTitle;
    private String billType;
    private String cancelledByNameTitle;

    public CancelledBillDTO() {
    }

    public CancelledBillDTO(String originalBillNumber,
            String cancelBillNumber,
            Date billedAt,
            Date cancelledAt,
            Title cancelledByTitle,
            String cancelledBy,
            String reasonForCancel,
            PaymentMethod paymentMethod,
            Double amount,
            String billType) {
        this.originalBillNumber = originalBillNumber;
        this.cancelBillNumber = cancelBillNumber;
        this.billedAt = billedAt;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.reasonForCancel = reasonForCancel;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.cancelledByTitle = cancelledByTitle;
        this.billType = billType;
    }

    public String getOriginalBillNumber() {
        return originalBillNumber;
    }

    public void setOriginalBillNumber(String originalBillNumber) {
        this.originalBillNumber = originalBillNumber;
    }

    public String getCancelBillNumber() {
        return cancelBillNumber;
    }

    public void setCancelBillNumber(String cancelBillNumber) {
        this.cancelBillNumber = cancelBillNumber;
    }

    public Date getBilledAt() {
        return billedAt;
    }

    public void setBilledAt(Date billedAt) {
        this.billedAt = billedAt;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getReasonForCancel() {
        return reasonForCancel;
    }

    public void setReasonForCancel(String reasonForCancel) {
        this.reasonForCancel = reasonForCancel;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getAbsoluteAmount() {
        return amount == null ? 0.0 : Math.abs(amount);
    }

    public Title getCancelledByTitle() {
        return cancelledByTitle;
    }

    public void setCancelledByTitle(Title cancelledByTitle) {
        this.cancelledByTitle = cancelledByTitle;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public String getCancelledByDisplay() {
        String namePart = cancelledBy == null ? "" : cancelledBy;
        if (cancelledByTitle == null) {
            return namePart;
        }
        return cancelledByTitle + ". " + namePart;
    }

    public String getBillTypeLabel() {
        return billType == null ? "" : billType;
    }

    public String getCancelledByNameTitle() {
        String temT;
        Title t = getCancelledByTitle();
        if (t != null) {
            temT = t.getLabel();
        } else {
            temT = "";
        }
        cancelledByNameTitle = temT + " " + getCancelledByTitle();
        return cancelledByNameTitle;
    }

    public void setCancelledByNameTitle(String cancelledByNameTitle) {
        this.cancelledByNameTitle = cancelledByNameTitle;
    }
}
