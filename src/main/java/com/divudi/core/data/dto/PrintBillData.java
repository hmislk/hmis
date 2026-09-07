package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PrintBillData implements Serializable {

    private static final long serialVersionUID = 1L;

    // Department / Institution header
    private String departmentName;
    private String departmentPrintingName;
    private String departmentTelephone1;
    private String departmentTelephone2;
    private String departmentFax;
    private String departmentAddress;
    private String departmentSiteName;
    private String institutionName;
    private String institutionAddress;
    private String institutionEmail;
    private String institutionWeb;

    // Bill identity
    private String billNo;
    private Date createdAt;
    private String creatorName;
    private String billIdStr;
    private boolean cancelled;
    // Bill.invoiceNumber — shown on the FiveFive cashier bill formats.
    private String invoiceNumber;
    // WebUser.code of the bill's creator — printed as "Cashier : <code>" on the
    // FiveFive cashier format.
    private String creatorCode;
    // Pharmacy token number (Token.tokenNumber). Null when the token system is disabled
    // or the bill has no token; the token composites render nothing in that case.
    private String tokenNumber;

    // Patient
    private String patientName;
    private String patientPhone;
    private String patientPhn;
    private String patientAgeSex;
    private String bhtNo;
    private String roomName;

    // Payment
    private String paymentMethodLabel;
    private String paymentSchemePrintingName;
    private String comment;
    private double total;
    private double discount;
    private double discountPercentPharmacy;
    private double netTotal;
    private double cashPaid;
    private double balance;
    private double margin;

    // Individual payment lines (one per component) for Multiple Payment Methods.
    // Empty for single-method bills.
    private List<PaymentLine> payments = new ArrayList<>();

    // Credit / Staff / Dept targets (shown on bill when applicable)
    private String toStaffName;
    private String toDepartmentName;
    private String toInstitutionName;

    // --- Department / Institution ---

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDepartmentPrintingName() { return departmentPrintingName; }
    public void setDepartmentPrintingName(String departmentPrintingName) { this.departmentPrintingName = departmentPrintingName; }

    public String getDepartmentTelephone1() { return departmentTelephone1; }
    public void setDepartmentTelephone1(String departmentTelephone1) { this.departmentTelephone1 = departmentTelephone1; }

    public String getDepartmentTelephone2() { return departmentTelephone2; }
    public void setDepartmentTelephone2(String departmentTelephone2) { this.departmentTelephone2 = departmentTelephone2; }

    public String getDepartmentFax() { return departmentFax; }
    public void setDepartmentFax(String departmentFax) { this.departmentFax = departmentFax; }

    public String getDepartmentAddress() { return departmentAddress; }
    public void setDepartmentAddress(String departmentAddress) { this.departmentAddress = departmentAddress; }

    public String getDepartmentSiteName() { return departmentSiteName; }
    public void setDepartmentSiteName(String departmentSiteName) { this.departmentSiteName = departmentSiteName; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getInstitutionAddress() { return institutionAddress; }
    public void setInstitutionAddress(String institutionAddress) { this.institutionAddress = institutionAddress; }

    public String getInstitutionEmail() { return institutionEmail; }
    public void setInstitutionEmail(String institutionEmail) { this.institutionEmail = institutionEmail; }

    public String getInstitutionWeb() { return institutionWeb; }
    public void setInstitutionWeb(String institutionWeb) { this.institutionWeb = institutionWeb; }

    // --- Bill identity ---

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getBillIdStr() { return billIdStr; }
    public void setBillIdStr(String billIdStr) { this.billIdStr = billIdStr; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getCreatorCode() { return creatorCode; }
    public void setCreatorCode(String creatorCode) { this.creatorCode = creatorCode; }

    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }

    // --- Patient ---

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getPatientPhn() { return patientPhn; }
    public void setPatientPhn(String patientPhn) { this.patientPhn = patientPhn; }

    public String getPatientAgeSex() { return patientAgeSex; }
    public void setPatientAgeSex(String patientAgeSex) { this.patientAgeSex = patientAgeSex; }

    public String getBhtNo() { return bhtNo; }
    public void setBhtNo(String bhtNo) { this.bhtNo = bhtNo; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    // --- Payment ---

    public String getPaymentMethodLabel() { return paymentMethodLabel; }
    public void setPaymentMethodLabel(String paymentMethodLabel) { this.paymentMethodLabel = paymentMethodLabel; }

    public String getPaymentSchemePrintingName() { return paymentSchemePrintingName; }
    public void setPaymentSchemePrintingName(String paymentSchemePrintingName) { this.paymentSchemePrintingName = paymentSchemePrintingName; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getDiscountPercentPharmacy() { return discountPercentPharmacy; }
    public void setDiscountPercentPharmacy(double discountPercentPharmacy) { this.discountPercentPharmacy = discountPercentPharmacy; }

    public double getNetTotal() { return netTotal; }
    public void setNetTotal(double netTotal) { this.netTotal = netTotal; }

    public double getCashPaid() { return cashPaid; }
    public void setCashPaid(double cashPaid) { this.cashPaid = cashPaid; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getMargin() { return margin; }
    public void setMargin(double margin) { this.margin = margin; }

    public List<PaymentLine> getPayments() { return payments; }
    public void setPayments(List<PaymentLine> payments) { this.payments = payments; }

    // --- Targets ---

    public String getToStaffName() { return toStaffName; }
    public void setToStaffName(String toStaffName) { this.toStaffName = toStaffName; }

    public String getToDepartmentName() { return toDepartmentName; }
    public void setToDepartmentName(String toDepartmentName) { this.toDepartmentName = toDepartmentName; }

    public String getToInstitutionName() { return toInstitutionName; }
    public void setToInstitutionName(String toInstitutionName) { this.toInstitutionName = toInstitutionName; }

    /**
     * One settled payment line for printing. {@code paymentMethodLabel} is the
     * human-readable method name (e.g. "Cash", "Credit Card") and {@code reference}
     * carries any method-specific detail to show (card last digits, cheque/slip
     * reference, etc.); it may be empty.
     */
    public static class PaymentLine implements Serializable {

        private static final long serialVersionUID = 1L;

        private String paymentMethodLabel;
        private double value;
        private String reference;

        public PaymentLine() {
        }

        public PaymentLine(String paymentMethodLabel, double value, String reference) {
            this.paymentMethodLabel = paymentMethodLabel;
            this.value = value;
            this.reference = reference;
        }

        public String getPaymentMethodLabel() { return paymentMethodLabel; }
        public void setPaymentMethodLabel(String paymentMethodLabel) { this.paymentMethodLabel = paymentMethodLabel; }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
    }
}
