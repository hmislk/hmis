/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.data.table.String1Value3;
import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

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
    private String issuedDeptName;
    private String receivedDeptName;
    private double netTotal;
    private double totalCost;
    private String department;
    private double goodInTransistAmount;
    private BigDecimal totalPurchaseValue;
    private BigDecimal totalCostValue;
    private BigDecimal totalRetailSaleValue;
    private Map<String, List<PharmacySummery>> summeriesMap;

    public PharmacySummery(String departmentName, double netTotal) {
        this.departmentName = departmentName;
        this.netTotal = netTotal;
    }

    public PharmacySummery(String departmentName, double netTotal, double goodInTransistAmount) {
        this.departmentName = departmentName;
        this.netTotal = netTotal;
        this.goodInTransistAmount = goodInTransistAmount;
    }

    public PharmacySummery(String departmentName, double netTotal, double goodInTransistAmount, double totalCost) {
        this.departmentName = departmentName;
        this.netTotal = netTotal;
        this.goodInTransistAmount = goodInTransistAmount;
        this.totalCost = totalCost;
    }

    public PharmacySummery(String departmentName, String issuedDeptName, String receivedDeptName, BigDecimal totalPurchaseValue, BigDecimal totalCostValue, BigDecimal totalRetailSaleValue) {
        this.departmentName = departmentName;
        this.issuedDeptName = issuedDeptName;
        this.receivedDeptName = receivedDeptName;
        this.totalPurchaseValue = totalPurchaseValue;
        this.totalCostValue = totalCostValue;
        this.totalRetailSaleValue = totalRetailSaleValue;
    }
    public PharmacySummery(String departmentName, BigDecimal totalPurchaseValue, BigDecimal totalCostValue, BigDecimal totalRetailSaleValue, double goodInTransistAmount) {
        this.departmentName = departmentName;
        this.totalPurchaseValue = totalPurchaseValue;
        this.totalCostValue = totalCostValue;
        this.totalRetailSaleValue = totalRetailSaleValue;
        this.goodInTransistAmount = goodInTransistAmount;
    }
    public PharmacySummery(String departmentName, BigDecimal totalPurchaseValue, BigDecimal totalCostValue, BigDecimal totalRetailSaleValue) {
        this.departmentName = departmentName;
        this.totalPurchaseValue = totalPurchaseValue;
        this.totalCostValue = totalCostValue;
        this.totalRetailSaleValue = totalRetailSaleValue;
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

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(BigDecimal totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public BigDecimal getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(BigDecimal totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public BigDecimal getTotalRetailSaleValue() {
        return totalRetailSaleValue;
    }

    public void setTotalRetailSaleValue(BigDecimal totalRetailSaleValue) {
        this.totalRetailSaleValue = totalRetailSaleValue;
    }

    public Map<String, List<PharmacySummery>> getSummeriesMap() {
        return summeriesMap;
    }

    public void setSummeriesMap(Map<String, List<PharmacySummery>> summeriesMap) {
        this.summeriesMap = summeriesMap;
    }

    public String getIssuedDeptName() {
        return issuedDeptName;
    }

    public void setIssuedDeptName(String issuedDeptName) {
        this.issuedDeptName = issuedDeptName;
    }

    public String getReceivedDeptName() {
        return receivedDeptName;
    }

    public void setReceivedDeptName(String receivedDeptName) {
        this.receivedDeptName = receivedDeptName;
    }
}
