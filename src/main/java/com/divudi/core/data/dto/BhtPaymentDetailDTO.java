package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.inward.AdmissionType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DTO for BHT Deposit and Credit Settlement Detail Report.
 * One instance per individual payment ("Make a Deposit", "Make a Payment",
 * "Post Final Payment", or CC settlement) - see {@link #getPaymentCategory()}.
 */
public class BhtPaymentDetailDTO implements Serializable {

    private String bhtNo;
    private String patientName;
    private AdmissionType admissionType;
    private Date dateOfAdmission;
    private Date dateOfDischarge;
    private Long billId;
    private String billNo;
    private Date createdAt;
    private PaymentMethod paymentMethod;
    private double amount;
    private String referenceNo;
    private String creditCompanyName;

    /**
     * Distinguishes which kind of transaction this row is: "Deposit"
     * ("Make a Deposit" / INWARD_DEPOSIT), "Payment" ("Make a Payment" /
     * INWARD_PAYMENT), "Post Payment" ("Post Final Payment" /
     * BillType.PostFinalBillInwardPayment), or "CC Settlement" (credit company
     * payment). Not set by every caller of this DTO (e.g.
     * {@code BhtDepositDetailReportController} predates this field) - callers
     * that don't set it leave rows blank rather than defaulting to a
     * possibly-wrong category.
     */
    private String paymentCategory;

    /**
     * Credit companies on this row's admission's Final Bill, each with its
     * own Due/Paid/Balance - populated only by
     * {@code BhtDepositDetailWithCreditCompaniesReportController} (issue
     * #23770); every other caller of this DTO leaves this empty. An
     * admission can carry more than one credit company, so this is a list
     * rather than reusing the single {@link #creditCompanyName} field above.
     */
    private List<CreditCompanySettlement> creditCompanySettlements = new ArrayList<>();

    public BhtPaymentDetailDTO() {
    }

    public List<CreditCompanySettlement> getCreditCompanySettlements() {
        return creditCompanySettlements;
    }

    public void setCreditCompanySettlements(List<CreditCompanySettlement> creditCompanySettlements) {
        this.creditCompanySettlements = creditCompanySettlements != null ? creditCompanySettlements : new ArrayList<>();
    }

    /**
     * One credit company's commitment on an admission's Final Bill: Due
     * (commitment bill netTotal), Paid (recomputed via the
     * CREDIT_SETTLE_BY_COMPANY settlement pattern, never the stored
     * paidAmount - see developer_docs/billing/inward-cc-settlement-tracking.md),
     * and Balance (Due - Paid).
     */
    public static class CreditCompanySettlement implements Serializable {

        private final String creditCompanyName;
        private final double due;
        private final double paid;
        private final double balance;

        public CreditCompanySettlement(String creditCompanyName, double due, double paid, double balance) {
            this.creditCompanyName = creditCompanyName;
            this.due = due;
            this.paid = paid;
            this.balance = balance;
        }

        public String getCreditCompanyName() { return creditCompanyName; }
        public double getDue() { return due; }
        public double getPaid() { return paid; }
        public double getBalance() { return balance; }
    }

    public String getBhtNo() { return bhtNo; }
    public void setBhtNo(String bhtNo) { this.bhtNo = bhtNo; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public AdmissionType getAdmissionType() { return admissionType; }
    public void setAdmissionType(AdmissionType admissionType) { this.admissionType = admissionType; }

    public Date getDateOfAdmission() { return dateOfAdmission; }
    public void setDateOfAdmission(Date dateOfAdmission) { this.dateOfAdmission = dateOfAdmission; }

    public Date getDateOfDischarge() { return dateOfDischarge; }
    public void setDateOfDischarge(Date dateOfDischarge) { this.dateOfDischarge = dateOfDischarge; }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getReferenceNo() { return referenceNo != null ? referenceNo : ""; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public String getCreditCompanyName() { return creditCompanyName != null ? creditCompanyName : ""; }
    public void setCreditCompanyName(String creditCompanyName) { this.creditCompanyName = creditCompanyName; }

    public String getPaymentCategory() { return paymentCategory != null ? paymentCategory : ""; }
    public void setPaymentCategory(String paymentCategory) { this.paymentCategory = paymentCategory; }
}
