package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class PharmacySaleItemDTO implements Serializable {

    private Long departmentId;
    private String departmentName;
    private String bhtNumber;
    private Long billId;
    private String deptId;
    private String insId;
    private Date billDate;
    private String patientPhn;
    private String patientName;
    private Long itemId;
    private String itemName;
    private Double qty;
    private Double retailRate;
    private Double purchaseRate;
    private Double grossValue;
    private Double marginValue;
    private Double discount;
    private Double netValue;

    public PharmacySaleItemDTO() {
    }

    public PharmacySaleItemDTO(Long departmentId, String departmentName,
            String bhtNumber, Long billId, String deptId, String insId,
            Date billDate, String patientPhn, String patientName,
            Long itemId, String itemName,
            Double qty, Double retailRate, Double purchaseRate,
            Double grossValue, Double marginValue,
            Double discount, Double netValue) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.bhtNumber = bhtNumber;
        this.billId = billId;
        this.deptId = deptId;
        this.insId = insId;
        this.billDate = billDate;
        this.patientPhn = patientPhn;
        this.patientName = patientName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.qty = qty;
        this.retailRate = retailRate;
        this.purchaseRate = purchaseRate;
        this.grossValue = grossValue;
        this.marginValue = marginValue;
        this.discount = discount;
        this.netValue = netValue;
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

    public String getBhtNumber() {
        return bhtNumber;
    }

    public void setBhtNumber(String bhtNumber) {
        this.bhtNumber = bhtNumber;
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

    public String getInsId() {
        return insId;
    }

    public void setInsId(String insId) {
        this.insId = insId;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public String getPatientPhn() {
        return patientPhn;
    }

    public void setPatientPhn(String patientPhn) {
        this.patientPhn = patientPhn;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(Double retailRate) {
        this.retailRate = retailRate;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Double getGrossValue() {
        return grossValue;
    }

    public void setGrossValue(Double grossValue) {
        this.grossValue = grossValue;
    }

    public Double getMarginValue() {
        return marginValue;
    }

    public void setMarginValue(Double marginValue) {
        this.marginValue = marginValue;
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
}