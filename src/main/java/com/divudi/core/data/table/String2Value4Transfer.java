/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.table;

/**
 * DTO for transfer reports with from and to department information
 * @author Claude
 */
public class String2Value4Transfer {

    private String fromDepartment;
    private String toDepartment;
    private Double transferValue = 0.0;      // value1
    private Double purchaseValue = 0.0;      // value2  
    private Double retailValue = 0.0;        // value3
    private Double costValue = 0.0;          // value4

    public String2Value4Transfer() {
    }

    public String2Value4Transfer(String fromDepartment, String toDepartment, 
                                Double transferValue, Double purchaseValue, 
                                Double retailValue, Double costValue) {
        this.fromDepartment = fromDepartment;
        this.toDepartment = toDepartment;
        this.transferValue = transferValue;
        this.purchaseValue = purchaseValue;
        this.retailValue = retailValue;
        this.costValue = costValue;
    }

    public String getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(String fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public String getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(String toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Double getTransferValue() {
        return transferValue;
    }

    public void setTransferValue(Double transferValue) {
        this.transferValue = transferValue;
    }

    public Double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public Double getRetailValue() {
        return retailValue;
    }

    public void setRetailValue(Double retailValue) {
        this.retailValue = retailValue;
    }

    public Double getCostValue() {
        return costValue;
    }

    public void setCostValue(Double costValue) {
        this.costValue = costValue;
    }
}