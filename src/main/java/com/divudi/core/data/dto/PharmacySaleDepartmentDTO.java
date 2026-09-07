package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PharmacySaleDepartmentDTO implements Serializable {

    private Long departmentId;
    private String departmentName;
    private List<PharmacySaleBhtBillDTO> bhtBills;

    private Double totalGrossValue;
    private Double totalMarginValue;
    private Double totalDiscount;
    private Double totalNetValue;

    public PharmacySaleDepartmentDTO() {
        this.bhtBills = new ArrayList<>();
    }

    public PharmacySaleDepartmentDTO(Long departmentId, String departmentName) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.bhtBills = new ArrayList<>();
    }

    public void calculateTotals() {
        totalGrossValue = 0.0;
        totalMarginValue = 0.0;
        totalDiscount = 0.0;
        totalNetValue = 0.0;
        for (PharmacySaleBhtBillDTO bht : bhtBills) {
            bht.calculateTotals();
            totalGrossValue += bht.getTotalGrossValue() != null ? bht.getTotalGrossValue() : 0.0;
            totalMarginValue += bht.getTotalMarginValue() != null ? bht.getTotalMarginValue() : 0.0;
            totalDiscount += bht.getTotalDiscount() != null ? bht.getTotalDiscount() : 0.0;
            totalNetValue += bht.getTotalNetValue() != null ? bht.getTotalNetValue() : 0.0;
        }
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public List<PharmacySaleBhtBillDTO> getBhtBills() {
        return bhtBills;
    }

    public void setBhtBills(List<PharmacySaleBhtBillDTO> bhtBills) {
        this.bhtBills = bhtBills;
    }

    public Double getTotalGrossValue() {
        return totalGrossValue;
    }

    public void setTotalGrossValue(Double totalGrossValue) {
        this.totalGrossValue = totalGrossValue;
    }

    public Double getTotalMarginValue() {
        return totalMarginValue;
    }

    public void setTotalMarginValue(Double totalMarginValue) {
        this.totalMarginValue = totalMarginValue;
    }

    public Double getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(Double totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public Double getTotalNetValue() {
        return totalNetValue;
    }

    public void setTotalNetValue(Double totalNetValue) {
        this.totalNetValue = totalNetValue;
    }
}