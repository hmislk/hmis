package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * Row of the Card Payments report. One row per Payment, whatever the bill
 * type. Navigation uses ids; display uses plain names so that no entity graph
 * is loaded.
 */
public class CardPaymentReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long paymentId;
    private Long billId;
    private String billNo;
    private BillTypeAtomic billTypeAtomic;
    private Date paymentDate;
    private PaymentMethod paymentMethod;
    private String patientName;
    private String bankName;
    private String creditCardRefNo;
    private String referenceNo;
    private String comments;
    private double paidValue;
    private String cashierName;
    private String departmentName;

    public CardPaymentReportDTO() {
    }

    public CardPaymentReportDTO(Long paymentId, Long billId, String billNo,
            BillTypeAtomic billTypeAtomic, Date paymentDate, PaymentMethod paymentMethod,
            String patientName, String bankName, String creditCardRefNo,
            String referenceNo, String comments, Double paidValue,
            String cashierName, String departmentName) {
        this.paymentId = paymentId;
        this.billId = billId;
        this.billNo = billNo;
        this.billTypeAtomic = billTypeAtomic;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.patientName = patientName;
        this.bankName = bankName;
        this.creditCardRefNo = creditCardRefNo;
        this.referenceNo = referenceNo;
        this.comments = comments;
        this.paidValue = paidValue == null ? 0.0 : paidValue;
        this.cashierName = cashierName;
        this.departmentName = departmentName;
    }

    /**
     * Card reference number to show: the credit card reference when present,
     * otherwise the generic payment reference number.
     */
    public String getReference() {
        if (creditCardRefNo != null && !creditCardRefNo.trim().isEmpty()) {
            return creditCardRefNo;
        }
        return referenceNo;
    }

    public String getBillTypeLabel() {
        return billTypeAtomic == null ? "" : billTypeAtomic.getLabel();
    }

    public String getPaymentMethodLabel() {
        return paymentMethod == null ? "" : paymentMethod.getLabel();
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getBillId() {
        return billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getBankName() {
        return bankName;
    }

    public String getCreditCardRefNo() {
        return creditCardRefNo;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public String getComments() {
        return comments;
    }

    public double getPaidValue() {
        return paidValue;
    }

    public String getCashierName() {
        return cashierName;
    }

    public String getDepartmentName() {
        return departmentName;
    }
}
