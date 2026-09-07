package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PaymentScheme;
import java.io.Serializable;
import java.util.Date;

/**
 * Enhanced DTO for Laboratory Income Report optimization Matches the
 * functionality of the original report with all payment breakdown fields
 */
public class LabIncomeReportDTO implements Serializable {

    private Long billId;
    private String billNumber;
    private Date billDate;
    private String patientName;
    private String departmentName;
    private String institutionName;
    private Double netTotal;
    private Double total;

    // Critical fields for payment calculation (like OpdIncomeReportDTO)
    private BillTypeAtomic billTypeAtomic;
    private PaymentMethod paymentMethod;
    private PatientEncounter patientEncounter;
    private PaymentScheme paymentScheme;

    // Payment breakdown fields to match original report
    private Double cashValue;
    private Double cardValue;
    private Double inpatientCreditValue;
    private Double opdCreditValue;
    private Double staffValue;
    private Double agentValue;
    private Double discount;
    private Double serviceCharge;

    public LabIncomeReportDTO() {
    }

    public LabIncomeReportDTO(Long billId, String billNumber, Date billDate,
            String departmentName, String institutionName,
            Double netTotal, Double total) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.departmentName = departmentName;
        this.institutionName = institutionName;
        this.netTotal = netTotal;
        this.total = total;
    }

    public LabIncomeReportDTO(Long billId, String billNumber, Date billDate, String patientName,
            String departmentName, String institutionName,
            Double netTotal, Double total,
            BillTypeAtomic billTypeAtomic, PaymentMethod paymentMethod,
            PatientEncounter patientEncounter, Double discount,
            Double serviceCharge, PaymentScheme paymentScheme) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.patientName = patientName;
        this.departmentName = departmentName;
        this.institutionName = institutionName;
        this.netTotal = netTotal;
        this.total = total;
        this.billTypeAtomic = billTypeAtomic;
        this.paymentMethod = paymentMethod;
        this.patientEncounter = patientEncounter;
        this.discount = discount;
        this.serviceCharge = serviceCharge;
        this.paymentScheme = paymentScheme;
    }

    public LabIncomeReportDTO(Long billId,
            String billNumber,
            Date billDate,
            Double netTotal,
            Double total,
            BillTypeAtomic billTypeAtomic,
            PaymentMethod paymentMethod,
            Double discount,
            Double serviceCharge,
            PaymentScheme paymentScheme) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.netTotal = netTotal;
        this.total = total;
        this.billTypeAtomic = billTypeAtomic;
        this.paymentMethod = paymentMethod;
        this.discount = discount;
        this.serviceCharge = serviceCharge;
        this.paymentScheme = paymentScheme;
    }

    public LabIncomeReportDTO(Long billId, String billNumber, Date billDate, String patientName,
            String departmentName, String institutionName,
            Double netTotal, Double total,
            BillTypeAtomic billTypeAtomic, PaymentMethod paymentMethod,
            Double discount,
            Double serviceCharge, PaymentScheme paymentScheme) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.patientName = patientName;
        this.departmentName = departmentName;
        this.institutionName = institutionName;
        this.netTotal = netTotal;
        this.total = total;
        this.billTypeAtomic = billTypeAtomic;
        this.paymentMethod = paymentMethod;
        this.discount = discount;
        this.serviceCharge = serviceCharge;
        this.paymentScheme = paymentScheme;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Double getCashValue() {
        return cashValue != null ? cashValue : 0.0;
    }

    public void setCashValue(Double cashValue) {
        this.cashValue = cashValue;
    }

    public Double getCardValue() {
        return cardValue != null ? cardValue : 0.0;
    }

    public void setCardValue(Double cardValue) {
        this.cardValue = cardValue;
    }

    public Double getInpatientCreditValue() {
        return inpatientCreditValue != null ? inpatientCreditValue : 0.0;
    }

    public void setInpatientCreditValue(Double inpatientCreditValue) {
        this.inpatientCreditValue = inpatientCreditValue;
    }

    public Double getOpdCreditValue() {
        return opdCreditValue != null ? opdCreditValue : 0.0;
    }

    public void setOpdCreditValue(Double opdCreditValue) {
        this.opdCreditValue = opdCreditValue;
    }

    public Double getStaffValue() {
        return staffValue != null ? staffValue : 0.0;
    }

    public void setStaffValue(Double staffValue) {
        this.staffValue = staffValue;
    }

    public Double getAgentValue() {
        return agentValue != null ? agentValue : 0.0;
    }

    public void setAgentValue(Double agentValue) {
        this.agentValue = agentValue;
    }

    public Double getDiscount() {
        return discount != null ? discount : 0.0;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getServiceCharge() {
        return serviceCharge != null ? serviceCharge : 0.0;
    }

    public void setServiceCharge(Double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }
}
