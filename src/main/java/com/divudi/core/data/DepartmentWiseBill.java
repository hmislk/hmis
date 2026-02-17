/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import java.util.Date;

/**
 *
 * @author Pubudu Piyankara
 */
public class DepartmentWiseBill {

    private Department department;
    private Department toDepartment;
    private Bill bill;
    private String billId;
    private Date createdDate;
    private Bill backwardReferenceBill;
    private String backwardReferenceBillId;
    private Double subTotal;

    public DepartmentWiseBill() {
    }

    public DepartmentWiseBill(Department department, Department toDepartment, Bill bill,
            String deptId, Date createdAt, Bill backwardReferenceBill,
            String backwardReferenceBillId, Double netTotal) {
        this.department = department;
        this.toDepartment = toDepartment;
        this.bill = bill;
        this.billId = deptId;
        this.createdDate = createdAt;
        this.backwardReferenceBill = backwardReferenceBill;
        this.backwardReferenceBillId = backwardReferenceBillId;
        this.subTotal = netTotal;
    }
    public DepartmentWiseBill(Department department,
            String deptId, Date createdAt, Bill backwardReferenceBill,
            String backwardReferenceBillId, Double netTotal, Bill bill) {
        this.department = department;
        this.toDepartment = toDepartment;
        this.bill = bill;
        this.billId = deptId;
        this.createdDate = createdAt;
        this.backwardReferenceBill = backwardReferenceBill;
        this.backwardReferenceBillId = backwardReferenceBillId;
        this.subTotal = netTotal;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Bill getBackwardReferenceBill() {
        return backwardReferenceBill;
    }

    public void setBackwardReferenceBill(Bill backwardReferenceBill) {
        this.backwardReferenceBill = backwardReferenceBill;
    }

    public String getBackwardReferenceBillId() {
        return backwardReferenceBillId;
    }

    public void setBackwardReferenceBillId(String backwardReferenceBillId) {
        this.backwardReferenceBillId = backwardReferenceBillId;
    }

    public Double getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(Double subTotal) {
        this.subTotal = subTotal;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

}
