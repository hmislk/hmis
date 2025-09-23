/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.data.table.String1Value3;

import java.util.List;

/**
 *
 * @author safrin
 */
public class PharmacySummery {

    private List<String1Value3> bills;
    private Double billedTotal = 0.0;
    private Double cancelledTotal = 0.0;
    private Double refundedTotal = 0.0;
    private Long count;
    private String departmentName;
    private double netTotal;
    private String department;
    private double goodInTransistAmount;

    public PharmacySummery(String departmentName, double netTotal) {
        this.departmentName = departmentName;
        this.netTotal = netTotal;
    }
    public PharmacySummery(String departmentName, double netTotal, double goodInTransistAmount) {
        this.departmentName = departmentName;
        this.netTotal = netTotal;
        this.goodInTransistAmount = goodInTransistAmount;
    }
    public PharmacySummery(String departmentName, String department, double netTotal) {
        this.departmentName = departmentName;
        this.department = department;
        this.netTotal = netTotal;
    }
    public PharmacySummery() {

    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public List<String1Value3> getBills() {
        return bills;
    }

    public void setBills(List<String1Value3> bills) {
        this.bills = bills;
    }

    public Double getBilledTotal() {
        return billedTotal;
    }

    public void setBilledTotal(Double billedTotal) {
        this.billedTotal = billedTotal;
    }

    public Double getCancelledTotal() {
        return cancelledTotal;
    }

    public void setCancelledTotal(Double cancelledTotal) {
        this.cancelledTotal = cancelledTotal;
    }

    public Double getRefundedTotal() {
        return refundedTotal;
    }

    public void setRefundedTotal(Double refundedTotal) {
        this.refundedTotal = refundedTotal;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public double getGoodInTransistAmount() {
        return goodInTransistAmount;
    }

    public void setGoodInTransistAmount(double goodInTransistAmount) {
        this.goodInTransistAmount = goodInTransistAmount;
    }

}
