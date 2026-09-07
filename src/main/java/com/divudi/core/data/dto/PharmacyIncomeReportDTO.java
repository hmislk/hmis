package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * DTO for Pharmacy Income Report optimization
 * This class represents only the essential display fields needed for the report
 * to improve performance by avoiding entity relationship loading
 * 
 * @author Claude Code
 */
public class PharmacyIncomeReportDTO implements Serializable {
    
    private Long billId;
    private String billNumber;
    private Date billDate;
    private String billTypeName;
    private String departmentName;
    private String institutionName;
    private String patientName;
    private String patientPhone;
    private BigDecimal netTotal;
    private BigDecimal total;
    private BigDecimal discount;
    private BigDecimal tax;
    private String paymentMethodName;
    private String createdUserName;
    private Date createdAt;
    
    // Default constructor
    public PharmacyIncomeReportDTO() {
    }
    
    // Constructor for basic report fields
    public PharmacyIncomeReportDTO(Long billId, String billNumber, Date billDate, 
                                  String billTypeName, String departmentName, 
                                  String institutionName, BigDecimal netTotal, 
                                  BigDecimal total) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.billTypeName = billTypeName;
        this.departmentName = departmentName;
        this.institutionName = institutionName;
        this.netTotal = netTotal;
        this.total = total;
    }
    
    // Constructor for detailed report fields
    public PharmacyIncomeReportDTO(Long billId, String billNumber, Date billDate,
                                  String billTypeName, String departmentName,
                                  String institutionName, String patientName,
                                  String patientPhone, BigDecimal netTotal,
                                  BigDecimal total, BigDecimal discount,
                                  BigDecimal tax, String paymentMethodName,
                                  String createdUserName, Date createdAt) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.billTypeName = billTypeName;
        this.departmentName = departmentName;
        this.institutionName = institutionName;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.netTotal = netTotal;
        this.total = total;
        this.discount = discount;
        this.tax = tax;
        this.paymentMethodName = paymentMethodName;
        this.createdUserName = createdUserName;
        this.createdAt = createdAt;
    }

    // Getters and Setters
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

    public String getBillTypeName() {
        return billTypeName;
    }

    public void setBillTypeName(String billTypeName) {
        this.billTypeName = billTypeName;
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

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public BigDecimal getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(BigDecimal netTotal) {
        this.netTotal = netTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public String getPaymentMethodName() {
        return paymentMethodName;
    }

    public void setPaymentMethodName(String paymentMethodName) {
        this.paymentMethodName = paymentMethodName;
    }

    public String getCreatedUserName() {
        return createdUserName;
    }

    public void setCreatedUserName(String createdUserName) {
        this.createdUserName = createdUserName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PharmacyIncomeReportDTO{" +
                "billId=" + billId +
                ", billNumber='" + billNumber + '\'' +
                ", billDate=" + billDate +
                ", billTypeName='" + billTypeName + '\'' +
                ", total=" + total +
                '}';
    }
}