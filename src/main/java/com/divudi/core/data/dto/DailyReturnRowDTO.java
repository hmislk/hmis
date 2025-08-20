package com.divudi.core.data.dto;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO-specific row for Daily Return detailed transaction data.
 */
public class DailyReturnRowDTO implements Serializable {
    private String billNumber;
    private BillType billType;
    private BillClassType billClassType;
    private String departmentName;
    private String fromDepartmentName;
    private PaymentMethod paymentMethod;
    private Date createdAt;
    
    private String categoryName;
    private String itemName;
    private String feeName;
    private String paymentName;
    
    private Long itemCount = 1L;
    private Double itemHospitalFee = 0.0;
    private Double itemProfessionalFee = 0.0;
    private Double itemDiscountAmount = 0.0;
    private Double itemNetTotal = 0.0;
    private Double total = 0.0;

    public DailyReturnRowDTO() {
    }

    public DailyReturnRowDTO(String billNumber, BillType billType, BillClassType billClassType, 
                            String departmentName, PaymentMethod paymentMethod, Double netTotal) {
        this.billNumber = billNumber;
        this.billType = billType;
        this.billClassType = billClassType;
        this.departmentName = departmentName;
        this.paymentMethod = paymentMethod;
        this.itemNetTotal = netTotal;
        this.total = netTotal;
        
        // Set display names
        this.itemName = billNumber;
        this.categoryName = billType != null ? billType.toString() : "";
        this.feeName = departmentName;
        this.paymentName = paymentMethod != null ? paymentMethod.toString() : "";
        
        // Calculate simulated breakdowns
        if (netTotal != null && netTotal > 0) {
            this.itemHospitalFee = netTotal * 0.8;  // 80% hospital fee
            this.itemProfessionalFee = netTotal * 0.2;  // 20% professional fee
        }
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
        this.itemName = billNumber;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
        this.categoryName = billType != null ? billType.toString() : "";
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
        this.feeName = departmentName;
    }

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        this.paymentName = paymentMethod != null ? paymentMethod.toString() : "";
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getFeeName() {
        return feeName;
    }

    public void setFeeName(String feeName) {
        this.feeName = feeName;
    }

    public String getPaymentName() {
        return paymentName;
    }

    public void setPaymentName(String paymentName) {
        this.paymentName = paymentName;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }

    public Double getItemHospitalFee() {
        return itemHospitalFee;
    }

    public void setItemHospitalFee(Double itemHospitalFee) {
        this.itemHospitalFee = itemHospitalFee;
    }

    public Double getItemProfessionalFee() {
        return itemProfessionalFee;
    }

    public void setItemProfessionalFee(Double itemProfessionalFee) {
        this.itemProfessionalFee = itemProfessionalFee;
    }

    public Double getItemDiscountAmount() {
        return itemDiscountAmount;
    }

    public void setItemDiscountAmount(Double itemDiscountAmount) {
        this.itemDiscountAmount = itemDiscountAmount;
    }

    public Double getItemNetTotal() {
        return itemNetTotal;
    }

    public void setItemNetTotal(Double itemNetTotal) {
        this.itemNetTotal = itemNetTotal;
        this.total = itemNetTotal;
        
        // Recalculate breakdowns when net total changes
        if (itemNetTotal != null && itemNetTotal > 0) {
            this.itemHospitalFee = itemNetTotal * 0.8;
            this.itemProfessionalFee = itemNetTotal * 0.2;
        }
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}