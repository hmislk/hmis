/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PaymentScheme;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author OVS
 */
public class OpdRevenueDashboardDTO implements Serializable{
    private Long billId;
    private String deptId;
    private BillTypeAtomic billTypeAtomic;
    private Date createdAt;
    private Double netTotal;
    private Double total;
    private Double discount;
    private Double margin;
    private Double serviceCharge;
    private PaymentScheme paymentScheme;
    private Department department;
    private Department toDepartment;
    private Institution institution;
    
    public OpdRevenueDashboardDTO() {
        
    }
    
    // constructor used for dashboard opd revenue
    public OpdRevenueDashboardDTO(Long billId, String deptId, BillTypeAtomic billTypeAtomic, Date createdAt,
                              Double netTotal, Double total, Department department, Institution institution, Department toDepartment) {
        this.billId = billId;
        this.deptId = deptId;
        this.billTypeAtomic = billTypeAtomic;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.total = total;
        this.department = department;
        this.institution = institution;
        this.toDepartment = toDepartment;
        
        System.out.println("billID: " + billId + "total = " + total);
    }
    
    // constructor used for dashboard discount department wise
    public OpdRevenueDashboardDTO(Long billId, String deptId, BillTypeAtomic billTypeAtomic, Double discount,Department toDepartment) {
        this.billId = billId;
        this.deptId = deptId;
        this.billTypeAtomic = billTypeAtomic;
        this.discount = discount;
        this.toDepartment = toDepartment;
    }
    
    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
    }

    public Double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(Double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }
    
    public Department getToDepartment () {
        return toDepartment;
    }
    
    public void setToDepartment(Department toDepartment) {
        this.toDepartment= toDepartment;
    }
            
}
