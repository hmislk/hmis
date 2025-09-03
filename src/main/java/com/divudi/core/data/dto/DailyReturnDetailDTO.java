package com.divudi.core.data.dto;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * Detailed projection for individual Daily Return bill entries.
 */
public class DailyReturnDetailDTO implements Serializable {
    private String billNumber;
    private BillType billType;
    private BillClassType billClassType;
    private Double netTotal;
    private Date createdAt;
    private PaymentMethod paymentMethod;
    private String departmentName;
    private String fromDepartmentName;

    public DailyReturnDetailDTO() {
    }

    public DailyReturnDetailDTO(String billNumber, BillType billType, BillClassType billClassType, 
                               Double netTotal, Date createdAt, PaymentMethod paymentMethod, 
                               String departmentName, String fromDepartmentName) {
        this.billNumber = billNumber;
        this.billType = billType;
        this.billClassType = billClassType;
        this.netTotal = netTotal;
        this.createdAt = createdAt;
        this.paymentMethod = paymentMethod;
        this.departmentName = departmentName;
        this.fromDepartmentName = fromDepartmentName;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }
}