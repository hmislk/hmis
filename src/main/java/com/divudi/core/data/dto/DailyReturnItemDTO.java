package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Daily Return Report Item Performance Optimization
 * 
 * This DTO represents individual line items in the Daily Return report,
 * optimized for direct JPQL queries without entity loading.
 * 
 * @author Dr M H B Ariyaratne
 * @author Kabi10 (Performance Optimization Implementation)
 */
public class DailyReturnItemDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Identification
    private Long id;
    private String itemName;
    private String itemCode;
    private String categoryName;
    private String departmentName;
    private String institutionName;
    
    // Bill Information
    private Long billId;
    private String billNumber;
    private BillTypeAtomic billTypeAtomic;
    private Date billCreatedAt;
    private String billCreatedBy;
    
    // Financial Information - Using Double wrapper types to prevent NPE in JPQL constructors
    private Double quantity;
    private Double rate;
    private Double grossValue;
    private Double discount;
    private Double netValue;
    private Double hospitalFee;
    private Double professionalFee;
    private Double total;
    
    // Payment Information
    private PaymentMethod paymentMethod;
    private Double paidValue;
    private String paymentCreatedBy;
    private Date paymentCreatedAt;
    
    // Credit Company Information
    private String creditCompanyName;
    private String creditCompanyCode;
    
    // Patient Information (for audit purposes)
    private String patientName;
    private String patientPhone;
    
    // Constructors
    public DailyReturnItemDTO() {
    }
    
    // Constructor for Bill Item based queries
    public DailyReturnItemDTO(Long id, String itemName, String categoryName, String departmentName,
                             Double quantity, Double rate, Double grossValue, Double discount, 
                             Double netValue, Double hospitalFee, Double professionalFee, Double total) {
        this.id = id;
        this.itemName = itemName;
        this.categoryName = categoryName;
        this.departmentName = departmentName;
        this.quantity = quantity;
        this.rate = rate;
        this.grossValue = grossValue;
        this.discount = discount;
        this.netValue = netValue;
        this.hospitalFee = hospitalFee;
        this.professionalFee = professionalFee;
        this.total = total;
    }
    
    // Constructor for Payment based queries
    public DailyReturnItemDTO(Long id, PaymentMethod paymentMethod, Double paidValue, 
                             String paymentCreatedBy, Date paymentCreatedAt, String departmentName) {
        this.id = id;
        this.paymentMethod = paymentMethod;
        this.paidValue = paidValue;
        this.paymentCreatedBy = paymentCreatedBy;
        this.paymentCreatedAt = paymentCreatedAt;
        this.departmentName = departmentName;
        this.total = paidValue;
    }
    
    // Constructor for Credit Company queries
    public DailyReturnItemDTO(Long id, String creditCompanyName, String creditCompanyCode,
                             Double paidValue, String departmentName, Date paymentCreatedAt) {
        this.id = id;
        this.creditCompanyName = creditCompanyName;
        this.creditCompanyCode = creditCompanyCode;
        this.paidValue = paidValue;
        this.departmentName = departmentName;
        this.paymentCreatedAt = paymentCreatedAt;
        this.total = paidValue;
    }
    
    // Constructor for comprehensive bill item data
    public DailyReturnItemDTO(Long id, String itemName, String itemCode, String categoryName, 
                             String departmentName, String institutionName, Long billId, 
                             String billNumber, BillTypeAtomic billTypeAtomic, Date billCreatedAt,
                             Double quantity, Double rate, Double grossValue, Double discount,
                             Double netValue, Double hospitalFee, Double professionalFee, Double total) {
        this.id = id;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.categoryName = categoryName;
        this.departmentName = departmentName;
        this.institutionName = institutionName;
        this.billId = billId;
        this.billNumber = billNumber;
        this.billTypeAtomic = billTypeAtomic;
        this.billCreatedAt = billCreatedAt;
        this.quantity = quantity;
        this.rate = rate;
        this.grossValue = grossValue;
        this.discount = discount;
        this.netValue = netValue;
        this.hospitalFee = hospitalFee;
        this.professionalFee = professionalFee;
        this.total = total;
    }
    
    // Utility Methods - Null-safe formatting for Double wrapper types
    public String getFormattedTotal() {
        return total != null ? String.format("%.2f", total) : "0.00";
    }
    
    public String getFormattedQuantity() {
        return quantity != null ? String.format("%.2f", quantity) : "0.00";
    }
    
    public String getFormattedRate() {
        return rate != null ? String.format("%.2f", rate) : "0.00";
    }
    
    public boolean isPositiveValue() {
        return total != null && total > 0;
    }
    
    public boolean isNegativeValue() {
        return total != null && total < 0;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public String getItemCode() {
        return itemCode;
    }
    
    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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
    
    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }
    
    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }
    
    public Date getBillCreatedAt() {
        return billCreatedAt;
    }
    
    public void setBillCreatedAt(Date billCreatedAt) {
        this.billCreatedAt = billCreatedAt;
    }
    
    public String getBillCreatedBy() {
        return billCreatedBy;
    }
    
    public void setBillCreatedBy(String billCreatedBy) {
        this.billCreatedBy = billCreatedBy;
    }
    
    public Double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
    
    public Double getRate() {
        return rate;
    }
    
    public void setRate(Double rate) {
        this.rate = rate;
    }
    
    public Double getGrossValue() {
        return grossValue;
    }
    
    public void setGrossValue(Double grossValue) {
        this.grossValue = grossValue;
    }
    
    public Double getDiscount() {
        return discount;
    }
    
    public void setDiscount(Double discount) {
        this.discount = discount;
    }
    
    public Double getNetValue() {
        return netValue;
    }
    
    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }
    
    public Double getHospitalFee() {
        return hospitalFee;
    }
    
    public void setHospitalFee(Double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }
    
    public Double getProfessionalFee() {
        return professionalFee;
    }
    
    public void setProfessionalFee(Double professionalFee) {
        this.professionalFee = professionalFee;
    }
    
    public Double getTotal() {
        return total;
    }
    
    public void setTotal(Double total) {
        this.total = total;
    }
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public Double getPaidValue() {
        return paidValue;
    }
    
    public void setPaidValue(Double paidValue) {
        this.paidValue = paidValue;
    }
    
    public String getPaymentCreatedBy() {
        return paymentCreatedBy;
    }
    
    public void setPaymentCreatedBy(String paymentCreatedBy) {
        this.paymentCreatedBy = paymentCreatedBy;
    }
    
    public Date getPaymentCreatedAt() {
        return paymentCreatedAt;
    }
    
    public void setPaymentCreatedAt(Date paymentCreatedAt) {
        this.paymentCreatedAt = paymentCreatedAt;
    }
    
    public String getCreditCompanyName() {
        return creditCompanyName;
    }
    
    public void setCreditCompanyName(String creditCompanyName) {
        this.creditCompanyName = creditCompanyName;
    }
    
    public String getCreditCompanyCode() {
        return creditCompanyCode;
    }
    
    public void setCreditCompanyCode(String creditCompanyCode) {
        this.creditCompanyCode = creditCompanyCode;
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
}
