package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PharmacySaleBhtBillDTO implements Serializable {

    private String bhtNumber;
    private Long billId;
    private String deptId;
    private String insId;
    private Date billDate;
    private String patientPhn;
    private String patientName;
    private List<PharmacySaleItemDTO> items;

    private Double totalGrossValue;
    private Double totalMarginValue;
    private Double totalDiscount;
    private Double totalNetValue;

    public PharmacySaleBhtBillDTO() {
        this.items = new ArrayList<>();
    }

    public PharmacySaleBhtBillDTO(String bhtNumber, Long billId, String deptId,
            String insId, Date billDate, String patientPhn, String patientName) {
        this.bhtNumber = bhtNumber;
        this.billId = billId;
        this.deptId = deptId;
        this.insId = insId;
        this.billDate = billDate;
        this.patientPhn = patientPhn;
        this.patientName = patientName;
        this.items = new ArrayList<>();
    }

    public void calculateTotals() {
        totalGrossValue = 0.0;
        totalMarginValue = 0.0;
        totalDiscount = 0.0;
        totalNetValue = 0.0;
        for (PharmacySaleItemDTO item : items) {
            totalGrossValue += item.getGrossValue() != null ? item.getGrossValue() : 0.0;
            totalMarginValue += item.getMarginValue() != null ? item.getMarginValue() : 0.0;
            totalDiscount += item.getDiscount() != null ? item.getDiscount() : 0.0;
            totalNetValue += item.getNetValue() != null ? item.getNetValue() : 0.0;
        }
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

    public List<PharmacySaleItemDTO> getItems() {
        return items;
    }

    public void setItems(List<PharmacySaleItemDTO> items) {
        this.items = items;
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